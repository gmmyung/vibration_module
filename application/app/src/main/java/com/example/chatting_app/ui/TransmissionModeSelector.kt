package com.example.chatting_app.ui

import com.example.chatting_app.TransmissionMode
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 전송 모드 선택 컴포넌트
 */
@Composable
fun TransmissionModeSelector(
    currentMode: TransmissionMode,
    onModeChanged: (TransmissionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "모드:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 패턴 모드
            Row(
                modifier = Modifier
                    .selectable(
                        selected = currentMode == TransmissionMode.PATTERN,
                        onClick = { onModeChanged(TransmissionMode.PATTERN) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == TransmissionMode.PATTERN,
                    onClick = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "패턴",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // 개별 점 모드
            Row(
                modifier = Modifier
                    .selectable(
                        selected = currentMode == TransmissionMode.DOT_BY_DOT,
                        onClick = { onModeChanged(TransmissionMode.DOT_BY_DOT) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = currentMode == TransmissionMode.DOT_BY_DOT,
                    onClick = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "개별점",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
