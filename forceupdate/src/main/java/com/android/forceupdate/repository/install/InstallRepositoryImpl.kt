package com.android.forceupdate.repository.install

import android.app.DownloadManager.*
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.pm.PackageInstaller.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.net.toUri
import com.android.forceupdate.broadcast.InstallBroadcastReceiver
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.*
import com.android.forceupdate.broadcast.InstallBroadcastReceiver.InstallStatus.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.io.File

internal class InstallRepositoryImpl(private val context: Context) : InstallRepository {

    override fun installApk(localFile: File) = callbackFlow {

        val resultReceiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
                super.onReceiveResult(resultCode, resultData)
                resultData.getParcelable<InstallStatus>(EXTRA_BUNDLE)?.let { installStatus ->
                    this@callbackFlow.trySend(installStatus)
                }
            }
        }

        val pendingIntent = getIntent(resultReceiver, localFile)
        startInstalling(localFile, pendingIntent)

        this.awaitClose()
    }.flowOn(Dispatchers.IO)

    private fun getIntent(resultReceiver: ResultReceiver, localFile: File): PendingIntent {
        val intent = Intent(context, InstallBroadcastReceiver::class.java).apply {
            this.putExtra(EXTRA_BUNDLE, Bundle().apply {
                this.putParcelable(RESULT_RECEIVER, resultReceiver)
                this.putSerializable(LOCAL_FILE, localFile)
            })
        }

        return getBroadcast(context, 2, intent, FLAG_UPDATE_CURRENT or FLAG_MUTABLE)
    }

    private fun startInstalling(localFile: File, pendingIntent: PendingIntent) {
        val packageInstaller = context.packageManager.packageInstaller
        val contentResolver  = context.contentResolver

        contentResolver.openInputStream(localFile.toUri())?.use { apkStream ->
            val sessionParams = SessionParams(SessionParams.MODE_FULL_INSTALL)
            val sessionId     = packageInstaller.createSession(sessionParams)
            val session       = packageInstaller.openSession(sessionId)

            session.openWrite(localFile.name, 0, -1).use { sessionStream ->
                apkStream.copyTo(sessionStream)
                session.fsync(sessionStream)
            }

            session.commit(pendingIntent.intentSender)
            session.close()
        }
    }

    companion object {
        const val LOCAL_FILE = "local_file"
        const val EXTRA_BUNDLE = "extra_bundle"
        const val RESULT_RECEIVER = "result_receiver"
    }
}