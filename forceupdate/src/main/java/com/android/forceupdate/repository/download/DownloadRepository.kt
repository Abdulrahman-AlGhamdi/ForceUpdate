package com.android.forceupdate.repository.download

import com.android.forceupdate.repository.download.DownloadRepositoryImpl.DownloadStatus
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface DownloadRepository {

    val downloadStatus: StateFlow<DownloadStatus>

    suspend fun downloadApk(apkLink: String, header: Pair<*, *>?)

    fun getLocalFile() : File
}