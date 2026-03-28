package com.aura_on_device_ai.mnnllm.android.visual

/**
 * Standard MediaPipe connection pairs for skeletal rendering.
 * These are used as a fallback if the library constants are unresolved.
 */
object MediaPipeConstants {
    
    // Hand Landmark Connections (21 points)
    val HAND_CONNECTIONS = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,    // Thumb
        0 to 5, 5 to 6, 6 to 7, 7 to 8,    // Index
        0 to 9, 9 to 10, 10 to 11, 11 to 12, // Middle
        0 to 13, 13 to 14, 14 to 15, 15 to 16, // Ring
        0 to 17, 17 to 18, 18 to 19, 19 to 20, // Pinky
        0 to 17, 5 to 9, 9 to 13, 13 to 17 // Palm base
    )

    // Pose Landmark Connections (33 points)
    val POSE_CONNECTIONS = listOf(
        11 to 12, // Shoulders
        11 to 13, 13 to 15, // Left arm
        12 to 14, 14 to 16, // Right arm
        11 to 23, 12 to 24, // Torso
        23 to 24, // Hips
        23 to 25, 25 to 27, 27 to 29, 29 to 31, // Left leg
        24 to 26, 26 to 28, 28 to 30, 30 to 32, // Right leg
        27 to 31, 28 to 32 // Feet
    )

    // Face Outlines (Selective contours for a professional look)
    val FACE_CONTOURS = listOf(
        // Face Oval
        10 to 338, 338 to 297, 297 to 332, 332 to 284, 284 to 251, 251 to 389, 389 to 356, 356 to 454,
        454 to 323, 323 to 361, 361 to 288, 288 to 397, 397 to 365, 365 to 379, 379 to 378, 378 to 400,
        400 to 377, 377 to 152, 152 to 148, 148 to 176, 176 to 149, 149 to 150, 150 to 136, 136 to 172,
        172 to 58, 58 to 132, 132 to 93, 93 to 234, 234 to 127, 127 to 162, 162 to 21, 21 to 54,
        54 to 103, 103 to 67, 67 to 109, 109 to 10
    )
}

