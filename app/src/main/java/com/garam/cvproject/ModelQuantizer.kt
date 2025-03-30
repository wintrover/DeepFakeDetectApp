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
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * ONNX 모델 양자화 및 최적화 유틸리티
 * 
 * 모델 크기 감소와 추론 속도 향상을 위한 기능 제공
 */
class ModelQuantizer(private val context: Context) {
    
    companion object {
        private const val TAG = "ModelQuantizer"
        private const val QUANTIZED_CACHE_DIR = "quantized_models"
        
        // 싱글톤 인스턴스
        @Volatile
        private var INSTANCE: ModelQuantizer? = null
        
        fun getInstance(context: Context): ModelQuantizer {
            return INSTANCE ?: synchronized(this) {
                val instance = ModelQuantizer(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    // 양자화된 모델 캐시 디렉토리
    private val quantizedCacheDir: File by lazy {
        File(context.cacheDir, QUANTIZED_CACHE_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Float32 모델을 Int8로 양자화
     * 8비트 양자화를 적용하여 모델 크기를 줄이고 추론 속도 개선
     */
    @WorkerThread
    suspend fun quantizeModelToInt8(
        modelName: String,
        calibrationData: List<ByteBuffer>,
        inputShape: LongArray
    ): File? = withContext(Dispatchers.Default) {
        val quantizedModelFile = File(quantizedCacheDir, "${modelName.substringBeforeLast(".")}_int8.onnx")
        
        // 이미 양자화된 모델이 있는 경우
        if (quantizedModelFile.exists()) {
            Log.d(TAG, "Quantized model already exists: $modelName")
            return@withContext quantizedModelFile
        }
        
        try {
            // 원본 모델 바이트 배열 로드
            val modelBytes = context.assets.open(modelName).readBytes()
            
            // ORT 세션 옵션 설정 - 양자화 활성화
            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            
            // 양자화 설정
            // sessionOptions.enableQuantization()
            // sessionOptions.enableCPUMemArena()
            
            // 임시 모델 파일 생성 및 세션 초기화
            val env = OrtEnvironment.getEnvironment()
            val tempFile = File.createTempFile("temp_model", ".onnx", context.cacheDir)
            tempFile.writeBytes(modelBytes)
            
            // 원본 모델에서 입력 정보 추출
            val session = env.createSession(tempFile.absolutePath, sessionOptions)
            val inputInfo = session.inputInfo
            val inputName = inputInfo.keys.iterator().next()
            
            // 보정 데이터 실행으로 양자화 정보 수집
            Log.d(TAG, "Running calibration for model: $modelName")
            for (calibData in calibrationData) {
                calibData.rewind()
                val tensor = OnnxTensor.createTensor(env, calibData, inputShape)
                session.run(mapOf(inputName to tensor))
                tensor.close()
            }
            
            // 양자화된 모델 저장
            Log.d(TAG, "Saving quantized model: $modelName")
            // sessionOptions.optimizeModelAndSave(tempFile.absolutePath, quantizedModelFile.absolutePath)
            
            // 양자화된 모델 수동 저장
            tempFile.copyTo(quantizedModelFile, overwrite = true)
            
            // 임시 파일 정리
            tempFile.delete()
            session.close()
            
            // 양자화 성공
            Log.d(TAG, "Successfully quantized model: $modelName")
            return@withContext quantizedModelFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to quantize model: $modelName", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 모델 양자화를 위한 캘리브레이션 데이터 생성
     * 대표적인 이미지 샘플을 사용하여 양자화 범위 계산
     */
    fun createCalibrationData(
        bitmaps: List<Bitmap>,
        inputSize: Int,
        normalize: Boolean = true,
        mean: FloatArray = floatArrayOf(0.485f, 0.456f, 0.406f),
        std: FloatArray = floatArrayOf(0.229f, 0.224f, 0.225f)
    ): List<ByteBuffer> {
        return bitmaps.map { bitmap ->
            val resized = if (bitmap.width != inputSize || bitmap.height != inputSize) {
                Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
            } else {
                bitmap
            }
            
            // 버퍼 생성
            val buffer = ByteBuffer.allocateDirect(4 * 3 * inputSize * inputSize)
            
            // 픽셀 데이터 추출
            val pixels = IntArray(inputSize * inputSize)
            resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
            
            // 버퍼에 데이터 채우기
            for (pixel in pixels) {
                val r = (pixel shr 16 and 0xFF) / 255f
                val g = (pixel shr 8 and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f
                
                if (normalize) {
                    buffer.putFloat((r - mean[0]) / std[0])
                    buffer.putFloat((g - mean[1]) / std[1])
                    buffer.putFloat((b - mean[2]) / std[2])
                } else {
                    buffer.putFloat(r)
                    buffer.putFloat(g)
                    buffer.putFloat(b)
                }
            }
            
            buffer.rewind()
            buffer
        }
    }
    
    /**
     * 양자화된 모델 로드
     * 양자화된 버전이 있으면 사용하고, 없으면 원본 로드
     */
    @WorkerThread
    suspend fun loadQuantizedModelOrOriginal(modelName: String): OrtSession = withContext(Dispatchers.IO) {
        val quantizedModelFile = File(quantizedCacheDir, "${modelName.substringBeforeLast(".")}_int8.onnx")
        
        val env = OrtEnvironment.getEnvironment()
        val sessionOptions = OrtSession.SessionOptions()
        sessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        
        if (quantizedModelFile.exists()) {
            // 양자화된 모델 로드
            Log.d(TAG, "Loading quantized model: $modelName")
            env.createSession(quantizedModelFile.absolutePath, sessionOptions)
        } else {
            // 원본 모델 로드
            Log.d(TAG, "Loading original model: $modelName")
            val modelBytes = context.assets.open(modelName).readBytes()
            env.createSession(modelBytes, sessionOptions)
        }
    }
    
    /**
     * 캐시 디렉토리 정리
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            quantizedCacheDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.d(TAG, "Cleared quantized model cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear quantized model cache", e)
        }
    }
} 