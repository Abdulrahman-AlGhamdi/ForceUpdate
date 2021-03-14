package com.ss.forceupdate

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.ss.forceupdate.Constants.EXTRA_APK_LINK
import com.ss.forceupdate.Constants.EXTRA_APPLICATION_NAME
import com.ss.forceupdate.Constants.EXTRA_LOGO_IMAGE
import com.ss.forceupdate.Constants.EXTRA_MESSAGE
import com.ss.forceupdate.Constants.EXTRA_TITLE

class ForceUpdateManager(
    private val activity: Activity,
    private val context: Context,
    private val newVersion: String,
    private val apkLink: String,
    private val title: String? = null,
    private val message: String? = null,
    private val logo: Int? = null,
    private val applicationName: String? = null
) {

    fun isApplicationUpdated() {
        val sharedPreferences = context.getSharedPreferences(FORCE_UPDATE_NAME, Context.MODE_PRIVATE)
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val applicationVersion = packageInfo.versionName
        val isUpdated = applicationVersion.toDouble() >= newVersion.toDouble()

        sharedPreferences.edit().apply {
            putBoolean(IS_UPDATED_KEY, isUpdated)
            apply()
        }

        if (!sharedPreferences.getBoolean(IS_UPDATED_KEY, false)) {
            val intent = Intent(context, ForceUpdateActivity::class.java).apply {
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