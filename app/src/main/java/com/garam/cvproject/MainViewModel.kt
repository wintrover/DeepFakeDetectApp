package com.garam.cvproject

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class MainViewModel(private val detector: DeepfakeDetector) : ViewModel() {

    // 다중 얼굴 결과를 위한 데이터 클래스
    data class FaceResult(
        val faceIndex: Int,
        val label: String,
        val confidence: Float,
        val message: String,
        val croppedBitmap: Bitmap?
    )

    // UI 상태를 나타내는 데이터 클래스
    data class UiState(
        val imageUri: Uri? = null,
        val imageUrl: String = "",
        val resultText: String = "",
        val resultLabel: String = "",
        val resultConf: String = "",
        val isLoading: Boolean = false,
        val croppedFaceBitmap: Bitmap? = null,
        val errorMessage: String? = null,
        val isFirstRun: Boolean = true,
        
        // 다중 얼굴 결과
        val allFaceResults: List<FaceResult> = emptyList(),
        val showAllFaces: Boolean = false,
        val selectedFaceIndex: Int = -1
    )

    // 상태 관리를 위한 StateFlow
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 이미지 URI 설정
    fun setImageUri(uri: Uri?) {
        _uiState.update { 
            it.copy(
                imageUri = uri, 
                imageUrl = "", 
                resultText = "", 
                resultLabel = "", 
                resultConf = "",
                croppedFaceBitmap = null,
                errorMessage = null,
                allFaceResults = emptyList(),
                showAllFaces = false,
                selectedFaceIndex = -1
            ) 
        }
    }

    // 이미지 URL 설정
    fun setImageUrl(url: String) {
        _uiState.update { 
            it.copy(
                imageUrl = url, 
                imageUri = null, 
                resultText = "", 
                resultLabel = "", 
                resultConf = "",
                croppedFaceBitmap = null,
                errorMessage = null,
                allFaceResults = emptyList(),
                showAllFaces = false,
                selectedFaceIndex = -1
            ) 
        }
    }

    // 첫 실행 상태 업데이트
    fun setFirstRun(isFirst: Boolean) {
        _uiState.update { it.copy(isFirstRun = isFirst) }
    }

    // 모든 얼굴 결과 표시 여부 설정
    fun setShowAllFaces(show: Boolean) {
        _uiState.update { it.copy(showAllFaces = show) }
    }

    // 선택된 얼굴 인덱스 설정
    fun selectFace(index: Int) {
        _uiState.update { currentState ->
            val selectedResult = currentState.allFaceResults.find { it.faceIndex == index }
            
            if (selectedResult != null) {
                currentState.copy(
                    selectedFaceIndex = index,
                    resultText = selectedResult.message,
                    resultLabel = selectedResult.label,
                    resultConf = "신뢰도 : (%.2f%%)".format(selectedResult.confidence * 100),
                    croppedFaceBitmap = selectedResult.croppedBitmap
                )
            } else {
                currentState
            }
        }
    }

    // 이미지 분석 함수
    fun analyzeImage(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // 비동기 모델 실행 - 다중 얼굴 결과 반환
                val allResults = withContext(Dispatchers.Default) {
                    detector.detectAndClassifyAllFaces(bitmap)
                }
                
                if (allResults.isNotEmpty()) {
                    // 가장 높은 신뢰도의 얼굴 결과
                    val bestResult = allResults.maxByOrNull { it.confidence }
                    
                    // 모든 얼굴 결과를 FaceResult 형태로 변환
                    val faceResults = allResults.map { result ->
                        FaceResult(
                            faceIndex = result.faceIndex,
                            label = result.label,
                            confidence = result.confidence,
                            message = result.message,
                            croppedBitmap = result.croppedBitmap
                        )
                    }
                    
                    if (bestResult != null) {
                        _uiState.update { 
                            it.copy(
                                resultText = bestResult.message,
                                resultLabel = bestResult.label,
                                resultConf = bestResult.conf,
                                croppedFaceBitmap = bestResult.croppedBitmap,
                                allFaceResults = faceResults,
                                selectedFaceIndex = bestResult.faceIndex,
                                isLoading = false
                            ) 
                        }
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            errorMessage = "얼굴을 감지할 수 없습니다.",
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error analyzing image", e)
                _uiState.update { 
                    it.copy(
                        errorMessage = "이미지 분석 중 오류가 발생했습니다: ${e.message}",
                        isLoading = false
                    ) 
                }
            }
        }
    }
    
    // URL로부터 비트맵 로드
    fun loadBitmapFromUrl(url: String, onComplete: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        val connection = URL(url).openConnection()
                        connection.connectTimeout = 10000
                        connection.readTimeout = 10000
                        connection.connect()
                        BitmapFactory.decodeStream(connection.getInputStream())
                    } catch (e: IOException) {
                        Log.e("MainViewModel", "Error loading image from URL", e)
                        null
                    }
                }
                _uiState.update { it.copy(isLoading = false) }
                onComplete(bitmap)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in loadBitmapFromUrl", e)
                _uiState.update { 
                    it.copy(
                        errorMessage = "URL에서 이미지를 불러오는 중 오류가 발생했습니다: ${e.message}",
                        isLoading = false
                    ) 
                }
                onComplete(null)
            }
        }
    }
    
    // 인증 마크 추가 및 갤러리 저장
    fun addAuthMarkAndSave(context: Context, bitmap: Bitmap, authMarkBitmap: Bitmap): Boolean {
        return try {
            // 원본 크기의 비트맵 생성
            val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            
            // 캔버스에 그리기
            val canvas = Canvas(resultBitmap)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            
            // 인증 마크 우측 하단 배치
            val markLeft = bitmap.width - authMarkBitmap.width * 0.8f
            val markTop = bitmap.height - authMarkBitmap.height * 0.8f
            canvas.drawBitmap(authMarkBitmap, markLeft, markTop, null)
            
            // 미디어 스토어에 저장
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "Blue_Check_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                }
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error adding auth mark and saving", e)
            false
        }
    }
    
    // 모든 얼굴 분석 결과 가져오기
    fun getAllFaceResults(): List<FaceResult> {
        return uiState.value.allFaceResults
    }
    
    // 얼굴 결과 개수 가져오기
    fun getFaceCount(): Int {
        return uiState.value.allFaceResults.size
    }
    
    // ViewModel Factory
    class Factory(private val detector: DeepfakeDetector) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(detector) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 