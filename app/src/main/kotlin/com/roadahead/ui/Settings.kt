package com.roadahead.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadahead.core.model.DirectionConvention

enum class AppMode(val label: String) { LIVE("실시간"), DEMO_A("시연 A"), DEMO_B("시연 B"), DEMO_C("시연 C") }
data class Prefs(val mode: AppMode, val convention: DirectionConvention, val disclaimerSeen: Boolean)

fun readPrefs(context: Context): Prefs {
    val store = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return Prefs(AppMode.entries.firstOrNull { it.name == store.getString("mode", null) } ?: AppMode.LIVE,
        DirectionConvention.entries.firstOrNull { it.name == store.getString("convention", null) } ?: DirectionConvention.APPROACH,
        store.getBoolean("disclaimerSeen", false))
}

fun writePrefs(context: Context, prefs: Prefs) {
    context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        .putString("mode", prefs.mode.name).putString("convention", prefs.convention.name)
        .putBoolean("disclaimerSeen", prefs.disclaimerSeen).apply()
}

@Composable
fun Settings(prefs: Prefs, onChange: (Prefs) -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("설정", style = MaterialTheme.typography.headlineMedium)
        Text("모드")
        AppMode.entries.forEach { mode ->
            Choice(mode.label, prefs.mode == mode) { onChange(prefs.copy(mode = mode)) }
        }
        Text("방향 해석")
        Text("방향 해석은 실시간 모드에만 적용돼요.")
        DirectionConvention.entries.forEach { convention ->
            Choice(if (convention == DirectionConvention.APPROACH) "진입 방향" else "진행 방향", prefs.convention == convention) {
                onChange(prefs.copy(convention = convention))
            }
        }
        Button(onClick = onClose, modifier = Modifier.heightIn(min = 64.dp)) { Text("닫기", fontSize = 24.sp) }
    }
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp), fontSize = 24.sp)
    }
}
