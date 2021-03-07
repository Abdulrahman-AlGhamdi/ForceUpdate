package com.ss.forceupdate

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File

class ForceUpdate(private val activity: Activity, private val context: Context) {

    fun isApplicationUpdated(newVersion: String) {
        val sharedPreferences = activity.getSharedPreferences(FORCE_UPDATE_NAME, Context.MODE_PRIVATE)
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
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        DOWNLOAD_ID = downloadManager.enqueue(request)
    }

    val downloadBroadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (DOWNLOAD_ID == id)
                Toast.makeText(context, "Downloaded", Toast.LENGTH_SHORT).show()
        }
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
        const val DOWNLOAD_URI = "https://upload.wikimedia.org/wikipedia/commons/6/66/Android_robot.png"
    }
}