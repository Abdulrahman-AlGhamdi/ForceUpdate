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
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_APPLICATION_NAME
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_LOGO_IMAGE
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_VERSION_CODE
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_VERSION_NAME

class ForceUpdateManager(private val activity: Activity) {

    fun checkAppVersion(updateVersion: Int): Boolean {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return if (SDK_INT >= P)
            updateVersion > packageInfo.longVersionCode
        else
            updateVersion > packageInfo.versionCode
    }

    fun updateApplication(
        apkLink: String,
        logo: Int? = null,
        versionCode: Int? = null,
        versionName: String? = null,
        applicationName: String? = null
    ) {
        val intent = Intent(activity, ForceUpdateActivity::class.java).apply {
            this.putExtra(EXTRA_LOGO_IMAGE, logo)
            this.putExtra(EXTRA_APK_LINK, apkLink)
            this.putExtra(EXTRA_VERSION_CODE, versionCode)
            this.putExtra(EXTRA_VERSION_NAME, versionName)
            this.putExtra(EXTRA_APPLICATION_NAME, applicationName)
        }
        activity.startActivity(intent)
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