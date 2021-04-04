package com.android.forceupdate.manager

import android.app.Activity
import android.content.Intent
import com.android.forceupdate.ui.ForceUpdateActivity
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_APK_LINK
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_APPLICATION_NAME
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_LOGO_IMAGE
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_MESSAGE
import com.android.forceupdate.ui.ForceUpdateActivity.Companion.EXTRA_TITLE
import javax.inject.Singleton

@Singleton
class ForceUpdateManager(
    private val activity: Activity,
    private val apkLink: String,
    private val title: String? = null,
    private val message: String? = null,
    private val logo: Int? = null,
    private val applicationName: String? = null
) {

    fun isApplicationUpdated() {
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