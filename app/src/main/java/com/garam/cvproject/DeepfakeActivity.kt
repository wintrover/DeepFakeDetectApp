package com.garam.cvproject

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.cvproject.ui.theme.CVProjectTheme

class DeepfakeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CVProjectTheme {
                DeepfakeScreen()
            }
        }
    }
}

@Composable
fun DeepfakeScreen() {
    val context = LocalContext.current as? Activity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // 여백 추가
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.weight(0.1f)) // 비율 기반 여백
        Text("Deepfake Screen", fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(0.05f)) // 비율 기반 여백

        // 이미지 컨테이너
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f) // 비율 기반 크기
                .background(color = Color.Gray),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("image", fontSize = 20.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.weight(0.1f)) // 비율 기반 여백

        // 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 아래 버튼과 동일한 너비 비율로 설정
                .align(Alignment.CenterHorizontally), // 중앙 정렬
            horizontalArrangement = Arrangement.SpaceBetween // 버튼 간 간격 조정
        ) {
            // 첫 번째 버튼
            Button(
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, // 배경색
                    contentColor = Color.Black   // 텍스트 색상
                ),
                modifier = Modifier
                    .weight(1f) // 버튼 너비 균등 분배
                    .height(50.dp)
                    .border(color = Color.Black, width = 2.dp, shape = RoundedCornerShape(15.dp)),
                onClick = {
                }
            ) {
                Text("이미지 선택", fontSize = 15.sp, color = Color.Black)
            }

            // Spacer: 버튼 간 간격
            Spacer(modifier = Modifier.width(16.dp)) // 고정된 간격 설정

            // 두 번째 버튼
            Button(
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, // 배경색
                    contentColor = Color.Black   // 텍스트 색상
                ),
                modifier = Modifier
                    .weight(1f) // 버튼 너비 균등 분배
                    .height(50.dp)
                    .border(color = Color.Black, width = 2.dp, shape = RoundedCornerShape(15.dp)),
                onClick = {
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
                containerColor = Color.White, // 배경색
                contentColor = Color.Black   // 텍스트 색상
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f) // 화면 너비에 비례
                .height(50.dp)
                .border(color = Color.Black, width = 2.dp, shape = RoundedCornerShape(15.dp)),
            onClick = {
            }
        ) {
            Text("이미지 분석", fontSize = 20.sp, color = Color.Black)
        }
        Spacer(modifier = Modifier.weight(0.03f)) // 비율 기반 여백
        // 뒤로가기 번째 버튼
        Button(
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White, // 배경색
                contentColor = Color.Black   // 텍스트 색상
            ),
            modifier = Modifier
                .fillMaxWidth(0.9f) // 화면 너비에 비례
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
}

@Preview(showBackground = true)
@Composable
fun DeepfakeScreenPreview() {
    CVProjectTheme {
        DeepfakeScreen()
    }
}