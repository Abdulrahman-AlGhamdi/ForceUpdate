package com.android.forceupdate.manager

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.P
import com.android.forceupdate.R
import com.android.forceupdate.ui.ForceUpdateActivity
import com.android.forceupdate.util.ConstantsUtils
import java.io.File

class ForceUpdateManager(private val activity: Activity) {

    fun checkAppVersion(updateVersion: Int): Boolean {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)

        return if (SDK_INT >= P) updateVersion > packageInfo.longVersionCode
        else updateVersion > packageInfo.versionCode
    }

    fun updateApplication(
        apkLink   : String,
        header    : Pair<String, String>? = null,
        optional  : Boolean = false,
        animation : String? = null
    ) {
        Intent(activity, ForceUpdateActivity::class.java).apply {
            this.putExtra(ConstantsUtils.EXTRA_APK_LINK, apkLink)
            this.putExtra(ConstantsUtils.EXTRA_OPTIONAL_DOWNLOAD, optional)
            header?.let { this.putExtra(ConstantsUtils.EXTRA_HEADER, it) }
            animation?.let { this.putExtra(ConstantsUtils.EXTRA_ANIMATION, animation) }
            activity.startActivity(this)
        }
    }

    fun deleteApkFile() {
        val apkFile = File(activity.filesDir, ConstantsUtils.APK_FILE_NAME)
        if (apkFile.exists()) apkFile.delete()
    }

    fun destroyApplication(dialogMessage: String? = null) {
        AlertDialog.Builder(activity).apply {
            this.setTitle(dialogMessage ?: activity.getString(R.string.forceupdate_dialog_message))
            this.setCancelable(false)
            this.setPositiveButton(activity.getString(R.string.forceupdate_dialog_confirm), null)
            this.create()
        }.show().apply {
            this.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val activityManager = activity.getSystemService(ACTIVITY_SERVICE) as ActivityManager
                activityManager.clearApplicationUserData()
            }
        }
    }
}