package com.android.forceupdate.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build.*
import android.os.Build.VERSION.*
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.android.forceupdate.R
import com.google.android.material.snackbar.Snackbar
import java.io.File

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
}

fun View.showSnackBar(
    message: String,
    length: Int = Snackbar.LENGTH_SHORT,
    anchorView: Int? = null,
    actionMessage: String? = null,
    action: (View) -> Unit = {}
) {
    Snackbar.make(this, message, length).apply {
        actionMessage?.let { this.setAction(actionMessage) { action(it) } }
        anchorView?.let { this.setAnchorView(anchorView) }
    }.show()
}

fun PackageInfo.getAppVersion(context: Context): String {
    val versionName = this.versionName
    val versionCode = if (SDK_INT >= VERSION_CODES.P) this.longVersionCode else this.versionCode
    val appVersion = "$versionCode ($versionName)"

    return context.getString(R.string.forceupdate_current_version, appVersion)
}

fun Activity.keepScreenOn(keep: Boolean) {
    if (keep) this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    else this.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

fun File.getSize(fileSize: Long): String {
    val fileSizeInKB = fileSize.div(1024)
    val fileSizeInMB = fileSize.div(1024 * 1024)

    return when {
        fileSizeInKB < 1 -> "$fileSize B"
        fileSizeInMB < 1 -> "$fileSizeInKB KB"
        else -> "$fileSizeInMB MB"
    }
}