package com.android.forceupdate.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.android.forceupdate.ui.ForceUpdateActivity
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_APK_LINK
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_APPLICATION_NAME
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_LOGO_IMAGE
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_MESSAGE
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_TITLE

class ForceUpdateManager(
    private val activity: Activity,
    private val apkLink: String,
    private val versionCode: Any,
    private val title: String? = null,
    private val message: String? = null,
    private val logo: Int? = null,
    private val applicationName: String? = null
) {

    fun isApplicationUpdated() {
        val sharedPreferences = activity.getSharedPreferences(FORCE_UPDATE_NAME, Context.MODE_PRIVATE)
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)

        var isUpdated = true

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            isUpdated = packageInfo.longVersionCode >= (versionCode as Long)
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            isUpdated = packageInfo.versionCode >= (versionCode as Long)
        }

        sharedPreferences.edit().apply {
            putBoolean(IS_UPDATED_KEY, isUpdated)
            apply()
        }

        if (!sharedPreferences.getBoolean(IS_UPDATED_KEY, false)) {
            val intent = Intent(activity, ForceUpdateActivity::class.java).apply {
                this.putExtra(EXTRA_TITLE, title)
                this.putExtra(EXTRA_MESSAGE, message)
                this.putExtra(EXTRA_APK_LINK, apkLink)
                this.putExtra(EXTRA_LOGO_IMAGE, logo)
                this.putExtra(EXTRA_APPLICATION_NAME, applicationName)
            }
            activity.startActivity(intent)
        }
    }

    companion object {
        private const val IS_UPDATED_KEY = "isUpdated"
        private const val FORCE_UPDATE_NAME = "Force Update"
    }
}