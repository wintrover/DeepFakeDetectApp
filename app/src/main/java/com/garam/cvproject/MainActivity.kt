@file:OptIn(ExperimentalMaterial3Api::class)

package com.garam.cvproject

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // 여백 추가
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.weight(0.1f)) // 비율 기반 여백
        Text("AI Killer", fontSize = 40.sp, fontWeight = FontWeight.Bold)
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

        // 첫 번째 버튼
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
                val intent1 = Intent(context, AIActivity::class.java)
                context.startActivity(intent1)
            }
        ) {
            Text("AI 이미지 화면", fontSize = 20.sp, color = Color.Black)
        }

        Spacer(modifier = Modifier.weight(0.05f)) // 비율 기반 여백

        // 두 번째 버튼
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
                val intent2 = Intent(context, DeepfakeActivity::class.java)
                context.startActivity(intent2)
            }
        ) {
            Text("Deepfake 이미지 화면", fontSize = 20.sp, color = Color.Black)
        }

        Spacer(modifier = Modifier.weight(0.1f)) // 비율 기반 여백
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    CVProjectTheme {
        MainScreen()
    }
}