@file:OptIn(ExperimentalMaterial3Api::class)

package com.garam.cvproject

import ai.onnxruntime.OrtEnvironment
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
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

class MainActivity : ComponentActivity() {
    private lateinit var detector: DeepfakeDetector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val env = OrtEnvironment.getEnvironment()

        // YOLO onnx
        val yoloBytes = assets.open("yolov11n-face.onnx").readBytes()
        val yoloSession = env.createSession(yoloBytes)

        // 이진 분류 onnx
        val clsBytes = assets.open("deepfake_binary_s128_e5_early.onnx").readBytes()
        val clsSession = env.createSession(clsBytes)

        // Detector 생성
        detector = DeepfakeDetector(yoloSession, clsSession)
        setContent {
            CVProjectTheme {
                MainScreen(detector)
            }
        }
    }
}

// todo 도움말 다이얼로그 관리 이넘 클래스
enum class DialogState { NONE, HELP1, HELP2, HELP3 }

@Composable
fun MainScreen(detector: DeepfakeDetector) {
    var croppedFaceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    val sharedPref = remember { activity?.getPreferences(Context.MODE_PRIVATE) }

    var isFirst by remember {
        val savedFirstRun = sharedPref?.getBoolean("SavedFirstRun", true)
        mutableStateOf(savedFirstRun ?: true)
    }

    var imageURI by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf("") }
    var showTextField by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") } // 결과값 표시를 위한 상태
    var resultLabel by remember { mutableStateOf("") }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                imageUrl = ""
                imageURI = uri
                resultText = ""
                resultLabel = ""
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
    ) {
        var showInfoDialog by remember { mutableStateOf(false) } // 추가된 정보 버튼
        if (isFirst) {
            showInfoDialog = true
        }
        var showTooltipDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showInfoDialog = true }) {  // todo info
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
//            Spacer(modifier = Modifier.weight(0.005f))
            // 이미지 컨테이너
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.blue))
            val lottieAnimatable = rememberLottieAnimatable()
            if (imageUrl == "" && imageURI == null) {
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
                        // 1) 처음 6번은 연속 재생
                        repeat(6) {
                            lottieAnimatable.animate(
                                composition = composition,
                                speed = 1.0f,
                                iterations = 1,
                                cancellationBehavior = LottieCancellationBehavior.OnIterationFinish
                            )
                        }
                        // 2) 이후에는 3초 간격으로 계속 재생
                        while (true) {
                            // 3초 대기
                            delay(3000L)
                            // 한 번 재생
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
            } else if ((imageUrl.isNotBlank() || imageURI != null) && resultText == "") {
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
                    if (imageURI != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageURI),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Image from URL",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("이미지를 업로드하세요!", fontSize = 20.sp, color = Color.White)
                    }
                }
            } else if (resultText != "") {
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
                    if (imageURI != null) {
                        Image(
                            painter = rememberAsyncImagePainter(imageURI),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Image from URL",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text("이미지를 업로드하세요!", fontSize = 20.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.size(15.dp))
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
                        if (resultLabel == "Fake") {
                            Text(
                                resultText,
                                color = Color.Red,
                                fontSize = 25.sp,
                                lineHeight = 40.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                resultText,
                                color = Color.White,
                                fontSize = 25.sp,
                                lineHeight = 40.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
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
                // 이미지 선택 버튼
                Button(
                    border = BorderStroke(0.01.dp, Color.Black),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(4.dp, RoundedCornerShape(15.dp)),
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text("이미지 선택", fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                // 이미지 주소 입력 버튼
                Button(
                    border = BorderStroke(0.01.dp, Color.Black),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .shadow(4.dp, RoundedCornerShape(15.dp)),
                    onClick = { showTextField = true }
                ) {
                    Text("이미지 주소 입력", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.weight(0.04f))
            // 이미지 분석 버튼
            Button(
                border = BorderStroke(0.01.dp, Color.Black),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = imageURI != null || imageUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        // 1) Bitmap 로딩
                        val bitmap = withContext(Dispatchers.IO) {
                            loadBitmap(imageURI, imageUrl, context)
                        }
                        if (bitmap != null) {
                            // 2) DeepfakeDetector로 얼굴검출+분류
                            val bestResult = detector.detectAndClassify(bitmap)
                            // 3) 결과 처리
                            if (bestResult != null) {
                                resultText = bestResult.message
                                // 크롭된 얼굴 이미지를 State에 저장
                                croppedFaceBitmap = bestResult.croppedBitmap
                                resultLabel = bestResult.label
                            } else {
                                resultText = "결과 없음"
                                croppedFaceBitmap = null
                            }
                        } else {
                            resultText = "이미지 로딩 실패"
                            croppedFaceBitmap = null
                        }
                    }
                }) {
                Text("이미지 분석", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.weight(0.04f))
            // 인증마크 버튼
            Button(
                border = BorderStroke(0.01.dp, Color.Black),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD2F0FF),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
                enabled = resultLabel == "Real", // "real"일 때만 활성화
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        val source = when {
                            imageURI != null -> context?.contentResolver?.openInputStream(imageURI!!)
                                ?.let {
                                    BitmapFactory.decodeStream(it)
                                }

                            imageUrl.isNotBlank() -> imageUrl
                            else -> null
                        }
                        if (source != null) {
                            val markedImageUri =
                                addCertificationMarkFromSourceAsync(source, context)
                            if (markedImageUri != null) {
                                Toast.makeText(context, "이미지가 갤러리에 저장되었습니다!", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                Toast.makeText(context, "이미지 저장에 실패했습니다.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            Toast.makeText(context, "이미지를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text("인증마크", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.weight(0.1f))
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
                    TextField(
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
                            onClick = { showTextField = false }) {
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
                                imageUrl = textFieldValue
                                imageURI = null
                                resultText = ""
                                resultLabel = ""
                                showTextField = false
                            }) {
                            Text("업로드")
                        }
                    }
                }
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
    val marginPx = (8 * density).toInt() // 여백을 dp에서 픽셀로 변환
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
                                .height(45.dp),
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
//                            .weight(1f)
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
                                .align(Alignment.Center),
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