package com.garam.cvproject

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import kotlin.math.exp

/**
 * 딥페이크 탐지를 위한 클래스
 * - YOLO 모델로 얼굴 검출
 * - 이진 분류 모델로 딥페이크 여부 분석
 */
class DeepfakeDetector(
    private val yoloSession: OrtSession,  // yolov11n-face.onnx
    private val clsSession: OrtSession    // deepfake_binary_s128_e5_early.onnx
) {

    companion object {
        // 상수 정의
        private const val YOLO_INPUT_SIZE = 640
        private const val CLS_INPUT_SIZE = 128
        private const val TAG = "DeepfakeDetector"
        
        // 얼굴 영역 확장 비율 기본값
        private const val DEFAULT_EXTEND_RATIO = 0.5f
        
        // 임계값 기본값
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.8f
    }

    /**
     * 얼굴 바운딩 박스 데이터 클래스
     */
    data class FaceBox(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val score: Float,
        val cls: Float
    )

    /**
     * 탐지 결과 데이터 클래스
     */
    data class DetectionResult(
        val faceIndex: Int,
        val label: String,
        val confidence: Float,
        val message: String,
        val conf: String,
        val croppedBitmap: Bitmap?
    )

    /**
     * 비동기 이미지 분석 함수
     * 코루틴을 활용한 딥페이크 탐지 및 분류
     */
    suspend fun detectAndClassifyAsync(
        bitmap: Bitmap,
        confThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        extendRatio: Float = DEFAULT_EXTEND_RATIO
    ): DetectionResult? = withContext(Dispatchers.Default) {
        try {
            // 얼굴 검출
            val faceBoxes = detectFaces(bitmap, confThreshold)

            // 얼굴이 하나도 없으면 -> 전체 이미지를 분류
            if (faceBoxes.isEmpty()) {
                val (cls, score) = classifyBitmap(bitmap)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "No Face Detected → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)

                return@withContext DetectionResult(
                    faceIndex = -1,
                    label = labelStr,
                    confidence = score,
                    message = msg,
                    conf = conf,
                    croppedBitmap = bitmap
                )
            }

            // 얼굴이 있다면 각 얼굴 크롭 -> 분류
            val results = faceBoxes.mapIndexed { index, box ->
                val cropped = cropFaceArea(bitmap, box, extendRatio)
                val (cls, score) = classifyBitmap(cropped)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "${index + 1}번 얼굴 → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)

                DetectionResult(
                    faceIndex = index,
                    label = labelStr,
                    confidence = score,
                    message = msg,
                    conf = conf,
                    croppedBitmap = cropped
                )
            }

            // 가장 높은 신뢰도의 결과 선택
            return@withContext results.maxByOrNull { it.confidence }
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectAndClassifyAsync", e)
            null
        }
    }

    /**
     * 동기식 분석 함수 (기존 호환성 유지)
     */
    fun detectAndClassify(
        bitmap: Bitmap,
        confThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        extendRatio: Float = DEFAULT_EXTEND_RATIO
    ): DetectionResult? {
        return try {
            // 얼굴 검출
            val faceBoxes = detectFaces(bitmap, confThreshold)

            // 얼굴이 하나도 없으면 -> 전체 이미지를 분류
            if (faceBoxes.isEmpty()) {
                val (cls, score) = classifyBitmap(bitmap)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "No Face Detected → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)

                return DetectionResult(
                    faceIndex = -1,
                    label = labelStr,
                    confidence = score,
                    message = msg,
                    conf = conf,
                    croppedBitmap = bitmap
                )
            }

            // 얼굴이 있다면 각 얼굴 크롭 -> 분류
            val results = faceBoxes.mapIndexed { index, box ->
                val cropped = cropFaceArea(bitmap, box, extendRatio)
                val (cls, score) = classifyBitmap(cropped)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "${index + 1}번 얼굴 → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)

                DetectionResult(
                    faceIndex = index,
                    label = labelStr,
                    confidence = score,
                    message = msg,
                    conf = conf,
                    croppedBitmap = cropped
                )
            }

            // 가장 높은 신뢰도의 결과 선택
            results.maxByOrNull { it.confidence }
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectAndClassify", e)
            null
        }
    }

    /**
     * YOLO 전처리: 640×640 리사이즈, 0~1 스케일링(RGB)
     * 메모리 최적화 및 효율적인 버퍼 관리 적용
     */
    private fun preprocessForYolo(bitmap: Bitmap, targetSize: Int = YOLO_INPUT_SIZE): OnnxTensor {
        val resized = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        val floatBuffer = FloatBuffer.allocate(1 * 3 * targetSize * targetSize)

        val pixels = IntArray(targetSize * targetSize)
        resized.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        // 효율적인 픽셀 처리
        for (i in pixels.indices) {
            val px = pixels[i]
            floatBuffer.put((px shr 16 and 0xFF) / 255f) // R
            floatBuffer.put((px shr 8 and 0xFF) / 255f)  // G
            floatBuffer.put((px and 0xFF) / 255f)        // B
        }
        floatBuffer.rewind()

        val env = OrtEnvironment.getEnvironment()
        return OnnxTensor.createTensor(
            env,
            floatBuffer,
            longArrayOf(1, 3, targetSize.toLong(), targetSize.toLong())
        )
    }

    /**
     * YOLO로 얼굴 검출.
     * 메모리 사용량 최적화 및 예외 처리 강화
     */
    private fun detectFaces(
        bitmap: Bitmap,
        confThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD
    ): List<FaceBox> {
        return try {
            // YOLO 입력 텐서 생성
            preprocessForYolo(bitmap).use { inputTensor ->
                // 세션 실행
                val output = yoloSession.run(mapOf("images" to inputTensor))
                val rawOutput = output[0].value as Array<Array<FloatArray>>

                // 스케일 계산
                val scaleX = bitmap.width / YOLO_INPUT_SIZE.toFloat()
                val scaleY = bitmap.height / YOLO_INPUT_SIZE.toFloat()

                // 결과 처리
                val boxes = mutableListOf<FaceBox>()
                for (detection in rawOutput[0]) {
                    val score = detection[4]
                    if (score >= confThreshold) {
                        boxes.add(
                            FaceBox(
                                x1 = detection[0] * scaleX,
                                y1 = detection[1] * scaleY,
                                x2 = detection[2] * scaleX,
                                y2 = detection[3] * scaleY,
                                score = score,
                                cls = detection[5]
                            )
                        )
                    }
                }
                boxes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Face detection error", e)
            emptyList()
        }
    }

    /**
     * 얼굴 영역을 상하좌우로 확장하여 Crop
     * 안전한 경계 검사 추가
     */
    private fun cropFaceArea(
        src: Bitmap,
        box: FaceBox,
        extendRatio: Float = DEFAULT_EXTEND_RATIO
    ): Bitmap {
        val imgW = src.width
        val imgH = src.height

        val faceW = box.x2 - box.x1
        val faceH = box.y2 - box.y1

        // 상하 확장
        val newY1 = (box.y1 - faceH * extendRatio * 0.7f).coerceAtLeast(0f)
        val newY2 = (box.y2 + faceH * extendRatio).coerceAtMost(imgH.toFloat())

        // 좌우 확장
        val extendW = faceW * extendRatio * 0.7f
        val newX1 = (box.x1 - extendW).coerceAtLeast(0f)
        val newX2 = (box.x2 + extendW).coerceAtMost(imgW.toFloat())

        val left = newX1.toInt()
        val top = newY1.toInt()
        val width = (newX2 - newX1).toInt()
        val height = (newY2 - newY1).toInt()

        return if (width > 0 && height > 0) {
            try {
                Bitmap.createBitmap(src, left, top, width, height)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating cropped bitmap", e)
                src // 에러 발생 시 원본 반환
            }
        } else {
            src // 유효하지 않은 크기일 경우 원본 반환
        }
    }

    /**
     * 분류 모델 전처리 & 추론
     * 메모리 사용 최적화 및 성능 개선
     */
    private fun classifyBitmap(bitmap: Bitmap): Pair<Int, Float> {
        // 리사이즈
        val resized = Bitmap.createScaledBitmap(bitmap, CLS_INPUT_SIZE, CLS_INPUT_SIZE, true)
        val floatBuffer = FloatBuffer.allocate(1 * 3 * CLS_INPUT_SIZE * CLS_INPUT_SIZE)

        // 정규화 파라미터
        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        // 픽셀 배열 준비
        val pixels = IntArray(CLS_INPUT_SIZE * CLS_INPUT_SIZE)
        resized.getPixels(pixels, 0, CLS_INPUT_SIZE, 0, 0, CLS_INPUT_SIZE, CLS_INPUT_SIZE)

        // 효율적인 정규화 처리
        for (px in pixels) {
            val r = (px shr 16 and 0xFF) / 255f
            val g = (px shr 8 and 0xFF) / 255f
            val b = (px and 0xFF) / 255f

            floatBuffer.put((r - mean[0]) / std[0])
            floatBuffer.put((g - mean[1]) / std[1])
            floatBuffer.put((b - mean[2]) / std[2])
        }
        floatBuffer.rewind()

        // ONNX 텐서 생성 및 추론
        return OrtEnvironment.getEnvironment().use { env ->
            OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, CLS_INPUT_SIZE.toLong(), CLS_INPUT_SIZE.toLong())).use { inputTensor ->
                val output = clsSession.run(mapOf("input" to inputTensor))
                val logits = output[0].value as Array<FloatArray>
                val logit = logits[0]

                // 소프트맥스 계산
                val expValues = logit.map { exp(it.toDouble()).toFloat() }
                val sumExp = expValues.sum()
                val probabilities = expValues.map { it / sumExp }

                // 0:Fake, 1:Real 클래스 중 더 높은 확률 선택
                if (probabilities[0] >= probabilities[1]) {
                    0 to probabilities[0]
                } else {
                    1 to probabilities[1]
                }
            }
        }
    }

    /**
     * 모든 얼굴 검출 및 분류하여 모든 결과 반환
     * 다중 얼굴 처리에 최적화
     */
    suspend fun detectAndClassifyAllFaces(
        bitmap: Bitmap,
        confThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        extendRatio: Float = DEFAULT_EXTEND_RATIO
    ): List<DetectionResult> = withContext(Dispatchers.Default) {
        try {
            // 얼굴 검출
            val faceBoxes = detectFaces(bitmap, confThreshold)
            
            // 얼굴이 없는 경우 - 전체 이미지 분석
            if (faceBoxes.isEmpty()) {
                val (cls, score) = classifyBitmap(bitmap)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "얼굴 없음 → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)
                
                return@withContext listOf(
                    DetectionResult(
                        faceIndex = -1,
                        label = labelStr,
                        confidence = score,
                        message = msg,
                        conf = conf,
                        croppedBitmap = bitmap
                    )
                )
            }
            
            // 병렬 처리를 위한 코루틴 사용
            faceBoxes.mapIndexed { index, box ->
                // 각 얼굴 영역 추출
                val cropped = cropFaceArea(bitmap, box, extendRatio)
                
                // 분류 실행
                val (cls, score) = classifyBitmap(cropped)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "${index + 1}번 얼굴 → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)
                
                // 결과 생성
                DetectionResult(
                    faceIndex = index,
                    label = labelStr,
                    confidence = score,
                    message = msg,
                    conf = conf,
                    croppedBitmap = cropped
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectAndClassifyAllFaces", e)
            emptyList()
        }
    }
    
    /**
     * 모든 얼굴 검출 및 분류 - 동기식 버전
     * 기존 코드와의 호환성 유지
     */
    fun detectAndClassifyAllFacesSync(
        bitmap: Bitmap,
        confThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
        extendRatio: Float = DEFAULT_EXTEND_RATIO
    ): List<DetectionResult> {
        return try {
            // 얼굴 검출
            val faceBoxes = detectFaces(bitmap, confThreshold)
            
            // 얼굴이 없는 경우 - 전체 이미지 분석
            if (faceBoxes.isEmpty()) {
                val (cls, score) = classifyBitmap(bitmap)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "얼굴 없음 → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)
                
                return listOf(
                    DetectionResult(
                        faceIndex = -1,
                        label = labelStr,
                        confidence = score,
                        message = msg,
                        conf = conf,
                        croppedBitmap = bitmap
                    )
                )
            }
            
            // 각 얼굴 영역 추출 및 분류
            faceBoxes.mapIndexed { index, box ->
                val cropped = cropFaceArea(bitmap, box, extendRatio)
                val (cls, score) = classifyBitmap(cropped)
                val labelStr = if (cls == 0) "Fake" else "Real"
                val msg = "${index + 1}번 얼굴 → $labelStr (%.2f%%)".format(score * 100)
                val conf = "신뢰도 : (%.2f%%)".format(score * 100)
                
                DetectionResult(
                    faceIndex = index,
                    label = labelStr,
                    confidence = score,
                    message = msg,
                    conf = conf,
                    croppedBitmap = cropped
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectAndClassifyAllFacesSync", e)
            emptyList()
        }
    }
}
