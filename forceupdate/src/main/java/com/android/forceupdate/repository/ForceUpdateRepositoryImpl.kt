package com.android.forceupdate.repository

import android.app.Application
import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File

class ForceUpdateRepositoryImpl(private val application: Application) : ForceUpdateRepository {

    override suspend fun downloadApk(apkLink: String) = flow {
        val request = DownloadManager.Request(Uri.parse(apkLink)).apply {
            this.setAllowedOverRoaming(true)
            this.setAllowedOverMetered(true)
            this.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            this.setDestinationInExternalFilesDir(application, null, "update.apk")
        }

        val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val sharedPreferences = application.getSharedPreferences(DOWNLOAD_ID_NAME, Context.MODE_PRIVATE)
        val id = sharedPreferences.getLong(DOWNLOAD_ID_KEY, -1L)
        downloadManager.remove(id)
        val downloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)

        sharedPreferences.edit().apply {
            this.putLong(DOWNLOAD_ID_KEY, downloadId)
            this.apply()
        }

        withContext(Dispatchers.IO) {
            var isDownloading = true
            while (isDownloading) {
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.count >= 0 && cursor.moveToFirst()) {

                    val bytes = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalSize = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    val percentage = ((bytes.toDouble() / totalSize) * 100).toInt()

                    withContext(Dispatchers.Main) {
                        this@flow.emit(DownloadStatus.DownloadProgress(percentage))
                    }

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        withContext(Dispatchers.Main) {
                            isDownloading = false
                            val uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            this@flow.emit(DownloadStatus.DownloadCompleted(File(Uri.parse(uri).path!!)))
                            this.cancel()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        this@flow.emit(DownloadStatus.DownloadCanceled)
                        this.cancel()
                    }
                }
            }
        }
    }

    sealed class DownloadStatus {
        data class DownloadProgress(val progress: Int) : DownloadStatus()
        data class DownloadCompleted(val localFile: File) : DownloadStatus()
        object DownloadCanceled : DownloadStatus()
    }

    override fun installApk(localFile: File) = callbackFlow<InstallStatus> {
        val contentUri = FileProvider.getUriForFile(application, application.packageName, localFile)
        val packageInstaller = application.packageManager.packageInstaller
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session: PackageInstaller.Session = packageInstaller.openSession(sessionId)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            this.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            this.setDataAndType(contentUri, INSTALL_TYPE)
        }

        application.startActivity(intent)

        val pendingIntent = PendingIntent.getActivity(application, 0, intent, 0)
        val statusReceiver = pendingIntent.intentSender
        session.commit(statusReceiver)
        session.abandon()
        session.close()

        packageInstaller.registerSessionCallback(object : PackageInstaller.SessionCallback() {
            override fun onCreated(sessionId: Int) {
            }
            override fun onBadgingChanged(sessionId: Int) {
            }
            override fun onActiveChanged(id: Int, active: Boolean) {
            }
            override fun onProgressChanged(sessionId: Int, progress: Float) {
                val installProgress = ((progress / 0.90000004) * 100).toInt()
                sendBlocking(InstallStatus.InstallProgress(installProgress))
            }
            override fun onFinished(id: Int, success: Boolean) {
                sendBlocking(InstallStatus.InstallFinished(success))
            }
        })

        awaitClose()
    }

    sealed class InstallStatus {
        data class InstallProgress(val progress: Int) : InstallStatus()
        data class InstallFinished(val isSuccess: Boolean) : InstallStatus()
    }

    companion object {
        private const val DOWNLOAD_ID_KEY = "Download"
        private const val DOWNLOAD_ID_NAME = "Download ID"
        private const val INSTALL_TYPE = "application/vnd.android.package-archive"
    }
}