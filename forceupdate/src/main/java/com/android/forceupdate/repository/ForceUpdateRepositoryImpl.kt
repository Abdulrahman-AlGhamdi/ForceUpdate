package com.android.forceupdate.repository

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.app.PendingIntent.*
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.pm.PackageInstaller.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.android.forceupdate.broadcast.InstallBroadcastReceiver
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.*
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.DownloadStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.File

internal class ForceUpdateRepositoryImpl(private val context: Context) : ForceUpdateRepository {

    override suspend fun downloadApk(apkLink: String) = flow {
        val request = Request(Uri.parse(apkLink)).apply {
            this.setAllowedOverRoaming(true)
            this.setAllowedOverMetered(true)
            this.setNotificationVisibility(Request.VISIBILITY_VISIBLE)
            this.setDestinationInExternalFilesDir(context, null, APK_FILE_NAME)
        }

        val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val sharedPreferences = context.getSharedPreferences(DOWNLOAD_ID_NAME, MODE_PRIVATE)
        val id = sharedPreferences.getLong(DOWNLOAD_ID_KEY, -1L)
        downloadManager.remove(id)
        val downloadId = downloadManager.enqueue(request)
        val query = Query().setFilterById(downloadId)

        sharedPreferences.edit().apply {
            this.putLong(DOWNLOAD_ID_KEY, downloadId)
            this.apply()
        }

        withContext(Dispatchers.IO) {
            while (true) {
                withContext(Dispatchers.Main) {

                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.count >= 0 && cursor.moveToFirst()) {

                        val bytes = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val totalSize = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES))
                        val status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS))
                        val percentage = ((bytes.toDouble() / totalSize) * 100).toInt()

                        if (status == STATUS_PENDING) {
                        }

                        if (status == STATUS_PAUSED) {
                            this@flow.emit(DownloadCanceled)
                            this.cancel()
                        }

                        if (status == STATUS_FAILED) {
                            this@flow.emit(DownloadCanceled)
                            this.cancel()
                        }

                        if (status == STATUS_RUNNING) {
                            this@flow.emit(DownloadProgress(percentage))
                        }

                        if (status == STATUS_SUCCESSFUL) {
                            val uri = cursor.getString(cursor.getColumnIndex(COLUMN_LOCAL_URI))
                            Uri.parse(uri).path?.let { path ->
                                val file = File(path)
                                this@flow.emit(DownloadCompleted(file))
                                this.cancel()
                            }
                        }
                    } else {
                        this@flow.emit(DownloadCanceled)
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
        val contentUri = FileProvider.getUriForFile(context, context.packageName, localFile)
        val packageInstaller = context.packageManager.packageInstaller
        val contentResolver = context.contentResolver

        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                super.onReceiveResult(resultCode, resultData)

                resultData.getParcelable<InstallStatus>(EXTRA_BUNDLE)?.let { installStatus ->
                    sendBlocking(installStatus)
                }
            }
        }

        val intent = Intent(context, InstallBroadcastReceiver::class.java).apply {
            this.putExtra(EXTRA_BUNDLE, Bundle().apply {
                this.putParcelable(RESULT_RECEIVER, resultReceiver)
                this.putSerializable(LOCAL_FILE, localFile)
            })
        }

        if (contentUri != null)
            contentResolver.openInputStream(contentUri)?.use { apkStream ->
                val length = DocumentFile.fromSingleUri(context, contentUri)?.length() ?: -1
                val sessionParams = SessionParams(SessionParams.MODE_FULL_INSTALL)
                val sessionId = packageInstaller.createSession(sessionParams)
                val session = packageInstaller.openSession(sessionId)

                session.openWrite(localFile.name, 0, length).use { sessionStream ->
                    apkStream.copyTo(sessionStream)
                    session.fsync(sessionStream)
                }

                val pendingIntent = getBroadcast(context, 2, intent, FLAG_UPDATE_CURRENT)
                session.commit(pendingIntent.intentSender)
                session.close()
            }

        packageInstaller.registerSessionCallback(object : SessionCallback() {
            override fun onCreated(sessionId: Int) {
            }
            override fun onBadgingChanged(sessionId: Int) {
            }
            override fun onActiveChanged(sessionId: Int, active: Boolean) {
            }
            override fun onProgressChanged(sessionId: Int, progress: Float) {
                sendBlocking(InstallProgress(((progress / 0.90000004) * 100).toInt()))
            }
            override fun onFinished(sessionId: Int, success: Boolean) {
            }
        })

        awaitClose()
    }

    companion object {
        const val LOCAL_FILE = "Local File"
        const val EXTRA_BUNDLE = "extra bundle"
        const val RESULT_RECEIVER = "result receiver"
        private const val APK_FILE_NAME = "update.apk"
        private const val DOWNLOAD_ID_KEY = "Download"
        private const val DOWNLOAD_ID_NAME = "Download ID"
    }
}