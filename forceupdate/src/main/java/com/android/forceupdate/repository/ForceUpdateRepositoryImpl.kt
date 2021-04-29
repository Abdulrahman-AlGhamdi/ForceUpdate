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
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.android.forceupdate.R
import com.android.forceupdate.broadcast.InstallBroadcastReceiver
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.*
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.DownloadStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

internal class ForceUpdateRepositoryImpl(private val context: Context) : ForceUpdateRepository {

    override suspend fun downloadApk(apkLink: String, header: Pair<String,String>?) = flow {
        try {
            val request = Request(Uri.parse(apkLink)).apply {
                this.setAllowedOverRoaming(true)
                this.setAllowedOverMetered(true)
                this.setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                this.setDestinationInExternalFilesDir(context, null, APK_FILE_NAME)
                header?.let { this.addRequestHeader(header.first, header.second) }
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

            var isDownloading = true
            while (isDownloading) {
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.count >= 0 && cursor.moveToFirst()) {
                    val bytes = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalSize = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES))
                    val status = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS))
                    val percentage = ((bytes.toDouble() / totalSize) * 100).toInt()

                    when (status) {
                        STATUS_PAUSED -> {
                            this@flow.emit(DownloadCanceled(context.getString(R.string.download_paused)))
                            isDownloading = false
                        }
                        STATUS_FAILED -> {
                            this@flow.emit(DownloadCanceled(context.getString(R.string.download_failed)))
                            isDownloading = false
                        }
                        STATUS_RUNNING -> {
                            this@flow.emit(DownloadProgress(percentage))
                        }
                        STATUS_SUCCESSFUL -> {
                            val uri = cursor.getString(cursor.getColumnIndex(COLUMN_LOCAL_URI))
                            Uri.parse(uri).path?.let { externalPath ->
                                writeFileToInternalStorage(File(externalPath))
                                this@flow.emit(DownloadCompleted)
                                isDownloading = false
                            }
                        }
                    }
                } else {
                    this@flow.emit(DownloadCanceled(context.getString(R.string.download_canceled)))
                    isDownloading = false
                }
            }
        } catch (illegalArgumentException: IllegalArgumentException) {
            this@flow.emit(DownloadCanceled(context.getString(R.string.download_wrong_link)))
        } catch (exception: Exception) {
            exception.message?.let { this@flow.emit(DownloadCanceled(it)) }

        }
    }.flowOn(Dispatchers.IO)

    private fun writeFileToInternalStorage(file: File) {
        val outputStream = context.openFileOutput(file.name, MODE_PRIVATE).buffered(32768)
        val inputStream = file.inputStream().buffered(32768)

        var read = 0
        while ({ read = inputStream.read(); read != -1 }()) {
            outputStream.write(read)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        file.delete()
    }

    override fun getLocalFile() = File(context.filesDir, APK_FILE_NAME)

    sealed class DownloadStatus {
        data class DownloadProgress(val progress: Int) : DownloadStatus()
        data class DownloadCanceled(val message: String) : DownloadStatus()
        object DownloadCompleted : DownloadStatus()
    }

    override fun installApk(localFile: File) = callbackFlow<InstallStatus> {
        val packageInstaller = context.packageManager.packageInstaller
        val contentResolver = context.contentResolver

        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                super.onReceiveResult(resultCode, resultData)
                super.onReceiveResult(resultCode, resultData)

                resultData.getParcelable<InstallStatus>(EXTRA_BUNDLE)?.let { installStatus ->
                    this@callbackFlow.offer(installStatus)
                }
            }
        }

        val intent = Intent(context, InstallBroadcastReceiver::class.java).apply {
            this.putExtra(EXTRA_BUNDLE, Bundle().apply {
                this.putParcelable(RESULT_RECEIVER, resultReceiver)
                this.putSerializable(LOCAL_FILE, localFile)
            })
        }

        contentResolver.openInputStream(localFile.toUri())?.use { apkStream ->
            val length = DocumentFile.fromSingleUri(context, localFile.toUri())?.length() ?: -1
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

        this.awaitClose()
    }

    companion object {
        const val LOCAL_FILE = "local_file"
        const val EXTRA_BUNDLE = "extra_bundle"
        const val RESULT_RECEIVER = "result_receiver"
        private const val APK_FILE_NAME = "update.apk"
        private const val DOWNLOAD_ID_KEY = "download"
        private const val DOWNLOAD_ID_NAME = "download_id"
    }
}