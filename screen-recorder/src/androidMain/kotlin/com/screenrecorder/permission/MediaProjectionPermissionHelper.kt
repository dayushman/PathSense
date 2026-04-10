package com.screenrecorder.permission

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager

internal object MediaProjectionPermissionHelper {

    fun getProjectionManager(context: Context): MediaProjectionManager {
        return context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun createScreenCaptureIntent(context: Context): Intent {
        return getProjectionManager(context).createScreenCaptureIntent()
    }
}
