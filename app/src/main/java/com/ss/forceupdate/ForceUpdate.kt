package com.ss.forceupdate

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.*

class ForceUpdate(private val activity: Activity, private val context: Context) {

    private lateinit var downloadManager: DownloadManager

    fun isApplicationUpdated(newVersion: String) {
        val sharedPreferences = activity.getSharedPreferences(
            FORCE_UPDATE_NAME,
            Context.MODE_PRIVATE
        )
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val applicationVersion = packageInfo.versionName
        val editor = sharedPreferences.edit()
        val check = applicationVersion.toDouble() >= newVersion.toDouble()
        editor.putBoolean(IS_UPDATED_KEY, check)
        editor.apply()

        if (!sharedPreferences.getBoolean(IS_UPDATED_KEY, false))
            showUpdateDialog()
    }

    private fun showUpdateDialog() {
        updateDialog = AlertDialog.Builder(context)
            .setCancelable(false)
            .setTitle(DIALOG_TITLE)
            .setMessage(DIALOG_MESSAGE)
            .setPositiveButton(DIALOG_POSITIVE_BUTTON, null)
            .create()

        updateDialog.setOnShowListener {
            updateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                downloadApk()
            }
        }

        updateDialog.show()
    }

    private fun downloadApk() {
        val request = DownloadManager.Request(Uri.parse(DOWNLOAD_URI))
            .setTitle(DOWNLOAD_TITLE)
            .setDescription(DOWNLOAD_DESCRIPTION)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverRoaming(true)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "update.apk")

        downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        DOWNLOAD_ID = downloadManager.enqueue(request)
    }

    val downloadBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (DOWNLOAD_ID == id) {
                val file = File("/storage/emulated/0/Download/update.apk")
                val uri = FileProvider.getUriForFile(context, context.packageName, file)
                saveFile(uri)
                installApk(uri)
            }
        }
    }

    private fun saveFile(uri: Uri) {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor != null && cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            val descriptor = contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor
            val inputStream = FileInputStream(descriptor)
            context.openFileOutput(name, Context.MODE_PRIVATE).write(inputStream.readBytes())
        }
        cursor?.close()
    }

    private fun installApk(uri: Uri) {
        val file = context.filesDir.listFiles().first()
        val packageInstaller = context.packageManager.packageInstaller
        val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(sessionParams)
        val session = packageInstaller.openSession(sessionId)

        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        val statusReceiver = pendingIntent.intentSender
        session.commit(statusReceiver)
        file.delete()
    }

    companion object {
        // Shared Preferences
        const val IS_UPDATED_KEY = "isUpdated"
        const val FORCE_UPDATE_NAME = "Force Update"

        // Update Dialog
        lateinit var updateDialog: AlertDialog
        const val DIALOG_TITLE = "New Update"
        const val DIALOG_MESSAGE = "New Update Available"
        const val DIALOG_POSITIVE_BUTTON = "Update"

        // Download Manager
        var DOWNLOAD_ID = -1L
        const val DOWNLOAD_TITLE = "New Version"
        const val DOWNLOAD_DESCRIPTION = "Downloading..."
        const val DOWNLOAD_URI = "https://storage.evozi.com/apk/dl/21/03/08/com.ss.gpacalculator_2.apk"

        const val TAG = "JAVA_INSTALLER"
        const val PACKAGE_INSTALLED_ACTION = "com.ss.forceupdate.SESSION_API_PACKAGE_INSTALLED"
    }
}