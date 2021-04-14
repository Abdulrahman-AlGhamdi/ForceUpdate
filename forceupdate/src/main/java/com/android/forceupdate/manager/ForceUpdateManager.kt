package com.android.forceupdate.manager

import android.app.Activity
import android.content.Intent
import com.android.forceupdate.ui.ForceUpdateActivity
import javax.inject.Singleton

@Singleton
class ForceUpdateManager(
    private val activity: Activity,
    private val apkLink: String,
    private val logo: Int? = null,
    private val title: String? = null,
    private val message: String? = null,
    private val versionCode: Int? = null,
    private val versionName: String? = null,
    private val applicationName: String? = null
) {

    fun isApplicationUpdated() {
        val intent = Intent(activity, ForceUpdateActivity::class.java).apply {
            this.putExtra(EXTRA_TITLE, title)
            this.putExtra(EXTRA_LOGO_IMAGE, logo)
            this.putExtra(EXTRA_MESSAGE, message)
            this.putExtra(EXTRA_APK_LINK, apkLink)
            this.putExtra(EXTRA_VERSION_CODE, versionCode)
            this.putExtra(EXTRA_VERSION_NAME, versionName)
            this.putExtra(EXTRA_APPLICATION_NAME, applicationName)
        }
        activity.startActivity(intent)
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_APK_LINK = "link"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_LOGO_IMAGE = "logo"
        const val EXTRA_VERSION_NAME = "version name"
        const val EXTRA_VERSION_CODE = "version code"
        const val EXTRA_APPLICATION_NAME = "application name"
    }
}