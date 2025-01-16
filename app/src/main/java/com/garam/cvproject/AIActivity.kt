package com.garam.cvproject

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.garam.cvproject.ui.theme.CVProjectTheme

class AIActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CVProjectTheme {
                AIScreen()
            }
        }
    }
}

@Composable
fun AIScreen() {
    val context = LocalContext.current as? Activity
    var imageURI by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var textFieldValue by remember { mutableStateOf("") }
    var showTextField by remember { mutableStateOf(false) }

    // Photo Picker launcher
    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                imageURI = uri
                imageUrl = "" // Clear image URL when a new image is selected
            }
        }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.weight(0.1f)) // 비율 기반 여백
            Text("AI Screen", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(0.05f)) // 비율 기반 여백

            // 이미지 컨테이너
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.25f)
                    .background(color = Color.Gray),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center

            ) {
                Text("result")
            }

            Spacer(modifier = Modifier.weight(0.05f)) // 비율 기반 여백

            // 버튼 Row
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
                        .border(
                            color = Color.Black,
                            width = 2.dp,
                            shape = RoundedCornerShape(15.dp)
                        ),
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Text("이미지 선택", fontSize = 15.sp, color = Color.Black)
                }

                Spacer(modifier = Modifier.width(16.dp)) // 버튼 간 간격

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
                        .border(
                            color = Color.Black,
                            width = 2.dp,
                            shape = RoundedCornerShape(15.dp)
                        ),
                    onClick = {
                        showTextField = true
                    }
                ) {
                    Text("이미지 주소 입력", fontSize = 15.sp, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.weight(0.03f)) // 비율 기반 여백

            // 분석 버튼
            Button(
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp)
                    .border(color = Color.Black, width = 2.dp, shape = RoundedCornerShape(15.dp)),
                onClick = {
                    // 분석 로직 추가 가능
                }
            ) {
                Text("이미지 분석", fontSize = 20.sp, color = Color.Black)
            }

            Spacer(modifier = Modifier.weight(0.03f)) // 비율 기반 여백

            // 뒤로가기 버튼
            Button(
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp)
                    .border(color = Color.Black, width = 2.dp, shape = RoundedCornerShape(15.dp)),
                onClick = {
                    context?.finish()
                }
            ) {
                Text("뒤로가기", fontSize = 20.sp, color = Color.Black)
            }
            Spacer(modifier = Modifier.weight(0.1f)) // 비율 기반 여백
        }

        // 팝업으로 텍스트 입력 필드
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
                        Button(onClick = {
                            showTextField = false
                        }) {
                            Text("Cancel")
                        }
                        Button(onClick = {
                            imageUrl = textFieldValue
                            imageURI = null // Clear image URI when a URL is entered
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

@Preview(showBackground = true)
@Composable
fun AIScreenPreview() {
    CVProjectTheme {
        AIScreen()
    }
}