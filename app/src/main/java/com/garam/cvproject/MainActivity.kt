@file:OptIn(ExperimentalMaterial3Api::class)

package com.garam.cvproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.cvproject.ui.theme.CVProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    // 배경 색상 (그라데이션)
    Box(
        modifier = Modifier
            .fillMaxSize()
//            .background(
//                brush = Brush.verticalGradient(
//                    colors = listOf(Color(0xFF404040), Color(0xFFBFBFBF))
//                )
//            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
//                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상단 영역
            Box(
                modifier = Modifier
                    .weight(0.2f)
                    .fillMaxWidth(),
//                    .background(color = Color(0xFF004FFF)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                    .background(color = Color(0xFF004FFF)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // 타이틀 텍스트
                    Text(
                        "AiGO",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
//                        modifier = Modifier.shadow(8.dp)
                    )
                }
            }

            // 이미지 컨테이너
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.Transparent), // 배경색 투명 설정
//                    .border(2.dp, Color.White, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
//                Image(
//                    painter = painterResource(R.drawable.aikiller),
//                    contentDescription = null,
//                    modifier = Modifier.fillMaxSize()
////                        .clip(RoundedCornerShape(40.dp))
//                )
                Text("Image Placeholder", fontSize = 20.sp, color = Color.White)
            }

            // 버튼 영역
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 첫 번째 버튼
                Button(
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00796B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(50.dp)
                        .shadow(4.dp, RoundedCornerShape(15.dp)),
                    onClick = {
                        val intent1 = Intent(context, AIActivity::class.java)
                        context.startActivity(intent1)
                    }
                ) {
                    Text("AI 이미지 화면", fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.height(30.dp))

                // 두 번째 버튼
                Button(
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(50.dp)
                        .shadow(4.dp, RoundedCornerShape(15.dp)),
                    onClick = {
                        val intent2 = Intent(context, DeepfakeActivity::class.java)
                        context.startActivity(intent2)
                    }
                ) {
                    Text("Deepfake 이미지 화면", fontSize = 18.sp)
                }
            }

            // 하단 크레딧
            Box(
                modifier = Modifier
                    .weight(0.1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "© close AI",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CVProjectTheme {
        MainScreen()
    }
}