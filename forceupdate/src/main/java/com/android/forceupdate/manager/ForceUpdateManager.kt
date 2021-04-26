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
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_APK_LINK

class ForceUpdateManager(private val activity: Activity) {

    fun checkAppVersion(updateVersion: Int): Boolean {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)

        return if (SDK_INT >= P)
            updateVersion > packageInfo.longVersionCode
        else
            updateVersion > packageInfo.versionCode
    }

    fun updateApplication(apkLink: String) {
        val intent = Intent(activity, ForceUpdateActivity::class.java).apply {
            this.putExtra(EXTRA_APK_LINK, apkLink)
            activity.startActivity(this)
        }
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