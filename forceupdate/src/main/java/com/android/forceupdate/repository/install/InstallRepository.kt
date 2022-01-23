package com.android.forceupdate.repository.install

import com.android.forceupdate.repository.install.InstallRepositoryImpl.InstallStatus
import kotlinx.coroutines.flow.StateFlow
import java.io.File

internal interface InstallRepository {

    val installStatus: StateFlow<InstallStatus>

    suspend fun installApk(localFile: File)
}