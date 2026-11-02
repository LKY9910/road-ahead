package com.roadahead.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadahead.core.Screen
import com.roadahead.R

data class AppStatus(val modeLabel: String?, val speechAvailable: Boolean, val note: String?)

@Composable
fun RoadAheadScreen(screen: Screen, status: AppStatus, parked: Boolean, onOpenSettings: () -> Unit,
    overlayAllowed: Boolean, onOpenAppInfo: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text("앞길 안내", fontSize = 32.sp)
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (overlayAllowed) "내비 화면 위에 안내 카드를 보여 드려요" else stringResource(R.string.overlay_permission),
                fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            if (!overlayAllowed) {
                Spacer(Modifier.height(24.dp))
                Button(onClick = onOpenAppInfo, modifier = Modifier.heightIn(min = 64.dp)) {
                    Text(stringResource(R.string.open_app_info), fontSize = 24.sp)
                }
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
