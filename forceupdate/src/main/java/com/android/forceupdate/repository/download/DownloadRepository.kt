package com.android.forceupdate.repository.download

import com.android.forceupdate.repository.download.DownloadRepositoryImpl.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

interface DownloadRepository {

    fun downloadApk(apkLink: String, header: Pair<String, String>?): Flow<DownloadStatus>

    fun writeFileToInternalStorage(uri: String)

    fun getLocalFile() : File
}