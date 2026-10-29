package com.roadahead.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadahead.core.Screen
import com.roadahead.core.model.LightColor

data class AppStatus(val modeLabel: String?, val speechAvailable: Boolean, val note: String?)

@Composable
fun RoadAheadScreen(screen: Screen, status: AppStatus, parked: Boolean, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        val signal = screen.signal
        if (signal != null) {
            val (color, label) = when (signal.color) {
                LightColor.RED -> Color(0xFFFF6259) to "빨간불"
                LightColor.GREEN -> Color(0xFF54D694) to "초록불"
                LightColor.YELLOW -> Color(0xFFFFD166) to "노란불"
                LightColor.UNKNOWN -> Color.Gray to "신호 정보 없음"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(Modifier.size(48.dp).background(color, CircleShape).semantics { contentDescription = label })
                Text(signal.movement.label, fontSize = 32.sp)
                Text(signal.remainingSec?.let { "${it}초" } ?: "—", fontSize = 96.sp)
            }
        } else {
            Text("앞길 안내", fontSize = 32.sp)
        }
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.CenterStart) {
            if (signal == null && screen.hazardText.isNullOrEmpty()) {
                Text("앞길에 알려 드릴 신호나 위험이 없어요", Modifier.align(Alignment.Center),
                    fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                Text(screen.hazardText.orEmpty(), fontSize = 36.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val line = listOfNotNull(status.modeLabel,
                "음성 안내를 쓸 수 없어요".takeUnless { status.speechAvailable },
                "신호 조회 한도를 다 썼어요".takeIf { screen.signalBudgetExhausted }, status.note).joinToString(" · ")
            Text(line, Modifier.weight(1f), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (parked) Button(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 64.dp)) { Text("설정") }
        }
    }
}
