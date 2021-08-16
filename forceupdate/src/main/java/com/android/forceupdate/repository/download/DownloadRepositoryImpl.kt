package com.android.forceupdate.repository.download

import android.app.DownloadManager
import android.app.DownloadManager.*
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.net.Uri
import com.android.forceupdate.R
import com.android.forceupdate.repository.download.DownloadRepositoryImpl.DownloadStatus.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

class DownloadRepositoryImpl(
    private val context: Context
) : DownloadRepository {

    override fun downloadApk(apkLink: String, header: Pair<String,String>?) = flow {
        try {
            val request = Request(Uri.parse(apkLink)).apply {
                this.setAllowedOverRoaming(true)
                this.setAllowedOverMetered(true)
                this.setNotificationVisibility(Request.VISIBILITY_VISIBLE)
                this.setDestinationInExternalFilesDir(context, null, APK_FILE_NAME)
                header?.let { this.addRequestHeader(header.first, header.second) }
            }

            val downloadManager   = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val query             = getDownloadQuery(request, downloadManager)
            var isDownloading     = true

            while (isDownloading) {
                val cursor = downloadManager.query(query)
                if (cursor != null && cursor.count >= 0 && cursor.moveToFirst()) {
                    val downloadStatus = getDownloadStatus(cursor)
                    this.emit(downloadStatus)
                    if (downloadStatus !is Progress) isDownloading = false
                } else {
                    isDownloading = false
                    this.emit(Canceled(context.getString(R.string.download_canceled)))
                }
            }
        } catch (illegalArgumentException: IllegalArgumentException) {
            this@flow.emit(Canceled(context.getString(R.string.download_wrong_link)))
        } catch (exception: Exception) {
            exception.localizedMessage?.let { this@flow.emit(Canceled(it)) }
        }
    }.flowOn(Dispatchers.IO)

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

        return when (status) {
            STATUS_PAUSED     -> Canceled(context.getString(R.string.download_paused, reason))
            STATUS_FAILED     -> Canceled(context.getString(R.string.download_failed, reason))
            STATUS_RUNNING    -> Progress(percentage)
            STATUS_SUCCESSFUL -> Completed(uri)
            else              -> Canceled(reason)
        }
    }

    private fun getReason(reasonCode: Int): String {
        return when (reasonCode) {
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
    }

    override fun writeFileToInternalStorage(uri: String) {
        val file         = File(Uri.parse(uri).path)
        val outputStream = context.openFileOutput(file.name, MODE_PRIVATE)
        val inputStream  = file.inputStream()

        inputStream.copyTo(outputStream)

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        file.delete()
    }

    override fun getLocalFile() = File(context.filesDir, APK_FILE_NAME)

    sealed class DownloadStatus {
        data class Completed(val uri: String)   : DownloadStatus()
        data class Progress(val progress: Int)  : DownloadStatus()
        data class Canceled(val reason: String) : DownloadStatus()
    }

    companion object DownloadConstant {
        private const val APK_FILE_NAME    = "update.apk"
        private const val DOWNLOAD_ID_KEY  = "download"
        private const val DOWNLOAD_ID_NAME = "download_id"
    }
}