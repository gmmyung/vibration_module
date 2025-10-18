package com.example.chatting_app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 점자 입력 그리드 컴포넌트
 * 6개 점을 2x3 그리드로 배치하여 점자 입력을 받음
 */
@Composable
fun BrailleInputGrid(
    modifier: Modifier = Modifier,
    brailleInputState: StateFlow<BrailleInputState>,
    onDotTouched: (Int) -> Unit,
    onComplete: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    onNextVoicePattern: () -> Unit = {},
    onPreviousVoicePattern: () -> Unit = {},
    onExitVoiceMode: () -> Unit = {}
) {
    val state by brailleInputState.collectAsState()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 점자 그리드 (2x3)
        BrailleDotsGrid(
            dots = state.dots,
            onDotTouched = if (state.isShowingVoiceBraille) { _ -> } else onDotTouched,
            isVoiceMode = state.isShowingVoiceBraille
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 음성 점자 표시 모드일 때의 정보 표시
        if (state.isShowingVoiceBraille) {
            VoiceBrailleInfo(
                currentIndex = state.currentVoicePatternIndex,
                totalPatterns = state.voiceBraillePatterns.size
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 입력된 텍스트 표시 (음성 모드가 아닐 때만)
        if (state.inputText.isNotEmpty() && !state.isShowingVoiceBraille) {
            InputTextDisplay(
                text = state.inputText
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 컨트롤 버튼들 (항상 표시, 음성 모드에 따라 기능 변경)
        ControlButtons(
            state = state,
            onComplete = if (state.isShowingVoiceBraille) {
                android.util.Log.d("BrailleInputGrid", "음성 모드 - 종료 버튼 콜백 설정")
                onExitVoiceMode
            } else {
                android.util.Log.d("BrailleInputGrid", "수동 모드 - 완료 버튼 콜백 설정")
                onSend
            },
            onClear = if (state.isShowingVoiceBraille) {
                android.util.Log.d("BrailleInputGrid", "음성 모드 - 이전 버튼 콜백 설정")
                onPreviousVoicePattern
            } else {
                android.util.Log.d("BrailleInputGrid", "수동 모드 - 지우기 버튼 콜백 설정")
                onClear
            },
            onSend = if (state.isShowingVoiceBraille) {
                android.util.Log.d("BrailleInputGrid", "음성 모드 - 다음 버튼 콜백 설정")
                onNextVoicePattern
            } else {
                android.util.Log.d("BrailleInputGrid", "수동 모드 - 다음 버튼 콜백 설정")
                onComplete
            },
            isVoiceMode = state.isShowingVoiceBraille
        )
        
    }
}

/**
 * 6개 점을 2x3 그리드로 배치한 컴포넌트
 */
@Composable
private fun BrailleDotsGrid(
    modifier: Modifier = Modifier,
    dots: List<Boolean>,
    onDotTouched: (Int) -> Unit,
    isVoiceMode: Boolean = false
) {
    // 화면 절반 크기에 맞는 점자 그리드 설정
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val availableWidth = screenWidth * 0.4f // 화면의 40% 정도 (절반의 80%)
    
    // 점자 그리드 크기 계산 (화면 크기에 비례)
    val gridWidth = availableWidth.coerceAtLeast(200.dp).coerceAtMost(300.dp)
    val dotSize = (gridWidth * 0.15f).coerceAtLeast(30.dp).coerceAtMost(50.dp)
    val dotSpacing = dotSize * 0.4f
    val columnSpacing = dotSize * 0.5f
    val padding = dotSize * 0.3f
    
    Surface(
        modifier = modifier
            .width(gridWidth)
            .height(gridWidth * 0.7f), // 세로는 가로의 70% 정도
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                        
            Spacer(modifier = Modifier.height(8.dp))
            
            // 점자 그리드 (2x3)
            Row(
                horizontalArrangement = Arrangement.spacedBy(columnSpacing)
            ) {
                // 왼쪽 열 (1, 2, 3번 점)
                Column(
                    verticalArrangement = Arrangement.spacedBy(dotSpacing)
                ) {
                    BrailleDot(
                        dotNumber = 1,
                        isActive = dots[0],
                        onTouched = { onDotTouched(0) },
                        isVoiceMode = isVoiceMode,
                        dotSize = dotSize
                    )
                    BrailleDot(
                        dotNumber = 2,
                        isActive = dots[1],
                        onTouched = { onDotTouched(1) },
                        isVoiceMode = isVoiceMode,
                        dotSize = dotSize
                    )
                    BrailleDot(
                        dotNumber = 3,
                        isActive = dots[2],
                        onTouched = { onDotTouched(2) },
                        isVoiceMode = isVoiceMode,
                        dotSize = dotSize
                    )
                }
                
                // 오른쪽 열 (4, 5, 6번 점)
                Column(
                    verticalArrangement = Arrangement.spacedBy(dotSpacing)
                ) {
                    BrailleDot(
                        dotNumber = 4,
                        isActive = dots[3],
                        onTouched = { onDotTouched(3) },
                        isVoiceMode = isVoiceMode,
                        dotSize = dotSize
                    )
                    BrailleDot(
                        dotNumber = 5,
                        isActive = dots[4],
                        onTouched = { onDotTouched(4) },
                        isVoiceMode = isVoiceMode,
                        dotSize = dotSize
                    )
                    BrailleDot(
                        dotNumber = 6,
                        isActive = dots[5],
                        onTouched = { onDotTouched(5) },
                        isVoiceMode = isVoiceMode,
                        dotSize = dotSize
                    )
                }
            }
        }
    }
}

/**
 * 개별 점자 점 컴포넌트
 */
@Composable
private fun BrailleDot(
    modifier: Modifier = Modifier,
    dotNumber: Int,
    isActive: Boolean,
    onTouched: () -> Unit,
    isVoiceMode: Boolean = false,
    dotSize: androidx.compose.ui.unit.Dp = 48.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1f,
        animationSpec = tween(100),
        label = "dot_scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isVoiceMode) 0.7f else if (isActive) 1f else 0.5f,
        animationSpec = tween(100),
        label = "dot_alpha"
    )
    
    Box(
        modifier = modifier
            .size(dotSize)
            .scale(scale)
            .background(
                color = if (isActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = alpha),
                shape = CircleShape
            )
            .border(
                width = if (isActive) 3.dp else 2.dp,
                color = if (isActive) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.outline.copy(alpha = alpha),
                shape = CircleShape
            )
            .clickable(enabled = !isVoiceMode) { 
                android.util.Log.d("BrailleInputGrid", "점자 점 ${dotNumber}번 터치됨")
                onTouched() 
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dotNumber.toString(),
            color = if (isActive) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            fontSize = (dotSize * 0.35f).value.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


/**
 * 입력된 텍스트 표시 컴포넌트
 */
@Composable
private fun InputTextDisplay(
    modifier: Modifier = Modifier,
    text: String
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "입력된 텍스트",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * 컨트롤 버튼들 컴포넌트
 */
@Composable
private fun ControlButtons(
    modifier: Modifier = Modifier,
    state: BrailleInputState,
    onComplete: () -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    isVoiceMode: Boolean = false
) {
    // 화면 크기에 맞는 버튼 크기 설정
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val availableWidth = screenWidth * 0.4f // 점자 그리드와 같은 영역
    
    val buttonHeight = (availableWidth * 0.12f).coerceAtLeast(36.dp).coerceAtMost(48.dp)
    val buttonPadding = (availableWidth * 0.02f).coerceAtLeast(2.dp).coerceAtMost(6.dp)
    val textSize = 12.sp
    
    // 디버깅 로그
    android.util.Log.d("BrailleInputGrid", "버튼 크기 계산: availableWidth=${availableWidth}, buttonHeight=${buttonHeight}, textSize=${textSize}")
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 완료 버튼 (음성 모드에서는 종료)
        Button(
            onClick = {
                android.util.Log.d("BrailleInputGrid", "완료/종료 버튼 클릭됨")
                onComplete()
            },
            enabled = true, // 항상 활성화
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = buttonPadding)
                .height(buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isVoiceMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (isVoiceMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            Text(
                text = if (isVoiceMode) "종료" else "다음",
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                    color = if (isVoiceMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // 지우기/이전 버튼
        Button(
            onClick = {
                android.util.Log.d("BrailleInputGrid", "지우기/이전 버튼 클릭됨")
                onClear()
            },
            enabled = if (isVoiceMode) state.currentVoicePatternIndex > 0 else true, // 음성 모드에서는 첫 번째가 아닐 때만 활성화
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = buttonPadding)
                .height(buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isVoiceMode) "이전" else "지우기",
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // 전송/다음 버튼
        Button(
            onClick = {
                android.util.Log.d("BrailleInputGrid", "전송/다음 버튼 클릭됨")
                android.util.Log.d("BrailleInputGrid", "현재 inputText: '${state.inputText}'")
                android.util.Log.d("BrailleInputGrid", "현재 패턴: '${state.currentPattern}'")
                android.util.Log.d("BrailleInputGrid", "완료대기: ${state.isWaitingForComplete}")
                android.util.Log.d("BrailleInputGrid", "음성 모드: $isVoiceMode")
                android.util.Log.d("BrailleInputGrid", "음성 점자 패턴 수: ${state.voiceBraillePatterns.size}")
                android.util.Log.d("BrailleInputGrid", "현재 음성 패턴 인덱스: ${state.currentVoicePatternIndex}")
                onSend()
            },
            enabled = if (isVoiceMode) state.currentVoicePatternIndex < state.voiceBraillePatterns.size - 1 else true, // 음성 모드에서는 마지막이 아닐 때만 활성화
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = buttonPadding)
                .height(buttonHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
            Text(
                text = if (isVoiceMode) "다음" else "완료",
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

/**
 * 음성 점자 표시 모드에서의 정보 표시 컴포넌트
 */
@Composable
private fun VoiceBrailleInfo(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    totalPatterns: Int
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "음성 점자 표시",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${currentIndex + 1} / $totalPatterns",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
