@file:OptIn(ExperimentalMaterial3Api::class)

package com.garam.cvproject

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.garam.cvproject.ui.theme.CVProjectTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CVProjectTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var imageURI by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf("") }
    var showTextField by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("결과: 없음") } // 결과값 표시를 위한 상태

    val env = OrtEnvironment.getEnvironment()
    val session: OrtSession =
        env.createSession(context.assets.open("deepfake_binary_s128_e5_early.onnx").readBytes())

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                imageURI = uri
                imageUrl = ""
                resultText = "분석 필요"
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
        var showTooltip by remember { mutableStateOf(false) }
        if (showTooltip) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(-60, 225) // 아이콘 아래로 약간의 여백
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "이미지 업로드 시 이미지 분석 버튼 활성화\n이미지가 real일 때 인증마크 버튼 활성화",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 20.dp, top = 41.dp)
                .align(Alignment.TopEnd)
                .size(30.dp)
                .clickable { showTooltip = !showTooltip }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // 상단 타이틀
            Spacer(modifier = Modifier.weight(0.07f))
            Text(
                "AiGO",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
//                modifier = Modifier.shadow(8.dp)
            )
            Spacer(modifier = Modifier.weight(0.08f))
            // 이미지 컨테이너
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.Transparent)
                    .border(2.dp, Color.White, RoundedCornerShape(15.dp)),
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
                    Text("image", fontSize = 20.sp, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.size(15.dp))
            // 결과 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
                    .clip(RoundedCornerShape(15.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .fillMaxHeight()
                            .background(color = Color.Yellow)
                    ) {
                        Text("크롭된 이미지")
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(resultText, color = Color.White, fontSize = 20.sp)
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
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                enabled = imageURI != null || imageUrl.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
//                    .shadow(4.dp, RoundedCornerShape(15.dp)),
                onClick = {
                    CoroutineScope(Dispatchers.Main).launch {
                        val bitmap = if (imageURI != null) {
                            val inputStream = context.contentResolver?.openInputStream(imageURI!!)
                            BitmapFactory.decodeStream(inputStream)
                        } else if (imageUrl.isNotBlank()) {
                            try {
                                withContext(Dispatchers.IO) {
                                    val inputStream = java.net.URL(imageUrl).openStream()
                                    BitmapFactory.decodeStream(inputStream)
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }
                        bitmap?.let {
                            try {
                                // 이미지 전처리
                                val inputTensor = preprocessImageForOnnx(it, env)

                                // ONNX 모델 추론
                                val output = session.run(mapOf("input" to inputTensor))
                                val resultArray = (output[0].value as Array<FloatArray>)[0]
                                val maxIndex =
                                    resultArray.indices.maxByOrNull { idx -> resultArray[idx] }
                                        ?: -1
                                resultText = if (maxIndex == 0) "fake" else "real"
                            } catch (e: Exception) {
                                resultText = "예측 중 오류 발생: ${e.message}"
                            }
                        } ?: run {
                            resultText = "이미지를 처리할 수 없습니다."
                        }
                    }
                }
            ) {
                Text("이미지 분석", fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.weight(0.04f))
            // 인증마크 버튼
            Button(
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
//                    .shadow(4.dp, RoundedCornerShape(15.dp)),
                enabled = resultText == "real", // "real"일 때만 활성화
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
                                addCertificationMarkFromSourceAsync(source, context!!)
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
                        label = { Text("Enter Image URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { showTextField = false }) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            imageUrl = textFieldValue
                            imageURI = null
                            resultText = "분석 필요"
                            showTextField = false
                        }) {
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

/**
 * ONNX 전용 이미지 전처리 함수 (예: 128x128 크기 변환, RGB 정규화)
 */
private fun preprocessImageForOnnx(bitmap: Bitmap, env: OrtEnvironment): OnnxTensor {
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 128, 128, true)
    val floatBuffer = FloatBuffer.allocate(1 * 3 * 128 * 128)

    val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    val pixels = IntArray(128 * 128)
    resizedBitmap.getPixels(pixels, 0, 128, 0, 0, 128, 128)

    for (y in 0 until 128) {
        for (x in 0 until 128) {
            val pixel = pixels[y * 128 + x]
            val r = ((pixel shr 16 and 0xFF) / 255.0f - mean[0]) / std[0]
            val g = ((pixel shr 8 and 0xFF) / 255.0f - mean[1]) / std[1]
            val b = ((pixel and 0xFF) / 255.0f - mean[2]) / std[2]
            floatBuffer.put(b)
            floatBuffer.put(g)
            floatBuffer.put(r)
        }
    }
    floatBuffer.rewind()
    return OnnxTensor.createTensor(env, floatBuffer, longArrayOf(1, 3, 128, 128))
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
    val markDrawable = ContextCompat.getDrawable(context, R.drawable.certificationmark)
    if (markDrawable == null) {
        // 인증마크가 없는 경우 로그 출력 또는 기본 동작 수행
        return null
    }

    val density = context.resources.displayMetrics.density
    val marginPx = (2 * density).toInt() // 여백을 dp에서 픽셀로 변환
    val markSize = (bitmap.width * 0.2).toInt() // 인증마크 크기 (이미지의 20%)
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
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AiGO")
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