package com.android.forceupdate.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_INTENT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageInstaller
import android.os.ResultReceiver
import com.android.forceupdate.repository.install.InstallRepositoryImpl.Companion.EXTRA_BUNDLE
import com.android.forceupdate.repository.install.InstallRepositoryImpl.Companion.LOCAL_FILE
import com.android.forceupdate.repository.install.InstallRepositoryImpl.Companion.RESULT_RECEIVER
import com.android.forceupdate.repository.install.InstallRepositoryImpl.InstallStatus
import java.io.File

internal class InstallBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val bundle         = intent.getBundleExtra(EXTRA_BUNDLE)
        val resultReceiver = bundle?.getParcelable(RESULT_RECEIVER) as? ResultReceiver
        val localFile      = bundle?.getSerializable(LOCAL_FILE) as File

        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val installIntent = intent.getParcelableExtra(EXTRA_INTENT) as? Intent
                context.startActivity(installIntent?.addFlags(FLAG_ACTIVITY_NEW_TASK))
                bundle.putParcelable(EXTRA_BUNDLE, InstallStatus.InstallProgress)
                resultReceiver?.send(1, bundle)
            }
            PackageInstaller.STATUS_SUCCESS -> {
                if (localFile.exists()) localFile.delete()
                bundle.putParcelable(EXTRA_BUNDLE, InstallStatus.InstallSucceeded)
                resultReceiver?.send(1, bundle)
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                bundle.putParcelable(EXTRA_BUNDLE, InstallStatus.InstallCanceled)
                resultReceiver?.send(1, bundle)
            }
            else -> intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.let { message ->
                if (localFile.exists()) localFile.delete()
                bundle.putParcelable(EXTRA_BUNDLE, InstallStatus.InstallFailure(message))
                resultReceiver?.send(1, bundle)
            }
        }
    }
}