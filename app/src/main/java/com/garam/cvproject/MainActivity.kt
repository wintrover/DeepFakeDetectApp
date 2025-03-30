@file:OptIn(ExperimentalMaterial3Api::class)

package com.garam.cvproject

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCancellationBehavior
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.garam.cvproject.ui.theme.CVProjectTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var detector: DeepfakeDetector
    private lateinit var viewModelFactory: MainViewModel.Factory
    private lateinit var modelOptimizer: ModelOptimizer
    private lateinit var modelQuantizer: ModelQuantizer
    private lateinit var performanceMonitor: PerformanceMonitor
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ModelOptimizer 인스턴스 초기화
        modelOptimizer = ModelOptimizer.getInstance(applicationContext)
        
        // ModelQuantizer 인스턴스 초기화
        modelQuantizer = ModelQuantizer.getInstance(applicationContext)
        
        // PerformanceMonitor 초기화 및 시작
        performanceMonitor = PerformanceMonitor.getInstance(applicationContext)
        performanceMonitor.startMonitoring(lifecycleScope)
        
        // 성능 모니터링 리스너 등록
        performanceMonitor.addPerformanceListener { metrics ->
            if (metrics.usedMemoryMB > 200) {
                Log.w(TAG, "Memory usage is high: ${metrics.usedMemoryMB}MB")
            }
        }
        
        // 모델 로딩 시간 측정 시작
        performanceMonitor.startOperation("model_loading")
        
        // 모델 로딩 작업 (비동기)
        lifecycleScope.launch {
            try {
                // 모델 양자화 및 최적화 시도
                val sessions = loadOptimizedModels()
                
                if (sessions != null) {
                    val (yoloSession, clsSession) = sessions
                    
                    // Detector 생성
                    detector = DeepfakeDetector(yoloSession, clsSession)
                    
                    // ViewModel Factory 생성
                    viewModelFactory = MainViewModel.Factory(detector)
                    
                    // 모델 로딩 시간 측정 종료
                    val loadTime = performanceMonitor.endOperation("model_loading")
                    Log.d(TAG, "Model loading completed in $loadTime ms")
                    
                    // UI 설정
                    setUpUI()
                } else {
                    // 양자화/최적화 실패 시 기존 방식으로 로드
                    loadModelsLegacy()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading optimized models", e)
                // 모델 로딩 실패 처리
                loadModelsLegacy()
            }
        }
    }
    
    /**
     * 최적화된 모델 로드 시도
     * 양자화 또는 캐시된 모델 사용
     */
    private suspend fun loadOptimizedModels(): Pair<OrtSession, OrtSession>? {
        return try {
            // YOLO 모델 로드 (우선 캐시 확인)
            val yoloSession = modelOptimizer.loadModelFromAssets("yolov11n-face.onnx")
            
            // 이진 분류 모델 로드 (양자화 또는 캐시)
            val clsModelName = "deepfake_binary_s128_e5_early.onnx"
            val clsSession = modelQuantizer.loadQuantizedModelOrOriginal(clsModelName)
            
            // 세션 반환
            Pair(yoloSession, clsSession)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load optimized models", e)
            null
        }
    }
    
    /**
     * 기존 방식으로 모델 로드 (최적화 없음)
     */
    private fun loadModelsLegacy() {
        try {
            // 로딩 시간 측정 시작
            performanceMonitor.startOperation("legacy_model_loading")
            
            val env = OrtEnvironment.getEnvironment()
            
            // YOLO onnx
            val yoloBytes = assets.open("yolov11n-face.onnx").readBytes()
            val yoloSession = env.createSession(yoloBytes)
            
            // 이진 분류 onnx
            val clsBytes = assets.open("deepfake_binary_s128_e5_early.onnx").readBytes()
            val clsSession = env.createSession(clsBytes)
            
            // 로딩 시간 측정 종료
            val loadTime = performanceMonitor.endOperation("legacy_model_loading")
            Log.d(TAG, "Legacy model loading completed in $loadTime ms")
            
            // Detector 생성
            detector = DeepfakeDetector(yoloSession, clsSession)
            
            // ViewModel Factory 생성
            viewModelFactory = MainViewModel.Factory(detector)
            
            // UI 설정
            setUpUI()
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadModelsLegacy", e)
            // 심각한 오류: 앱 종료 또는 오류 화면 표시
            Toast.makeText(this, "모델 로딩 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * 백그라운드에서 모델 양자화 작업 시작
     * 초기 앱 로딩 성능에 영향을 주지 않기 위해 별도 쓰레드에서 실행
     */
    private fun startModelQuantizationInBackground() {
        lifecycleScope.launch {
            try {
                // 여기서는 실제로 양자화를 수행하지는 않지만, 
                // 필요한 경우 샘플 이미지와 함께 양자화 로직을 실행할 수 있음
                // 실제 구현에서는 샘플 이미지를 수집하고 양자화 과정을 진행
                Log.d(TAG, "Starting model quantization in background")
                
                // 실제 양자화 로직은 앱 사용에 방해가 없도록 백그라운드에서 실행
            } catch (e: Exception) {
                Log.e(TAG, "Error in model quantization", e)
            }
        }
    }
    
    private fun setUpUI() {
        // UI 렌더링 시간 측정 시작
        performanceMonitor.startOperation("ui_setup")
        
        setContent {
            CVProjectTheme {
                val mainViewModel: MainViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = viewModelFactory
                )
                
                // 프레임 시간 측정 시작
                performanceMonitor.frameStart()
                
                MainScreen(mainViewModel)
                
                // 프레임 시간 측정 종료
                performanceMonitor.frameEnd()
            }
        }
        
        // UI 렌더링 시간 측정 종료
        val uiSetupTime = performanceMonitor.endOperation("ui_setup")
        Log.d(TAG, "UI setup completed in $uiSetupTime ms")
        
        // 백그라운드에서 모델 양자화 작업 시작
        startModelQuantizationInBackground()
    }
    
    override fun onDestroy() {
        // 성능 모니터링 중지
        performanceMonitor.stopMonitoring()
        
        // 리소스 정리
        lifecycleScope.launch {
            modelOptimizer.clearSessionCache()
            modelQuantizer.clearCache()
        }
        
        super.onDestroy()
    }
}

// todo 도움말 다이얼로그 관리 이넘 클래스
enum class DialogState { NONE, HELP1, HELP2, HELP3 }

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val localFocusManager = LocalFocusManager.current
    val context = LocalContext.current
    val activity = context as? Activity
    val sharedPref = remember { activity?.getPreferences(Context.MODE_PRIVATE) }

    // 로컬 UI 상태
    var showTextField by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showTooltipDialog by remember { mutableStateOf(false) }
    var showMultiFaceDialog by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // 첫 실행 체크
    var isFirst by remember {
        val savedFirstRun = sharedPref?.getBoolean("SavedFirstRun", true)
        mutableStateOf(savedFirstRun ?: true)
    }
    
    // 다중 얼굴 결과 표시 여부
    val hasMultipleFaces = uiState.allFaceResults.size > 1
    
    // 얼굴 결과 요약 계산
    val realFaceCount = uiState.allFaceResults.count { it.label == "Real" }
    val fakeFaceCount = uiState.allFaceResults.count { it.label == "Fake" }
    
    // 첫 실행시 ViewModel에 상태 업데이트
    LaunchedEffect(isFirst) {
        viewModel.setFirstRun(isFirst)
        if (isFirst) {
            showInfoDialog = true
        }
    }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                viewModel.setImageUri(uri)
            }
        }

    // 배경 색상
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF427CFA), Color.White)
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(onTap = { localFocusManager.clearFocus() })
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 상단 타이틀
            Spacer(modifier = Modifier.weight(0.07f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo2),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .offset(y = (-25).dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.5f)
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                    InfoDialog(
                        firstRun = isFirst,
                        showDialog = showInfoDialog,
                        onDismiss = {
                            showInfoDialog = false
                            if (isFirst) {
                                isFirst = false
                                sharedPref?.edit()?.putBoolean("SavedFirstRun", false)
                                    ?.apply() // 첫 실행 여부 저장
                                viewModel.setFirstRun(false)
                            }
                        }
                    )
                    IconButton(onClick = { showTooltipDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                    TooltipDialog(
                        showDialog = showTooltipDialog,
                        onDismiss = { showTooltipDialog = false }
                    )
                }
            }
            
            // 이미지 컨테이너 영역
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.blue))
            val lottieAnimatable = rememberLottieAnimatable()
            
            if (uiState.imageUri == null && uiState.imageUrl.isEmpty()) {
                // 이미지가 없을 때 애니메이션 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.9f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Transparent)
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LaunchedEffect(composition) {
                        // 처음 6번은 연속 재생
                        repeat(6) {
                            lottieAnimatable.animate(
                                composition = composition,
                                speed = 1.0f,
                                iterations = 1,
                                cancellationBehavior = LottieCancellationBehavior.OnIterationFinish
                            )
                        }
                        // 이후에는 3초 간격으로 계속 재생
                        while (true) {
                            delay(3000L)
                            lottieAnimatable.animate(
                                composition = composition,
                                speed = 1.0f,
                                iterations = 1,
                                cancellationBehavior = LottieCancellationBehavior.OnIterationFinish
                            )
                        }
                    }
                    LottieAnimation(
                        composition = composition,
                        progress = lottieAnimatable.progress,
                        modifier = Modifier.size(350.dp)
                    )
                }
            } else if (uiState.resultLabel.isEmpty()) {
                // 이미지가 있고 결과가 없을 때 이미지만 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Transparent)
                        .border(2.dp, Color.White, RoundedCornerShape(15.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(uiState.imageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (uiState.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = "Image from URL",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                // 이미지와 결과가 모두 있을 때 이미지와 결과 표시
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.6f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Transparent)
                        .border(2.dp, Color.White, RoundedCornerShape(15.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(uiState.imageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (uiState.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = uiState.imageUrl,
                            contentDescription = "Image from URL",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.size(15.dp))
                
                // 다중 얼굴이 있을 경우 요약 정보 표시
                if (hasMultipleFaces) {
                    AnalysisSummary(
                        realCount = realFaceCount,
                        fakeCount = fakeFaceCount,
                        onViewAllClick = { showMultiFaceDialog = true }
                    )
                } else {
                    // 단일 얼굴 결과 표시 박스
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .clip(RoundedCornerShape(15.dp))
                            .border(2.dp, Color.White, RoundedCornerShape(15.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (uiState.resultLabel == "Fake") {
                                Text(
                                    uiState.resultText,
                                    color = Color.Red,
                                    fontSize = 35.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    uiState.resultConf,
                                    color = Color.DarkGray,
                                    fontSize = 25.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Text(
                                    uiState.resultText,
                                    color = Color.White,
                                    fontSize = 35.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    uiState.resultConf,
                                    color = Color.White,
                                    fontSize = 25.sp,
                                    lineHeight = 40.sp,
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(0.05f))
            
            // 버튼 Row (이미지 선택 및 이미지 주소 입력)
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 이미지 선택 버튼 (커스텀)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(15.dp))
                        .background(Color.White, RoundedCornerShape(15.dp))
                        .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(15.dp))
                        .clickable {
                            Log.d("ButtonUI", "이미지 선택 버튼 클릭됨 - 스타일: 배경=White, 테두리=1dp Black, 그림자=8dp")
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("이미지 선택", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
                
                // 이미지 선택 버튼 스타일 로그
                LaunchedEffect(Unit) {
                    Log.d("ButtonUI", "이미지 선택 버튼 스타일: 배경=Color.White, 테두리=1dp Black, 그림자=8dp")
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // 이미지 주소 입력 버튼 (커스텀)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(8.dp, RoundedCornerShape(15.dp))
                        .background(Color.White, RoundedCornerShape(15.dp))
                        .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(15.dp))
                        .clickable { 
                            Log.d("ButtonUI", "이미지 주소 입력 버튼 클릭됨 - 스타일: 배경=White, 테두리=1dp Black, 그림자=8dp")
                            showTextField = true 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("이미지 주소 입력", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
                
                // 이미지 주소 입력 버튼 스타일 로그
                LaunchedEffect(Unit) {
                    Log.d("ButtonUI", "이미지 주소 입력 버튼 스타일: 배경=Color.White, 테두리=1dp Black, 그림자=8dp")
                }
            }
            
            Spacer(modifier = Modifier.weight(0.04f))
            
            // 이미지 분석 버튼 (커스텀)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp)
                    .shadow(8.dp, RoundedCornerShape(15.dp))
                    .background(
                        Color.White,
                        RoundedCornerShape(15.dp)
                    )
                    .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(15.dp))
                    .clickable(
                        enabled = (uiState.imageUri != null || uiState.imageUrl.isNotEmpty()) && !isAnalyzing && !uiState.isLoading
                    ) {
                        Log.d("ButtonUI", "이미지 분석 버튼 클릭됨 - 스타일: 배경=Color.White, 테두리=1dp Black, 그림자=8dp, 활성화=${(uiState.imageUri != null || uiState.imageUrl.isNotEmpty()) && !isAnalyzing && !uiState.isLoading}")
                        isAnalyzing = true
                        CoroutineScope(Dispatchers.Main).launch {
                            val bitmap = withContext(Dispatchers.IO) {
                                loadBitmap(uiState.imageUri, uiState.imageUrl, context)
                            }
                            
                            if (bitmap != null) {
                                viewModel.analyzeImage(context, bitmap)
                            } else {
                                Toast.makeText(context, "이미지 로딩 실패", Toast.LENGTH_SHORT).show()
                            }
                            isAnalyzing = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("이미지 분석", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
            
            // 시작 시 로그 한 번 출력
            LaunchedEffect(Unit) {
                Log.d("ButtonUI", "이미지 분석 버튼 스타일: 배경=Color.White, 테두리=1dp Black, 그림자=8dp")
            }
            
            Spacer(modifier = Modifier.weight(0.04f))
            
            // 인증마크 버튼 (커스텀)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp)
                    .shadow(8.dp, RoundedCornerShape(15.dp))
                    .background(
                        Color.White,
                        RoundedCornerShape(15.dp)
                    )
                    .border(BorderStroke(1.dp, Color.Black), RoundedCornerShape(15.dp))
                    .clickable(enabled = uiState.resultLabel == "Real") {
                        Log.d("ButtonUI", "인증마크 버튼 클릭됨 - 스타일: 배경=Color.White, 테두리=1dp Black, 그림자=8dp, 활성화=${uiState.resultLabel == "Real"}")
                        CoroutineScope(Dispatchers.Main).launch {
                            val bitmap = withContext(Dispatchers.IO) {
                                loadBitmap(uiState.imageUri, uiState.imageUrl, context)
                            }
                            
                            if (bitmap != null) {
                                // 인증마크 비트맵 로드
                                val authMarkBitmap = BitmapFactory.decodeResource(
                                    context.resources,
                                    R.drawable.bluecertification
                                )
                                
                                // 인증마크 추가 및 저장
                                val success = viewModel.addAuthMarkAndSave(context, bitmap, authMarkBitmap)
                                
                                if (success) {
                                    Toast.makeText(context, "이미지가 갤러리에 저장되었습니다!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("인증마크", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
            }
            
            // 시작 시 로그 한 번 출력
            LaunchedEffect(Unit) {
                Log.d("ButtonUI", "인증마크 버튼 스타일: 배경=Color.White, 테두리=1dp Black, 그림자=8dp")
            }
            
            Spacer(modifier = Modifier.weight(0.1f))
        }
        
        // 로딩 표시
        if (uiState.isLoading || isAnalyzing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                val loadingComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.blue))
                LottieAnimation(
                    composition = loadingComposition,
                    iterations = Int.MAX_VALUE,
                    modifier = Modifier.size(200.dp)
                )
            }
        }
        
        // 텍스트 입력 팝업
        if (showTextField) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.White, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        label = { Text("이미지 주소를 입력하세요.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            border = BorderStroke(0.01.dp, Color.Black),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD2F0FF),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .width(100.dp)
                                .height(40.dp),
                            onClick = { showTextField = false }
                        ) {
                            Text("취소")
                        }
                        Button(
                            border = BorderStroke(0.01.dp, Color.Black),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD2F0FF),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .width(100.dp)
                                .height(40.dp),
                            onClick = {
                                viewModel.setImageUrl(textFieldValue)
                                showTextField = false
                                textFieldValue = ""
                            }
                        ) {
                            Text("업로드")
                        }
                    }
                }
            }
        }
        
        // 다중 얼굴 결과 다이얼로그
        if (showMultiFaceDialog) {
            MultiFaceResultsDialog(
                faceResults = uiState.allFaceResults,
                selectedFaceIndex = uiState.selectedFaceIndex,
                onFaceSelected = { index -> viewModel.selectFace(index) },
                onDismiss = { showMultiFaceDialog = false }
            )
        }
        
        // 에러 메시지 표시
        uiState.errorMessage?.let { errorMsg ->
            LaunchedEffect(errorMsg) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        }
    }
}

suspend fun loadBitmap(
    uri: Uri?,
    imageUrl: String,
    context: Context
): Bitmap? = withContext(Dispatchers.IO) {
    return@withContext when {
        uri != null -> {
            context.contentResolver?.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }

        imageUrl.isNotBlank() -> {
            try {
                val inputStream = java.net.URL(imageUrl).openStream()
                BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        }

        else -> null
    }
}

suspend fun addCertificationMarkFromSourceAsync(source: Any, context: Context): Uri? {
    // Bitmap 초기화
    val bitmap = withContext(Dispatchers.IO) {
        when (source) {
            is Bitmap -> source
            is String -> { // URL 처리
                try {
                    val inputStream = java.net.URL(source).openStream()
                    BitmapFactory.decodeStream(inputStream)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            else -> null
        }
    } ?: return null // 비트맵 생성 실패 시 null 반환
    // 인증마크를 그릴 비트맵 생성
    val overlayBitmap = bitmap.config?.let { Bitmap.createBitmap(bitmap.width, bitmap.height, it) }
        ?: return null // 비트맵 생성 실패 시 null 반환
    val canvas = Canvas(overlayBitmap)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    // 인증마크 Drawable 로드
    val markDrawable = ContextCompat.getDrawable(context, R.drawable.bluecertification)
    if (markDrawable == null) {
        // 인증마크가 없는 경우 로그 출력 또는 기본 동작 수행
        return null
    }
    val density = context.resources.displayMetrics.density
    val marginPx = (4 * density).toInt() // 여백을 dp에서 픽셀로 변환
    val markSize = (bitmap.width * 0.1).toInt() // 인증마크 크기 (이미지의 10%)
    val left = bitmap.width - markSize - marginPx // 우측 하단에 위치
    val top = bitmap.height - markSize - marginPx
    // 인증마크 위치와 크기 설정
    markDrawable.setBounds(left, top, left + markSize, top + markSize)
    markDrawable.alpha = 128 // 50% 투명도
    markDrawable.draw(canvas)
    // 갤러리에 저장
    val contentValues = ContentValues().apply {
        put(
            MediaStore.Images.Media.DISPLAY_NAME,
            "certified_image_${System.currentTimeMillis()}.png"
        )
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BLUE CHECK")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { outputStream ->
            overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }
    }
    return uri
}

@Composable
fun InfoDialog(
    firstRun: Boolean,
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(25.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "BLUE CHECK 캠페인",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.height(15.dp))

                    Column(
                        modifier = Modifier
                            .height(500.dp)
                            .fillMaxWidth()
                            .background(Color.White),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Image(
                            painter = painterResource(R.drawable.bluecheck1),
                            contentDescription = "BLUE CHECK Campaign",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Button(
                            border = BorderStroke(0.01.dp, Color.Black),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD2F0FF),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(45.dp)
                                .shadow(4.dp, RoundedCornerShape(15.dp)),
                            onClick = { onDismiss() }
                        ) {
                            Text(text = "나가기")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TooltipDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit
) {
    var dialogState by remember { mutableStateOf(DialogState.HELP1) }

    val helpDialogText = when (dialogState) {
        DialogState.HELP1 -> "이미지 선택 버튼으로 갤러리 이미지를 업로드하거나 이미지 주소 입력 버튼으로 이미지 주소를 통해 이미지를 업로드하세요!"
        DialogState.HELP2 -> "이미지 분석 버튼은 이미지를 업로드해야 활성화됩니다!"
        DialogState.HELP3 -> "이미지 분석 후 결과가 Real일 때에만 인증마크 버튼이 활성화됩니다!"
        else -> ""
    }
    var numText = if (dialogState == DialogState.HELP1) {
        "1/3"
    } else if (dialogState == DialogState.HELP2) {
        "2/3"
    } else if (dialogState == DialogState.HELP3) {
        "3/3"
    } else {
        ""
    }
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(25.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceAround
                ) {
                    Text(
                        text = "도움말",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Black)
                    )
                    Spacer(modifier = Modifier.height(15.dp))

                    Column(
                        modifier = Modifier
                            .height(150.dp)
                            .fillMaxWidth()
                            .background(Color.White),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = helpDialogText,
                            fontSize = 20.sp,
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            border = BorderStroke(0.01.dp, Color.Black),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD2F0FF),
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(45.dp)
                                .align(Alignment.Center)
                                .shadow(4.dp, RoundedCornerShape(15.dp)),
                            onClick = {
                                when (dialogState) {
                                    DialogState.HELP1 -> dialogState = DialogState.HELP2
                                    DialogState.HELP2 -> dialogState = DialogState.HELP3
                                    else -> {
                                        onDismiss()
                                        dialogState = DialogState.HELP1
                                    }
                                }
                            }
                        ) {
                            Text(text = if (dialogState == DialogState.HELP3) "나가기" else "다음")
                        }
                        Text(
                            text = numText,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 5.dp),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}