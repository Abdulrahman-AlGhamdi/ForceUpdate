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
import javax.inject.Singleton

@Singleton
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

    fun destroyApplication(message: String? = null) {
        AlertDialog.Builder(activity).apply {
            this.setTitle(message ?: activity.getString(R.string.forceupdate_dialog_message))
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

    companion object {
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_LOGO_IMAGE = "logo"
        const val EXTRA_VERSION_NAME = "version name"
        const val EXTRA_VERSION_CODE = "version code"
        const val EXTRA_APPLICATION_NAME = "application name"
    }
}