package com.android.forceupdate.repository

import android.content.pm.PackageInstaller
import androidx.lifecycle.LiveData
import com.android.forceupdate.common.DownloadStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ForceUpdateRepository {

    suspend fun downloadApk(apkLink: String): Flow<DownloadStatus>

    fun installApk(localFile: File): PackageInstaller
}