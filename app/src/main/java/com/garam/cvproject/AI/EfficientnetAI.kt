package com.garam.cvproject.AI

import android.content.Context
import org.pytorch.IValue
//import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File


class EfficientnetAI(context: Context) {
    private val module: Module

    init {
        // assets 폴더에서 모델 로드
        val modelPath = assetFilePath(context, "efficientnet_b0_ai_gen.pt")
        module = Module.load(modelPath)
    }

    /**
     * 입력 데이터를 받아 PyTorch 모델 추론 실행
     * @param inputData 모델에 전달할 입력 데이터 (FloatArray)
     * @return 모델의 추론 결과 (FloatArray)
     */
    fun predict(inputData: FloatArray): FloatArray {
        // 입력 데이터를 Tensor로 변환
        val inputTensor = Tensor.fromBlob(inputData, longArrayOf(1, 3, 224, 224)) // 예: 224x224 RGB 이미지
        // 모델 추론 실행
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()
        // 결과 반환
        return outputTensor.dataAsFloatArray
    }

    /**
     * assets 디렉토리의 파일 경로 반환
     * @param context 애플리케이션 컨텍스트
     * @param assetName 파일 이름 (예: model.pt)
     * @return 파일의 절대 경로
     */
    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (!file.exists()) {
            context.assets.open(assetName).use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file.absolutePath
    }
}
