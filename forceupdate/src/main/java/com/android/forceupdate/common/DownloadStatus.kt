package com.android.forceupdate.common

import java.io.File

sealed class DownloadStatus {
    data class DownloadingProgress(val progress: Int) : DownloadStatus()
    data class DownloadCompleted(val localFile: File) : DownloadStatus()
    object DownloadCanceled : DownloadStatus()
}