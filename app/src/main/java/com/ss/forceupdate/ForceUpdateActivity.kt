package com.ss.forceupdate

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ss.forceupdate.Constants.EXTRA_APK_LINK
import com.ss.forceupdate.Constants.EXTRA_APPLICATION_NAME
import com.ss.forceupdate.Constants.EXTRA_LOGO_IMAGE
import com.ss.forceupdate.Constants.EXTRA_MESSAGE
import com.ss.forceupdate.Constants.EXTRA_TITLE
import com.ss.forceupdate.databinding.ActivityForceUpdateBinding
import kotlinx.coroutines.*
import java.io.File

class ForceUpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForceUpdateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForceUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestUpdate()
    }

    private fun requestUpdate() {
        binding.update.visibility = View.VISIBLE
        binding.message.text = getString(R.string.update_message)
        binding.progressBar.visibility = View.GONE
        binding.downloaded.visibility = View.GONE

        binding.title.text = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.new_update)
        binding.message.text = intent.getStringExtra(EXTRA_MESSAGE) ?: getString(R.string.update_message)
        binding.update.text = getString(R.string.update)
        binding.logo.setImageResource(intent.getIntExtra(EXTRA_LOGO_IMAGE, R.drawable.google_play_logo))
        binding.applicationName.text = intent.getStringExtra(EXTRA_APPLICATION_NAME) ?: getString(R.string.app_name)

        binding.update.setOnClickListener {
            downloadApk()
        }
    }

    private fun downloadApk() {
        val request = DownloadManager.Request(Uri.parse(intent.getStringExtra(EXTRA_APK_LINK))).apply {
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverRoaming(true)
            setAllowedOverMetered(true)
            setDestinationInExternalFilesDir(applicationContext, null, "update.apk")
        }

        val sharedPreferences = getSharedPreferences(DOWNLOAD_ID_NAME, Context.MODE_PRIVATE)
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val id = sharedPreferences.getLong(DOWNLOAD_ID_KEY, -1L)
        downloadManager.remove(id)
        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        sharedPreferences.edit().apply {
            putLong(DOWNLOAD_ID_KEY, downloadId)
            apply()
        }

        showDownloadProgress(downloadManager, query)
    }

    private fun showDownloadProgress(downloadManager: DownloadManager, query: DownloadManager.Query) {

        var isDownloading = true
        binding.update.visibility = View.GONE
        binding.message.text = getString(R.string.downloading)
        binding.progressBar.visibility = View.VISIBLE
        binding.downloaded.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            while (isDownloading) {
                val cursor = downloadManager.query(query)

                if (cursor != null && cursor.count >= 0 && cursor.moveToFirst()) {

                    val bytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val percentage = ((bytes.toDouble() / totalSize) * 100).toInt()

                    withContext(Dispatchers.Main) {
                        binding.progressBar.progress = percentage
                        binding.progressBar.max = 100
                        binding.downloaded.text = getString(R.string.percentage, percentage)
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        isDownloading = false
                        binding.message.text = getString(R.string.completed)
                        val uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        val localFile = File(Uri.parse(uri).path)

                        installApk(localFile)
                        this.cancel()
                    }

                } else withContext(Dispatchers.Main) {
                    isDownloading = false
                    binding.message.text = getString(R.string.canceled)
                    requestUpdate()
                    this.cancel()
                }
            }
        }
    }

    private fun installApk(localFile: File) {
        val contentUri = FileProvider.getUriForFile(this, packageName, localFile)
        val packageInstaller = packageManager.packageInstaller
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session: PackageInstaller.Session = packageInstaller.openSession(sessionId)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            setDataAndType(contentUri, INSTALL_TYPE)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val statusReceiver = pendingIntent.intentSender
        session.commit(statusReceiver)
        session.abandon()
        session.close()

        showInstallationStatus(packageInstaller, localFile)
    }

    private fun showInstallationStatus(packageInstaller: PackageInstaller, localFile: File) {
        lifecycleScope.launch {
            packageInstaller.registerSessionCallback(object : PackageInstaller.SessionCallback() {
                override fun onCreated(sessionId: Int) {
                }

                override fun onBadgingChanged(sessionId: Int) {
                }

                override fun onActiveChanged(id: Int, active: Boolean) {
                }

                override fun onProgressChanged(sessionId: Int, progress: Float) {
                    val installProgress = ((progress / 0.90000004) * 100).toInt() + 1
                    binding.update.visibility = View.GONE
                    binding.message.text = getString(R.string.installing)
                    binding.progressBar.visibility = View.VISIBLE
                    binding.downloaded.visibility = View.VISIBLE
                    binding.progressBar.max = 100
                    binding.progressBar.progress = installProgress
                }

                override fun onFinished(id: Int, success: Boolean) {
                    if (success) {
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                        localFile.delete()
                    } else requestUpdate()
                }
            })
        }
    }

    override fun onBackPressed() {
    }

    companion object {
        private const val DOWNLOAD_ID_KEY = "Download"
        private const val DOWNLOAD_ID_NAME = "Download ID"
        private const val INSTALL_TYPE = "application/vnd.android.package-archive"
    }
}