package com.android.forceupdate.repository.install

import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus
import kotlinx.coroutines.flow.Flow
import java.io.File

internal interface InstallRepository {

    fun installApk(localFile: File): Flow<InstallStatus>
}