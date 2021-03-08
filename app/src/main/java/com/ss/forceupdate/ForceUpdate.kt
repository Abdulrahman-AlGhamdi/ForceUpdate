package com.ss.forceupdate

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.net.Uri
import android.util.Log
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
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        DOWNLOAD_ID = downloadManager.enqueue(request)

        val files = File("/storage/emulated/0/Android/data")
        if (files.listFiles() != null)
            for (file: File in files.listFiles())
                Log.d("ForceUpdate", file.name)


        downloadManager.query(DownloadManager.Query()).use {
            while(it != null && it.moveToNext()) {
                val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))

                Log.d("ForceUpdate", "URI = $uri")
//                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
//                when(status) {
//                    DownloadManager.STATUS_SUCCESSFUL -> {
//                        Log.d("ForceUpdate", "STATUS_SUCCESSFUL")
//                    }
//                    DownloadManager.STATUS_FAILED -> {
//                        Log.d("ForceUpdate", "STATUS_FAILED")
//                    }
//                    DownloadManager.STATUS_PAUSED -> {
//                        Log.d("ForceUpdate", "STATUS_PAUSED")
//                    }
//                    DownloadManager.STATUS_RUNNING -> {
//                        Log.d("ForceUpdate", "STATUS_RUNNING")
//                    }
//                    DownloadManager.STATUS_PENDING -> {
//                        Log.d("ForceUpdate", "STATUS_PENDING")
//                    }
//                }
            }
        }
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
        const val DOWNLOAD_URI = "https://redirector.gvt1.com/edgedl/android/studio/install/4.1.2.0/android-studio-ide-201.7042882-windows.exe"
    }
}