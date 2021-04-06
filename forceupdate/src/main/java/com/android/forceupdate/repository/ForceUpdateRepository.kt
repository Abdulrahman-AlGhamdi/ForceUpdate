package com.android.forceupdate.repository

import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.DownloadStatus
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.InstallStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ForceUpdateRepository {

    suspend fun downloadApk(apkLink: String): Flow<DownloadStatus>

    fun installApk(localFile: File): Flow<InstallStatus>
}