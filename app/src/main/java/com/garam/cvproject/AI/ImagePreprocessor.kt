package com.garam.cvproject.AI

import android.graphics.Bitmap

object ImagePreprocessor {
    fun preprocessImage(bitmap: Bitmap): FloatArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true) // 크기 조정
        val floatArray = FloatArray(224 * 224 * 3) // 3채널 (RGB)
        var index = 0

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedBitmap.getPixel(x, y)
                floatArray[index++] = (pixel shr 16 and 0xFF) / 255.0f // R 채널
                floatArray[index++] = (pixel shr 8 and 0xFF) / 255.0f  // G 채널
                floatArray[index++] = (pixel and 0xFF) / 255.0f        // B 채널
            }
        }

        return floatArray
    }
}