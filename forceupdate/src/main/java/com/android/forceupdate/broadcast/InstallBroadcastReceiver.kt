package com.android.forceupdate.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller.*
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.Companion.LOCAL_FILE
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class InstallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val localFile = intent.getSerializableExtra(LOCAL_FILE) as? File

        when (intent.getIntExtra(EXTRA_STATUS, -1)) {
            STATUS_PENDING_USER_ACTION -> {
                val installIntent = intent.getParcelableExtra(EXTRA_INTENT) as? Intent
                context.startActivity(installIntent?.addFlags(FLAG_ACTIVITY_NEW_TASK))
            }
            STATUS_SUCCESS -> {
                localFile?.delete()
                mutableInstallStatus.value = InstallSucceeded
            }
            STATUS_FAILURE_ABORTED -> {
                localFile?.delete()
                mutableInstallStatus.value = InstallCanceled
            }
            else -> {
                localFile?.delete()
                val message = intent.getStringExtra(EXTRA_STATUS_MESSAGE)
                mutableInstallStatus.value = InstallError(message.toString())
            }
        }
    }

    sealed class InstallStatus {
        object InstallInitialized : InstallStatus()
        object InstallSucceeded : InstallStatus()
        object InstallCanceled : InstallStatus()
        data class InstallError(val message: String) : InstallStatus()
    }

    companion object {
        val mutableInstallStatus = MutableStateFlow<InstallStatus>(InstallInitialized)
        val installStatus get() = mutableInstallStatus
    }
}