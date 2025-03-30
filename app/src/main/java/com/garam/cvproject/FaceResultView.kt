package com.garam.cvproject

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * 다중 얼굴 결과 뷰 컴포넌트
 * 여러 얼굴 결과를 가로 스크롤로 표시
 */
@Composable
fun MultiFaceResultsDialog(
    faceResults: List<MainViewModel.FaceResult>,
    selectedFaceIndex: Int,
    onFaceSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "얼굴 분석 결과 (${faceResults.size}개)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(faceResults) { result ->
                        FaceResultCard(
                            faceResult = result,
                            isSelected = result.faceIndex == selectedFaceIndex,
                            onClick = { onFaceSelected(result.faceIndex) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 선택된 얼굴 상세 결과
                val selectedFace = faceResults.find { it.faceIndex == selectedFaceIndex }
                selectedFace?.let { face ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        face.croppedBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected Face",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = face.message,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 결과 아이콘 표시
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (face.label == "Real") Icons.Default.Check else Icons.Default.Warning,
                                    contentDescription = face.label,
                                    tint = if (face.label == "Real") Color.Green else Color.Red,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = "신뢰도: %.2f%%".format(face.confidence * 100),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (face.label == "Real") Color.Green else Color.Red
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 개별 얼굴 결과 카드
 * 클릭하여 선택 가능
 */
@Composable
fun FaceResultCard(
    faceResult: MainViewModel.FaceResult,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) Color.Blue else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 얼굴 이미지
            faceResult.croppedBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Face ${faceResult.faceIndex + 1}",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 얼굴 번호
            Text(
                text = "${faceResult.faceIndex + 1}번 얼굴",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp
            )
            
            // 결과 라벨
            Text(
                text = faceResult.label,
                color = if (faceResult.label == "Real") Color.Green else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 분석 요약 보기
 * 전체 결과의 요약정보 표시
 */
@Composable
fun AnalysisSummary(
    realCount: Int,
    fakeCount: Int,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "분석 결과 요약",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Real 개수
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Real",
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "진짜: $realCount",
                    color = Color.Green,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Fake 개수
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Fake",
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "가짜: $fakeCount",
                    color = Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "모든 결과 보기",
            color = Color.Blue,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable(onClick = onViewAllClick)
                .padding(8.dp)
        )
    }
} 