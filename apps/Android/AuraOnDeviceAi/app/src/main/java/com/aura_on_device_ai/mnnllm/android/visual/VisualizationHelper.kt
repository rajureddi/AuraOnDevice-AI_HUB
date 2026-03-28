package com.aura_on_device_ai.mnnllm.android.visual

import android.graphics.Bitmap
import android.graphics.Color
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage

object VisualizationHelper {

    /**
     * Converts a MediaPipe MPImage mask (Category or Confidence) into a Drawable Bitmap.
     * Uses a semi-transparent Cyan color for identified object regions.
     */
    fun createBitmapFromMask(mask: MPImage): Bitmap {
        val width = mask.width
        val height = mask.height
        
        // Extract the raw byte buffer from the MPImage
        val byteBuffer = ByteBufferExtractor.extract(mask)
        byteBuffer.rewind()
        
        val colors = IntArray(width * height)
        
        // Populate color array based on mask values
        // If maskValue > 0, it means it's part of the segmented object
        for (i in 0 until width * height) {
            val maskValue = byteBuffer.get().toInt() and 0xFF
            if (maskValue > 0) {
                // Semi-transparent Cyan (Alpha: 128, R: 0, G: 255, B: 255)
                colors[i] = Color.argb(128, 0, 255, 255)
            } else {
                colors[i] = Color.TRANSPARENT
            }
        }
        
        return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
    }
}

