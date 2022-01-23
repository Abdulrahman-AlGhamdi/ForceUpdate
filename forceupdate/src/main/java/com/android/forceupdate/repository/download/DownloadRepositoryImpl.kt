package com.android.forceupdate.repository.download

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.net.Uri
import com.android.forceupdate.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DownloadRepositoryImpl(
    private val context: Context
) : DownloadRepository {

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.DownloadIdle)
    override val downloadStatus = _downloadStatus.asStateFlow()

    override suspend fun downloadApk(apkLink: String, header: Pair<*, *>?) {
        try {
            val request = Request(Uri.parse(apkLink)).apply {
                this.setAllowedOverRoaming(true)
                this.setAllowedOverMetered(true)
                this.setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                this.setDestinationInExternalFilesDir(context, null, APK_FILE_NAME)
                header?.let { this.addRequestHeader(header.first as String, header.second as String) }
            }

            val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val query           = getDownloadQuery(request, downloadManager)
            var isDownloading   = true

            _downloadStatus.value = DownloadStatus.DownloadProgress(0)
            delay(1000)

            while (isDownloading) {
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.count >= 0 && cursor.moveToFirst()) {
                    val downloadStatus = getDownloadStatus(cursor)
                    _downloadStatus.value = downloadStatus
                    if (downloadStatus !is DownloadStatus.DownloadProgress) isDownloading = false
                } else {
                    isDownloading = false
                    _downloadStatus.value = DownloadStatus.DownloadCanceled(context.getString(R.string.download_canceled))
                }
            }
        } catch (illegalArgumentException: IllegalArgumentException) {
            _downloadStatus.value = DownloadStatus.DownloadCanceled(context.getString(R.string.download_wrong_link))
        } catch (exception: Exception) {
            exception.localizedMessage?.let { _downloadStatus.value = DownloadStatus.DownloadCanceled(it) }
        }
    }

    private fun getDownloadQuery(request: Request, downloadManager: DownloadManager): Query {
        val sharedPreferences = context.getSharedPreferences(DOWNLOAD_ID_NAME, MODE_PRIVATE)
        val oldDownloadId     = sharedPreferences.getLong(DOWNLOAD_ID_KEY, -1L)
        val newDownloadId     = downloadManager.enqueue(request)
        val query             = Query().setFilterById(newDownloadId)

        sharedPreferences.edit().apply {
            downloadManager.remove(oldDownloadId)
            this.putLong(DOWNLOAD_ID_KEY, newDownloadId)
            this.apply()
        }

        return query
    }

    private fun getDownloadStatus(cursor: Cursor): DownloadStatus {
        val bytes      = cursor.getInt(cursor.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val totalSize  = cursor.getInt(cursor.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES))
        val status     = cursor.getInt(cursor.getColumnIndex(COLUMN_STATUS))
        val uri        = cursor.getString(cursor.getColumnIndex(COLUMN_LOCAL_URI))
        val reason     = getReason(cursor.getInt(cursor.getColumnIndex(COLUMN_REASON)))
        val percentage = ((bytes.toDouble() / totalSize) * 100).toInt()
        cursor.close()

        return when (status) {
            STATUS_PAUSED     -> DownloadStatus.DownloadCanceled(context.getString(R.string.download_paused, reason))
            STATUS_FAILED     -> DownloadStatus.DownloadCanceled(context.getString(R.string.download_failed, reason))
            STATUS_RUNNING    -> DownloadStatus.DownloadProgress(percentage)
            STATUS_SUCCESSFUL -> DownloadStatus.DownloadCompleted(uri)
            else              -> DownloadStatus.DownloadCanceled(reason)
        }
    }

    private fun getReason(reasonCode: Int) = when (reasonCode) {
        1    -> "Waiting to Retry"
        2    -> "Waiting for Network"
        3    -> "Queued for WIFI"
        4    -> "Unknown"
        1001 -> "File Error"
        1002 -> "Unhandled HTTP Code"
        1004 -> "HTTP Data Error"
        1005 -> "Too Many Redirects"
        1006 -> "Insufficient Space"
        1007 -> "Device Not Found"
        1008 -> "Cannot Resume"
        1009 -> "File Already Exists"
        else -> "Undefined"
    }

    override suspend fun writeFileToInternalStorage(uri: String) {
        val file = File(Uri.parse(uri).path)

        if (file.exists()) {
            val outputStream = context.openFileOutput(file.name, MODE_PRIVATE)
            val inputStream  = file.inputStream()

            inputStream.copyTo(outputStream)

            outputStream.flush()
            outputStream.close()
            inputStream.close()
            file.delete()
        } else _downloadStatus.value = DownloadStatus.DownloadCanceled("File deleted")
    }

    override fun getLocalFile() = File(context.filesDir, APK_FILE_NAME)

    sealed class DownloadStatus {
        object DownloadIdle                             : DownloadStatus()
        data class DownloadCompleted(val uri: String)   : DownloadStatus()
        data class DownloadProgress(val progress: Int)  : DownloadStatus()
        data class DownloadCanceled(val reason: String) : DownloadStatus()
    }

    companion object DownloadConstant {
        private const val APK_FILE_NAME    = "update.apk"
        private const val DOWNLOAD_ID_KEY  = "download"
        private const val DOWNLOAD_ID_NAME = "download_id"
    }
}