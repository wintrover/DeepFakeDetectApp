package com.garam.cvproject

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import java.nio.FloatBuffer
import kotlin.math.exp

class DeepfakeDetector(
    private val yoloSession: OrtSession,  // yolov11n-face.onnx
    private val clsSession: OrtSession    // deepfake_binary_s128_e5_early.onnx
) {

    data class FaceBox(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val score: Float,
        val cls: Float
    )

    // 필요시 NMS 함수들 (주석 처리)
    /*
    private fun nonMaxSuppression(boxes: List<FaceBox>, iouThreshold: Float = 0.45f): List<FaceBox> {
        ...
    }
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
     * YOLO 전처리: 640×640 리사이즈, 0~1 스케일링(RGB)
     */
    private fun preprocessForYolo(bitmap: Bitmap, targetSize: Int = 640): OnnxTensor {
        val resized = Bitmap.createScaledBitmap(bitmap, targetSize, targetSize, true)
        val floatBuffer = FloatBuffer.allocate(1 * 3 * targetSize * targetSize)

        val pixels = IntArray(targetSize * targetSize)
        resized.getPixels(pixels, 0, targetSize, 0, 0, targetSize, targetSize)

        for (i in pixels.indices) {
            val px = pixels[i]
            val r = ((px shr 16) and 0xFF) / 255f
            val g = ((px shr 8) and 0xFF) / 255f
            val b = (px and 0xFF) / 255f
            floatBuffer.put(r)
            floatBuffer.put(g)
            floatBuffer.put(b)
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
     * (출력: [1][N][6] => (x1, y1, x2, y2, score, class))
     *  - NMS는 이미 모델 내부에서 처리되었다고 가정
     *  - detect(...) 결과 좌표는 640×640 기준이므로, 원본 좌표로 환산 필요
     */
    private fun detectFaces(
        bitmap: Bitmap,
        confThreshold: Float = 0.8f
    ): List<FaceBox> {
        // (1) YOLO 입력
        val inputTensor = preprocessForYolo(bitmap, 640)
        val output = yoloSession.run(mapOf("images" to inputTensor))

        // (2) 모델 출력: shape = [1][N][6]
        val rawOutput = output[0].value as Array<Array<FloatArray>>

        // (2-1) 원본 대비 스케일
        //      -> 640x640으로 리사이즈했으므로, 여기에 맞춰 보정
        val scaleX = bitmap.width / 640f
        val scaleY = bitmap.height / 640f

        val boxes = mutableListOf<FaceBox>()
        for (detection in rawOutput[0]) {
            val x1 = detection[0]
            val y1 = detection[1]
            val x2 = detection[2]
            val y2 = detection[3]
            val score = detection[4]
            val cls = detection[5]

            if (score >= confThreshold) {
                // (2-2) 원본 크기에 맞춰 좌표 환산
                val x1Scaled = x1 * scaleX
                val y1Scaled = y1 * scaleY
                val x2Scaled = x2 * scaleX
                val y2Scaled = y2 * scaleY

                boxes.add(
                    FaceBox(
                        x1 = x1Scaled,
                        y1 = y1Scaled,
                        x2 = x2Scaled,
                        y2 = y2Scaled,
                        score = score,
                        cls = cls
                    )
                )
            }
        }

        // 필요 시 추가 NMS
        // val finalBoxes = nonMaxSuppression(boxes, 0.25f)
        return boxes
    }

    /**
     * 얼굴 영역을 상하좌우로 확장하여 Crop
     */
    private fun cropFaceArea(
        src: Bitmap,
        box: FaceBox,
        extendRatio: Float = 0.5f
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
            Bitmap.createBitmap(src, left, top, width, height)
        } else {
            src // 바운딩 박스가 이상할 경우, 원본 리턴
        }
    }

    /**
     * 분류 모델 전처리 & 추론
     *  - 입력: 128×128
     *  - mean/std 정규화
     *  - 출력: [1,2] => [logitsFake, logitsReal]
     */
    private fun classifyBitmap(bitmap: Bitmap): Pair<Int, Float> {
        // (1) Resize & Normalize
        val resized = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
        val floatBuffer = FloatBuffer.allocate(1 * 3 * 128 * 128)

        val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
        val std = floatArrayOf(0.229f, 0.224f, 0.225f)

        val pixels = IntArray(128 * 128)
        resized.getPixels(pixels, 0, 128, 0, 0, 128, 128)

        var index = 0
        for (px in pixels) {
            val r = ((px shr 16) and 0xFF) / 255f
            val g = ((px shr 8) and 0xFF) / 255f
            val b = (px and 0xFF) / 255f

            val rr = (r - mean[0]) / std[0]
            val gg = (g - mean[1]) / std[1]
            val bb = (b - mean[2]) / std[2]

            floatBuffer.put(rr)
            floatBuffer.put(gg)
            floatBuffer.put(bb)
            index++
        }
        floatBuffer.rewind()

        val env = OrtEnvironment.getEnvironment()
        val inputTensor = OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, 128, 128))

        // (2) 추론
        val output = clsSession.run(mapOf("input" to inputTensor))
        val logits = output[0].value as Array<FloatArray>  // shape [1,2]
        val logit = logits[0]  // [FakeLogit, RealLogit]

        // (3) 소프트맥스 -> argmax
        val expFake = exp(logit[0].toDouble()).toFloat()
        val expReal = exp(logit[1].toDouble()).toFloat()
        val sumExp = expFake + expReal
        val probFake = expFake / sumExp
        val probReal = expReal / sumExp

        // 0:Fake, 1:Real
        return if (probFake >= probReal) {
            0 to probFake
        } else {
            1 to probReal
        }
    }

    /**
     * 1) YOLO로 얼굴 검출
     * 2) 얼굴 있으면 각 얼굴 크롭 & 분류 -> 최고 스코어 결과
     * 3) 얼굴 없으면 원본 이미지를 바로 분류
     */
    fun detectAndClassify(
        bitmap: Bitmap,
        confThreshold: Float = 0.8f,
        extendRatio: Float = 0.5f
    ): DetectionResult? {
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
            val msg = labelStr
            val conf = "신뢰도 : %.2f%%".format(score * 100)
            DetectionResult(
                faceIndex = index,
                label = labelStr,
                confidence = score,
                message = msg,
                conf = conf,
                croppedBitmap = cropped

            )
        }

        // 가장 높은 확률(스코어) 결과 반환
        return results.maxByOrNull { it.confidence }
    }
}
