package com.android.forceupdate.repository

import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

internal interface ForceUpdateRepository {

    suspend fun downloadApk(apkLink: String): Flow<DownloadStatus>

    fun getLocalFile() : File

    fun installApk(localFile: File): Flow<InstallStatus>
}