package com.android.forceupdate.repository.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.net.toUri
import com.android.forceupdate.R
import com.android.forceupdate.broadcast.InstallBroadcastReceiver
import com.android.forceupdate.repository.install.InstallRepositoryImpl.InstallStatus.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import java.io.File

internal class InstallRepositoryImpl(private val context: Context) : InstallRepository {

    private val _installStatus = MutableStateFlow<InstallStatus>(InstallIdle)
    override val installStatus = _installStatus.asStateFlow()

    override suspend fun installApk(localFile: File) {
        if (localFile.exists()) startInstalling(localFile, getPendingIntent())
        else _installStatus.value = InstallFailure(context.getString(R.string.install_failed))

        InstallBroadcastReceiver.installBroadcastState.collect {
            if (it is InstallFailure || it is InstallSucceeded) localFile.delete()
            _installStatus.value = it
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, InstallBroadcastReceiver::class.java)
        val intentFlag = if (VERSION.SDK_INT < VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT
        else PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, 2, intent, intentFlag)
    }

    private fun startInstalling(localFile: File, pendingIntent: PendingIntent) {
        val packageInstaller = context.packageManager.packageInstaller
        val contentResolver = context.contentResolver

        contentResolver.openInputStream(localFile.toUri())?.use { apkStream ->
            val installMode = PackageInstaller.SessionParams.MODE_FULL_INSTALL
            val sessionParams = PackageInstaller.SessionParams(installMode)
            val sessionId = packageInstaller.createSession(sessionParams)
            val session = packageInstaller.openSession(sessionId)

            session.openWrite(localFile.name, 0, -1).use { sessionStream ->
                apkStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }

            session.commit(pendingIntent.intentSender)
            session.close()
        }
    }

    sealed class InstallStatus {
        object InstallIdle : InstallStatus()
        object InstallProgress : InstallStatus()
        object InstallCanceled : InstallStatus()
        object InstallSucceeded : InstallStatus()
        data class InstallFailure(val message: String) : InstallStatus()
    }
}