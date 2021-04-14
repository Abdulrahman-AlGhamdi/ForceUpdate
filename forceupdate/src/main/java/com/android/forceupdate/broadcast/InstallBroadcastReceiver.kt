package com.android.forceupdate.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller.*
import android.os.Parcelable
import android.os.ResultReceiver
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.Companion.EXTRA_BUNDLE
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.Companion.LOCAL_FILE
import com.android.forceupdate.repository.ForceUpdateRepositoryImpl.Companion.RESULT_RECEIVER
import kotlinx.parcelize.Parcelize
import java.io.File

class InstallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.getBundleExtra(EXTRA_BUNDLE)
        val resultReceiver = bundle?.getParcelable(RESULT_RECEIVER) as? ResultReceiver
        val localFile = bundle?.getSerializable(LOCAL_FILE) as File

        when (intent.getIntExtra(EXTRA_STATUS, -1)) {
            STATUS_PENDING_USER_ACTION -> {
                val installIntent = intent.getParcelableExtra(EXTRA_INTENT) as? Intent
                context.startActivity(installIntent?.addFlags(FLAG_ACTIVITY_NEW_TASK))
            }
            STATUS_SUCCESS -> {
                localFile.delete()
                bundle.putParcelable(EXTRA_BUNDLE, InstallSucceeded)
                resultReceiver?.send(1, bundle)
            }
            STATUS_FAILURE_ABORTED -> {
                localFile.delete()
                bundle.putParcelable(EXTRA_BUNDLE, InstallCanceled)
                resultReceiver?.send(1, bundle)
            }
            STATUS_FAILURE_STORAGE -> {
                localFile.delete()
                bundle.putParcelable(EXTRA_BUNDLE, InstallError(STORAGE_FULL))
                resultReceiver?.send(1, bundle)
            }
            else -> {
                localFile.delete()
                intent.getStringExtra(EXTRA_STATUS_MESSAGE)?.let { message ->
                    bundle.putParcelable(EXTRA_BUNDLE, InstallError(message))
                    resultReceiver?.send(1, bundle)
                }
            }
        }
    }

    sealed class InstallStatus: Parcelable {
        @Parcelize object InstallCanceled : InstallStatus(), Parcelable
        @Parcelize object InstallSucceeded : InstallStatus(), Parcelable
        @Parcelize data class InstallError(val message: String) : InstallStatus(), Parcelable
        @Parcelize data class InstallProgress(val progress: Int) : InstallStatus(), Parcelable
    }

    companion object {
        private const val STORAGE_FULL = "Not enough storage, please erase some files"
    }
}