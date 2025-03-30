package com.garam.cvproject

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * 모델 최적화 및 캐싱을 위한 유틸리티 클래스
 */
class ModelOptimizer(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelOptimizer"
        private const val CACHE_DIR_NAME = "model_cache"
        
        // 싱글톤 인스턴스
        @Volatile
        private var INSTANCE: ModelOptimizer? = null
        
        fun getInstance(context: Context): ModelOptimizer {
            return INSTANCE ?: synchronized(this) {
                val instance = ModelOptimizer(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    // 세션 캐시
    private val sessionCache = ConcurrentHashMap<String, OrtSession>()
    
    // 캐시 디렉토리
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Assets에서 모델을 최적화된 형태로 로드
     * 캐싱 기능 적용
     */
    @WorkerThread
    suspend fun loadModelFromAssets(assetName: String): OrtSession = withContext(Dispatchers.IO) {
        // 캐시에 있는지 확인
        sessionCache[assetName]?.let { return@withContext it }
        
        try {
            val cachedModelFile = File(cacheDir, assetName)
            val env = OrtEnvironment.getEnvironment()
            
            // 캐시 파일 확인
            val session = if (cachedModelFile.exists()) {
                // 캐시에서 로드
                Log.d(TAG, "Loading model from cache: $assetName")
                val modelBytes = cachedModelFile.readBytes()
                env.createSession(modelBytes)
            } else {
                // Assets에서 로드 후 캐시 저장
                Log.d(TAG, "Loading model from assets: $assetName")
                val modelBytes = context.assets.open(assetName).readBytes()
                
                // 캐시에 저장
                try {
                    FileOutputStream(cachedModelFile).use { it.write(modelBytes) }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cache model: $assetName", e)
                }
                
                env.createSession(modelBytes)
            }
            
            // 세션 캐시에 저장
            sessionCache[assetName] = session
            session
        } catch (e: Exception) {
            Log.e(TAG, "Error loading model: $assetName", e)
            throw e
        }
    }
    
    /**
     * 비트맵 리사이징 및 전처리 최적화
     * 메모리 재사용
     */
    fun optimizedBitmapResize(bitmap: Bitmap, targetSize: Int): Bitmap {
        if (bitmap.width == targetSize && bitmap.height == targetSize) {
            return bitmap
        }
        
        // 비트맵 리사이징 (가능한 경우 메모리 재사용)
        return Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
    }
    
    /**
     * 메모리 효율적인 정규화 처리
     */
    fun normalizeForInference(
        bitmap: Bitmap,
        mean: FloatArray = floatArrayOf(0.485f, 0.456f, 0.406f),
        std: FloatArray = floatArrayOf(0.229f, 0.224f, 0.225f)
    ): ByteBuffer {
        val height = bitmap.height
        val width = bitmap.width
        val buffer = ByteBuffer.allocateDirect(4 * 3 * height * width)
        
        val pixels = IntArray(height * width)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        var pixel = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = pixels[pixel++]
                
                // 정규화
                buffer.putFloat(((value shr 16 and 0xFF) / 255f - mean[0]) / std[0])
                buffer.putFloat(((value shr 8 and 0xFF) / 255f - mean[1]) / std[1])
                buffer.putFloat(((value and 0xFF) / 255f - mean[2]) / std[2])
            }
        }
        
        buffer.rewind()
        return buffer
    }
    
    /**
     * 세션 캐시 정리
     */
    fun clearSessionCache() {
        sessionCache.values.forEach { session ->
            try {
                session.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing session", e)
            }
        }
        sessionCache.clear()
    }
    
    /**
     * 디스크 캐시 정리
     */
    suspend fun clearDiskCache() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "Disk cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing disk cache", e)
        }
    }
} 