// Copyright (c) 2025 R3BL LLC. Licensed under Apache License, Version 2.0.

package com.r3bl.imageblur

/**
 * Tunable constants for the blur effect.
 * Adjust these values to change the visual output.
 */
object Config {
    /** Log tag for all debug output */
    const val LOG_TAG = "ImageBlur"

    /** Blur radius in pixels (0-25). Higher = more blur. */
    const val BLUR_RADIUS = 20f

    /** Scale factor before blur (0.1-1.0). Lower = faster + more blur effect. */
    const val SCALE_FACTOR = 0.2f

    /** Darken overlay alpha (0.0-1.0). Higher = darker. */
    const val DARKEN_ALPHA = 0.18f

    /** Notification channel ID for completion notifications */
    const val NOTIFICATION_CHANNEL_ID = "image_processing"

    /** Notification channel ID for progress notifications */
    const val PROGRESS_CHANNEL_ID = "image_processing_progress"

    /** Notification IDs */
    const val NOTIFICATION_ID_PROGRESS = 1
    const val NOTIFICATION_ID_COMPLETE = 2
}
