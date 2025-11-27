// Copyright (c) 2025 R3BL LLC. Licensed under Apache License, Version 2.0.

package com.r3bl.imageblur

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Handles runtime permission requests for the app.
 */
object Permissions {

    /**
     * Request notification permission if needed (Android 13+).
     * Returns true if permission is already granted, false if request was sent.
     */
    fun requestNotificationPermissionIfNeeded(activity: Activity): Boolean {
        // Only needed on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(Config.LOG_TAG, "Permissions: Notification permission not needed (API < 33)")
            return true
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS
        val granted = ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED

        if (granted) {
            Log.d(Config.LOG_TAG, "Permissions: Notification permission already granted")
            return true
        }

        Log.d(Config.LOG_TAG, "Permissions: Requesting notification permission")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            REQUEST_CODE_NOTIFICATIONS
        )
        return false
    }

    const val REQUEST_CODE_NOTIFICATIONS = 1001
}
