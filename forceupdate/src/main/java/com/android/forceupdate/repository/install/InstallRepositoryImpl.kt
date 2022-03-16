package com.android.forceupdate.repository.install

import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.*
import android.os.Build.VERSION.SDK_INT
import androidx.core.net.toUri
import com.android.forceupdate.broadcast.InstallBroadcastReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.parcelize.Parcelize
import java.io.File

internal class InstallRepositoryImpl(private val context: Context) : InstallRepository {

    private val _installStatus = MutableStateFlow<InstallStatus>(InstallStatus.InstallIdle)
    override val installStatus = _installStatus.asStateFlow()

    override suspend fun installApk(localFile: File) {

        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                super.onReceiveResult(resultCode, resultData)
                resultData.getParcelable<InstallStatus>(EXTRA_BUNDLE)?.let { installStatus ->
                    _installStatus.value = installStatus
                }
            }
        }

        val pendingIntent = getIntent(resultReceiver, localFile)
        startInstalling(localFile, pendingIntent)
    }

    private fun getIntent(resultReceiver: ResultReceiver, localFile: File): PendingIntent {
        val intent = Intent(context, InstallBroadcastReceiver::class.java).apply {
            this.putExtra(EXTRA_BUNDLE, Bundle().apply {
                this.putParcelable(RESULT_RECEIVER, resultReceiver)
                this.putSerializable(LOCAL_FILE, localFile)
            })
        }

        return if (SDK_INT < 31) getBroadcast(context, 2, intent, FLAG_UPDATE_CURRENT)
        else getBroadcast(context, 2, intent, FLAG_UPDATE_CURRENT or FLAG_MUTABLE)
    }

    private fun startInstalling(localFile: File, pendingIntent: PendingIntent) = if (localFile.exists()) {
        val packageInstaller = context.packageManager.packageInstaller
        val contentResolver  = context.contentResolver

        contentResolver.openInputStream(localFile.toUri())?.use { apkStream ->
            val sessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val sessionId     = packageInstaller.createSession(sessionParams)
            val session       = packageInstaller.openSession(sessionId)

            session.openWrite(localFile.name, 0, -1).use { sessionStream ->
                apkStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }

            session.commit(pendingIntent.intentSender)
            session.close()
        }
    } else _installStatus.value = InstallStatus.InstallFailure("File deleted")

    sealed class InstallStatus: Parcelable {
        @Parcelize object InstallIdle                             : InstallStatus(), Parcelable
        @Parcelize object InstallProgress                         : InstallStatus(), Parcelable
        @Parcelize object InstallCanceled                         : InstallStatus(), Parcelable
        @Parcelize object InstallSucceeded                        : InstallStatus(), Parcelable
        @Parcelize data class InstallFailure(val message: String) : InstallStatus(), Parcelable
    }

    companion object {
        const val LOCAL_FILE      = "local_file"
        const val EXTRA_BUNDLE    = "extra_bundle"
        const val RESULT_RECEIVER = "result_receiver"
    }
}