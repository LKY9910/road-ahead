package com.roadahead.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadahead.core.model.DirectionConvention
import com.roadahead.core.model.Companion

enum class AppMode(val label: String) { LIVE("실시간"), DEMO_A("시연 A"), DEMO_B("시연 B"), DEMO_C("시연 C"), DEMO_E("시연 E") }
data class Prefs(val mode: AppMode, val convention: DirectionConvention, val disclaimerSeen: Boolean,
    val travelMode: Boolean = false, val companions: Set<Companion>? = null)

fun readPrefs(context: Context): Prefs {
    val store = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    return Prefs(AppMode.entries.firstOrNull { it.name == store.getString("mode", null) } ?: AppMode.LIVE,
        DirectionConvention.entries.firstOrNull { it.name == store.getString("convention", null) } ?: DirectionConvention.APPROACH,
        store.getBoolean("disclaimerSeen", false), store.getBoolean("travelMode", false),
        if (!store.contains("companions")) null else store.getStringSet("companions", emptySet()).orEmpty()
            .mapNotNull { name -> Companion.entries.firstOrNull { it.name == name } }.toSet())
}

fun writePrefs(context: Context, prefs: Prefs) {
    context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit()
        .putString("mode", prefs.mode.name).putString("convention", prefs.convention.name)
        .putBoolean("disclaimerSeen", prefs.disclaimerSeen).putBoolean("travelMode", prefs.travelMode)
        .apply { if (prefs.companions == null) remove("companions") else putStringSet("companions", prefs.companions.map { it.name }.toSet()) }
        .apply()
}

@Composable
fun Settings(prefs: Prefs, onChange: (Prefs) -> Unit, onClose: () -> Unit, onChooseCompanions: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF111418)).verticalScroll(rememberScrollState()).padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
      Column(Modifier.width(1080.dp)) {
        Text("설정", style = MaterialTheme.typography.headlineMedium)
        Text("모드")
        AppMode.entries.forEach { mode ->
            Choice(mode.label, prefs.mode == mode) { onChange(prefs.copy(mode = mode)) }
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 112.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("여행 모드", fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text("주차와 근처 행사·명소를 안내해요.", Modifier.padding(top = 8.dp), fontSize = 24.sp, lineHeight = 36.sp, color = TravelSecondary)
            }
            Box(Modifier.size(120.dp, 80.dp).toggleable(prefs.travelMode, role = Role.Switch,
                onValueChange = { if (it) onChooseCompanions(true) else onChange(prefs.copy(travelMode = false)) }),
                contentAlignment = Alignment.Center) {
                Box(Modifier.size(88.dp, 52.dp).background(if (prefs.travelMode) TravelBlue else Color(0xFF687383), CircleShape).padding(5.dp),
                    contentAlignment = if (prefs.travelMode) Alignment.CenterEnd else Alignment.CenterStart) {
                    Box(Modifier.size(42.dp).background(Color(0xFF0A2342), CircleShape))
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 20.dp, bottom = 24.dp), color = Color(0xFF414A56))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (prefs.travelMode) "동행" else "지난 동행", fontSize = 28.sp, lineHeight = 40.sp,
                    fontWeight = FontWeight.Medium, color = Color.White)
                Text(if (prefs.companions == null) "아직 선택하지 않았어요" else prefs.companions.toChoices()
                    .sortedBy { it.ordinal }.joinToString(" · ") { it.label() }, Modifier.padding(top = 8.dp),
                    fontSize = 24.sp, lineHeight = 36.sp, color = TravelSecondary)
            }
            if (prefs.travelMode) TravelButton("바꾸기", { onChooseCompanions(false) })
        }
        Spacer(Modifier.height(32.dp))
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
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Text(label, Modifier.padding(start = 12.dp), fontSize = 24.sp)
    }
}
