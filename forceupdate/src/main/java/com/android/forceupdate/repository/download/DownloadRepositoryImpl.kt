package com.android.forceupdate.repository.download

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Context.MODE_PRIVATE
import android.net.Uri
import com.android.forceupdate.R
import com.android.forceupdate.repository.download.DownloadRepositoryImpl.DownloadStatus.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DownloadRepositoryImpl(
    private val context: Context
) : DownloadRepository {

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadIdle)
    override val downloadStatus = _downloadStatus.asStateFlow()

    private val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager

    override suspend fun downloadApk(apkLink: String, header: Pair<*, *>?) = try {
        val request = DownloadManager.Request(Uri.parse(apkLink)).apply {
            this.setAllowedOverRoaming(true)
            this.setAllowedOverMetered(true)
            this.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            this.setDestinationInExternalFilesDir(context, null, APK_FILE_NAME)
            header?.let { this.addRequestHeader(header.first as String, header.second as String) }
        }

        val query = getDownloadQuery(request)
        do getDownloadStatus(query) while (_downloadStatus.value is DownloadProgress)

    } catch (illegalArgumentException: IllegalArgumentException) {
        _downloadStatus.value = DownloadCanceled(context.getString(R.string.download_wrong_link))
    } catch (exception: Exception) {
        exception.localizedMessage?.let { _downloadStatus.value = DownloadCanceled(it) }
        exception.printStackTrace()
    }

    private fun getDownloadQuery(request: DownloadManager.Request): DownloadManager.Query {
        val sharedPreferences = context.getSharedPreferences(DOWNLOAD_ID_NAME, MODE_PRIVATE)
        val oldDownloadId = sharedPreferences.getLong(DOWNLOAD_ID_KEY, -1L)
        val newDownloadId = downloadManager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(newDownloadId)

        sharedPreferences.edit().apply {
            downloadManager.remove(oldDownloadId)
            this.putLong(DOWNLOAD_ID_KEY, newDownloadId)
            this.apply()
        }

        return query
    }

    private fun getDownloadStatus(query: DownloadManager.Query) {
        val cursor = downloadManager.query(query)

        if (cursor == null || cursor.count < 0 || !cursor.moveToFirst()) {
            _downloadStatus.value = DownloadCanceled(context.getString(R.string.download_canceled))
            return
        }

        val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
        val status = cursor.getInt(statusColumn)
        val localUriColumn = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
        val localUri = cursor.getString(localUriColumn)
        val totalSizeColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
        val totalSize = cursor.getInt(totalSizeColumn)
        val reasonColumn = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
        val reasonNumber = cursor.getInt(reasonColumn)
        val reasonMessage = getReasonMessage(reasonNumber)
        val bytesDownloaded = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val bytes = cursor.getInt(bytesDownloaded)
        cursor.close()

        val percentage = ((bytes.toDouble() / totalSize) * 100).toInt()
        val message = context.getString(R.string.download_paused, reasonMessage)

        when (status) {
            DownloadManager.STATUS_PAUSED -> _downloadStatus.value = DownloadCanceled(message)
            DownloadManager.STATUS_FAILED -> _downloadStatus.value = DownloadCanceled(message)
            DownloadManager.STATUS_PENDING -> _downloadStatus.value = DownloadProgress(0)
            DownloadManager.STATUS_RUNNING -> _downloadStatus.value = DownloadProgress(percentage)
            DownloadManager.STATUS_SUCCESSFUL -> writeFileToInternalStorage(localUri)
            else -> _downloadStatus.value = DownloadCanceled(reasonMessage)
        }
    }

    private fun getReasonMessage(reasonCode: Int) = when (reasonCode) {
        DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Queued for WIFI"
        DownloadManager.PAUSED_UNKNOWN -> "Unknown paused"
        DownloadManager.ERROR_UNKNOWN -> "Unknown error"
        DownloadManager.ERROR_FILE_ERROR -> "File error"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
        DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume"
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
        else -> "Undefined reason"
    }

    private fun writeFileToInternalStorage(uri: String) {
        val uriPath = Uri.parse(uri)?.path ?: ""
        val file = File(uriPath)

        val outputStream = context.openFileOutput(APK_FILE_NAME, MODE_PRIVATE)
        val inputStream = file.inputStream()

        inputStream.copyTo(outputStream)

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        file.delete()

        _downloadStatus.value = DownloadCompleted
    }

    override fun getLocalFile() = File(context.filesDir, APK_FILE_NAME)

    sealed class DownloadStatus {
        object DownloadIdle : DownloadStatus()
        object DownloadCompleted : DownloadStatus()
        data class DownloadProgress(val progress: Int) : DownloadStatus()
        data class DownloadCanceled(val reason: String) : DownloadStatus()
    }

    private companion object DownloadConstant {
        private const val APK_FILE_NAME = "update.apk"
        private const val DOWNLOAD_ID_KEY = "download"
        private const val DOWNLOAD_ID_NAME = "download_id"
    }
}