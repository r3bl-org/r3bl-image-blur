// Copyright (c) 2025 R3BL LLC. Licensed under Apache License, Version 2.0.

package com.r3bl.imageblur

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters

/**
 * Background worker that processes images: blur + darken + save.
 * Shows progress notifications during processing.
 */
class ImageProcessWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_CACHED_PATH = "cached_path"
        const val KEY_ORIGINAL_NAME = "original_name"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d(Config.LOG_TAG, "=".repeat(60))
        Log.d(Config.LOG_TAG, "ImageProcessWorker.doWork() STARTED")
        Log.d(Config.LOG_TAG, "  workerId=${id}")

        val cachedPath = inputData.getString(KEY_CACHED_PATH)
        val originalName = inputData.getString(KEY_ORIGINAL_NAME) ?: "image"
        Log.d(Config.LOG_TAG, "  cachedPath=$cachedPath")
        Log.d(Config.LOG_TAG, "  originalName=$originalName")

        if (cachedPath == null) {
            Log.e(Config.LOG_TAG, "  FAILURE: cachedPath is null")
            return Result.failure()
        }

        val cachedFile = java.io.File(cachedPath)
        if (!cachedFile.exists()) {
            Log.e(Config.LOG_TAG, "  FAILURE: cached file does not exist")
            return Result.failure()
        }
        Log.d(Config.LOG_TAG, "  Cached file size: ${cachedFile.length()} bytes")

        // Create notification channels
        createNotificationChannels()

        // Start foreground service with initial progress notification
        setForeground(createProgressForegroundInfo("Preparing..."))

        return try {
            // Load bitmap from cached file
            updateProgress("Loading image...")
            Log.d(Config.LOG_TAG, "  Decoding bitmap from cached file...")
            val decodeStartTime = System.currentTimeMillis()
            val originalBitmap = BitmapFactory.decodeFile(cachedPath)
            val decodeTime = System.currentTimeMillis() - decodeStartTime

            if (originalBitmap == null) {
                Log.e(Config.LOG_TAG, "  FAILURE: BitmapFactory.decodeFile() returned null")
                return Result.failure()
            }
            Log.d(Config.LOG_TAG, "  Bitmap decoded in ${decodeTime}ms")
            Log.d(Config.LOG_TAG, "  Original bitmap: ${originalBitmap.width}x${originalBitmap.height}, config=${originalBitmap.config}")

            // Process: blur then darken
            updateProgress("Applying blur...")
            Log.d(Config.LOG_TAG, "  Starting blur...")
            val blurStartTime = System.currentTimeMillis()
            val blurred = ImageProcessor.blur(originalBitmap)
            val blurTime = System.currentTimeMillis() - blurStartTime
            Log.d(Config.LOG_TAG, "  Blur completed in ${blurTime}ms")
            Log.d(Config.LOG_TAG, "  Blurred bitmap: ${blurred.width}x${blurred.height}, config=${blurred.config}")

            updateProgress("Applying darken effect...")
            Log.d(Config.LOG_TAG, "  Starting darken...")
            val darkenStartTime = System.currentTimeMillis()
            val processed = ImageProcessor.darken(blurred)
            val darkenTime = System.currentTimeMillis() - darkenStartTime
            Log.d(Config.LOG_TAG, "  Darken completed in ${darkenTime}ms")
            Log.d(Config.LOG_TAG, "  Processed bitmap: ${processed.width}x${processed.height}, config=${processed.config}")

            // Create output filename
            val (baseName, ext) = createOutputFileName(originalName)
            Log.d(Config.LOG_TAG, "  baseName=$baseName, ext=$ext")

            // Save to Downloads
            updateProgress("Saving to Downloads...")
            Log.d(Config.LOG_TAG, "  Saving to Downloads...")
            val saveStartTime = System.currentTimeMillis()
            val saveResult = saveToDownloads(processed, baseName, ext)
            val saveTime = System.currentTimeMillis() - saveStartTime
            Log.d(Config.LOG_TAG, "  Save completed in ${saveTime}ms")

            val outputUri = saveResult?.first
            val outputName = saveResult?.second ?: "$baseName$ext"
            Log.d(Config.LOG_TAG, "  outputUri=$outputUri, outputName=$outputName")

            // Clean up cached file
            Log.d(Config.LOG_TAG, "  Cleaning up cached file...")
            cachedFile.delete()
            Log.d(Config.LOG_TAG, "  Cached file deleted")

            val totalTime = System.currentTimeMillis() - startTime
            Log.d(Config.LOG_TAG, "ImageProcessWorker.doWork() SUCCESS")
            Log.d(Config.LOG_TAG, "  Total time: ${totalTime}ms (decode=${decodeTime}ms, blur=${blurTime}ms, darken=${darkenTime}ms, save=${saveTime}ms)")
            Log.d(Config.LOG_TAG, "=".repeat(60))

            // Show completion notification
            showCompletionNotification(outputName, outputUri)

            Result.success()
        } catch (e: Exception) {
            Log.e(Config.LOG_TAG, "ImageProcessWorker.doWork() EXCEPTION", e)
            Log.e(Config.LOG_TAG, "  Exception class: ${e.javaClass.name}")
            Log.e(Config.LOG_TAG, "  Exception message: ${e.message}")
            Log.e(Config.LOG_TAG, "  Stack trace:")
            e.stackTrace.take(10).forEach { frame ->
                Log.e(Config.LOG_TAG, "    at $frame")
            }
            showErrorNotification(e.message ?: "Unknown error")
            Log.d(Config.LOG_TAG, "=".repeat(60))
            Result.failure()
        }
    }

    private fun createProgressForegroundInfo(status: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, Config.PROGRESS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Processing image")
            .setContentText(status)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                Config.NOTIFICATION_ID_PROGRESS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
        } else {
            ForegroundInfo(Config.NOTIFICATION_ID_PROGRESS, notification)
        }
    }

    private suspend fun updateProgress(status: String) {
        Log.d(Config.LOG_TAG, "  Progress: $status")
        setForeground(createProgressForegroundInfo(status))
    }

    private fun createOutputFileName(originalName: String): Pair<String, String> {
        val dotIndex = originalName.lastIndexOf('.')
        return if (dotIndex > 0) {
            val baseName = originalName.substring(0, dotIndex) + "_blur"
            val ext = originalName.substring(dotIndex)
            Pair(baseName, ext)
        } else {
            Pair("${originalName}_blur", ".png")
        }
    }

    private fun saveToDownloads(bitmap: android.graphics.Bitmap, baseName: String, ext: String): Pair<Uri, String>? {
        var fileName = "$baseName$ext"
        var counter = 1

        // Find a unique filename
        while (fileExistsInDownloads(fileName)) {
            fileName = "${baseName}_$counter$ext"
            counter++
            Log.d(Config.LOG_TAG, "  File exists, trying: $fileName")
        }

        Log.d(Config.LOG_TAG, "  Using filename: $fileName")

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, getMimeType(fileName))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            val format = if (fileName.endsWith(".png", ignoreCase = true)) {
                android.graphics.Bitmap.CompressFormat.PNG
            } else {
                android.graphics.Bitmap.CompressFormat.JPEG
            }
            bitmap.compress(format, 100, outputStream)
        }

        return Pair(uri, fileName)
    }

    private fun fileExistsInDownloads(fileName: String): Boolean {
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".jpg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/png"
        }
    }

    private fun showCompletionNotification(fileName: String, fileUri: Uri?) {
        Log.d(Config.LOG_TAG, "  Showing completion notification...")
        // Cancel the progress notification
        notificationManager.cancel(Config.NOTIFICATION_ID_PROGRESS)
        Log.d(Config.LOG_TAG, "  Cancelled progress notification")

        val builder = NotificationCompat.Builder(context, Config.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Image processed")
            .setContentText("Saved: $fileName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Add tap action to open the image
        fileUri?.let { uri ->
            Log.d(Config.LOG_TAG, "  Adding tap action for uri=$uri")
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                viewIntent,
                PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
        }

        notificationManager.notify(Config.NOTIFICATION_ID_COMPLETE, builder.build())
        Log.d(Config.LOG_TAG, "  Completion notification posted (id=${Config.NOTIFICATION_ID_COMPLETE})")
    }

    private fun showErrorNotification(message: String) {
        // Cancel the progress notification
        notificationManager.cancel(Config.NOTIFICATION_ID_PROGRESS)

        val notification = NotificationCompat.Builder(context, Config.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Processing failed")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Config.NOTIFICATION_ID_COMPLETE, notification)
    }

    private fun createNotificationChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        // Progress channel (low importance - no sound)
        val progressChannel = NotificationChannel(
            Config.PROGRESS_CHANNEL_ID,
            "Processing Progress",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress while processing images"
        }
        manager.createNotificationChannel(progressChannel)

        // Completion channel (default importance)
        val completionChannel = NotificationChannel(
            Config.NOTIFICATION_CHANNEL_ID,
            "Processing Complete",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when image processing completes"
        }
        manager.createNotificationChannel(completionChannel)
    }
}
