// Copyright (c) 2025 R3BL LLC. Licensed under the MIT License.

package com.r3bl.imageblur

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File

/**
 * Transparent activity that receives shared images.
 * Uses a dialog theme to allow waiting for permission results.
 */
class ShareActivity : Activity() {

    private var pendingImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(Config.LOG_TAG, "ShareActivity.onCreate() started")
        Log.d(Config.LOG_TAG, "  intent.action=${intent?.action}")
        Log.d(Config.LOG_TAG, "  intent.type=${intent?.type}")

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            Log.d(Config.LOG_TAG, "  imageUri=$imageUri")
            if (imageUri != null) {
                pendingImageUri = imageUri

                // Check if we need to request permission
                if (Permissions.requestNotificationPermissionIfNeeded(this)) {
                    // Permission already granted, process immediately
                    processAndFinish(imageUri)
                }
                // Otherwise wait for onRequestPermissionsResult
            } else {
                Log.w(Config.LOG_TAG, "  imageUri is null, cannot process")
                finish()
            }
        } else {
            Log.w(Config.LOG_TAG, "  Intent action/type mismatch, not processing")
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(Config.LOG_TAG, "onRequestPermissionsResult: requestCode=$requestCode")

        if (requestCode == Permissions.REQUEST_CODE_NOTIFICATIONS) {
            // Process regardless of grant result (image still gets processed)
            pendingImageUri?.let { processAndFinish(it) } ?: finish()
        }
    }

    private fun processAndFinish(imageUri: Uri) {
        enqueueProcessing(imageUri)
        Log.d(Config.LOG_TAG, "ShareActivity finishing")
        finish()
    }

    private fun enqueueProcessing(imageUri: Uri) {
        Log.d(Config.LOG_TAG, "enqueueProcessing() started")
        Log.d(Config.LOG_TAG, "  imageUri=$imageUri")

        // Copy image to cache while we still have URI permission
        val cachedFile = copyToCache(imageUri)
        if (cachedFile == null) {
            Log.e(Config.LOG_TAG, "  Failed to copy image to cache")
            return
        }
        Log.d(Config.LOG_TAG, "  cachedFile=${cachedFile.absolutePath}")

        // Get original filename for output naming
        val originalName = getFileName(imageUri)
        Log.d(Config.LOG_TAG, "  originalName=$originalName")

        val inputData = Data.Builder()
            .putString(ImageProcessWorker.KEY_CACHED_PATH, cachedFile.absolutePath)
            .putString(ImageProcessWorker.KEY_ORIGINAL_NAME, originalName)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ImageProcessWorker>()
            .setInputData(inputData)
            .build()

        Log.d(Config.LOG_TAG, "  workRequest.id=${workRequest.id}")
        WorkManager.getInstance(this).enqueue(workRequest)
        Log.d(Config.LOG_TAG, "enqueueProcessing() work enqueued successfully")
    }

    private fun copyToCache(uri: Uri): File? {
        return try {
            Log.d(Config.LOG_TAG, "  Copying URI to cache...")
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val cacheFile = File(cacheDir, "input_${System.currentTimeMillis()}.tmp")
            cacheFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            Log.d(Config.LOG_TAG, "  Copied ${cacheFile.length()} bytes to cache")
            cacheFile
        } catch (e: Exception) {
            Log.e(Config.LOG_TAG, "  Failed to copy to cache: ${e.message}", e)
            null
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "image"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            Log.w(Config.LOG_TAG, "  Could not get filename: ${e.message}")
        }
        Log.d(Config.LOG_TAG, "  getFileName() returning: $name")
        return name
    }
}
