package com.roadahead.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.roadahead.core.model.Companion

enum class CompanionChoice { CHILD, ELDER, PET, NONE }

fun toggleChoice(selected: Set<CompanionChoice>, tapped: CompanionChoice): Set<CompanionChoice> =
    if (tapped == CompanionChoice.NONE) setOf(tapped)
    else (selected - CompanionChoice.NONE).let { if (tapped in it) it - tapped else it + tapped }

fun Set<CompanionChoice>.toCompanions(): Set<Companion> =
    mapNotNull { choice -> Companion.entries.firstOrNull { it.name == choice.name } }.toSet()

fun Set<Companion>?.toChoices(): Set<CompanionChoice> = when {
    this == null -> emptySet()
    isEmpty() -> setOf(CompanionChoice.NONE)
    else -> map { CompanionChoice.valueOf(it.name) }.toSet()
}

internal val TravelBlue = Color(0xFFB8D4FF)
internal val TravelSecondary = Color(0xFFC9D1D9)
internal fun CompanionChoice.label() = when (this) {
    CompanionChoice.CHILD -> "아이"
    CompanionChoice.ELDER -> "어르신"
    CompanionChoice.PET -> "반려동물"
    CompanionChoice.NONE -> "동행 없음"
}

@Composable
fun CompanionPicker(previous: Set<Companion>?, enabling: Boolean,
    onComplete: (Set<Companion>) -> Unit, onCancel: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf(previous.toChoices().map { it.name }) }
    val selected = draft.map { CompanionChoice.valueOf(it) }.toSet()
    Column(Modifier.fillMaxSize().background(Color(0xFF111418)).padding(horizontal = 64.dp, vertical = 48.dp)) {
        Text("앞길 안내", fontSize = 32.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Column(Modifier.padding(top = 102.dp).width(1080.dp).align(Alignment.CenterHorizontally)) {
            Text("누구와 함께 가세요?", fontSize = 36.sp, lineHeight = 52.sp,
                fontWeight = FontWeight.SemiBold, color = Color.White)
            Text("해당하는 동행을 모두 골라 주세요.", Modifier.padding(top = 12.dp),
                fontSize = 24.sp, lineHeight = 36.sp, color = TravelSecondary)
            Text(if (previous == null) "동행에 맞춰 가까운 명소를 추천해 드려요." else "지난 동행을 미리 골랐어요.",
                Modifier.padding(top = 32.dp, bottom = 20.dp), fontSize = 24.sp, lineHeight = 36.sp, color = TravelBlue)
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                CompanionChoice.entries.take(3).forEach { choice ->
                    CompanionOption(choice, choice in selected, Modifier.weight(1f).height(112.dp)) {
                        draft = toggleChoice(selected, choice).map { it.name }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(top = 32.dp, bottom = 24.dp), color = Color(0xFF414A56))
            CompanionOption(CompanionChoice.NONE, CompanionChoice.NONE in selected, Modifier.fillMaxWidth().height(80.dp)) {
                draft = toggleChoice(selected, CompanionChoice.NONE).map { it.name }
            }
            Text("동행 없음은 다른 동행과 함께 선택할 수 없어요.", Modifier.padding(top = 16.dp),
                fontSize = 24.sp, lineHeight = 36.sp, color = TravelSecondary)
            Row(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TravelButton("취소", onCancel)
                TravelButton("완료", { onComplete(selected.toCompanions()) }, primary = true, enabled = selected.isNotEmpty())
            }
            Text(if (enabling) "선택을 마치면 여행 모드가 켜져요." else "완료하면 동행 선택을 기억해요.",
                Modifier.padding(top = 16.dp), fontSize = 24.sp, lineHeight = 36.sp, color = TravelSecondary)
        }
    }
}

@Composable
private fun CompanionOption(choice: CompanionChoice, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val border = if (selected) Color(0xFF8CB8FF) else Color(0xFF687383)
    val choiceModifier = if (choice == CompanionChoice.NONE) modifier.selectable(selected, role = Role.RadioButton, onClick = onClick)
        else modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onClick() })
    Surface(choiceModifier, shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, border),
        color = if (selected) Color(0xFF253B58) else Color(0xFF1B2028)) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            val shape = if (choice == CompanionChoice.NONE) CircleShape else RoundedCornerShape(6.dp)
            Surface(Modifier.size(32.dp), shape = shape, border = BorderStroke(2.dp,
                if (selected) Color(0xFF8CB8FF) else Color(0xFFA7B0BA)),
                color = if (selected) Color(0xFF8CB8FF) else Color.Transparent) {
                Box(contentAlignment = Alignment.Center) {
                    if (selected && choice == CompanionChoice.NONE) Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xFF101923)))
                    else if (selected) Text("✓", fontSize = 24.sp, color = Color(0xFF101923))
                }
            }
            Text(choice.label(), Modifier.padding(start = 20.dp), fontSize = 28.sp, lineHeight = 40.sp, color = Color.White)
        }
    }
}

@Composable
internal fun TravelButton(label: String, onClick: () -> Unit, primary: Boolean = false, enabled: Boolean = true) {
    val modifier = Modifier.width(if (primary) 320.dp else 160.dp).height(80.dp)
    val shape = RoundedCornerShape(40.dp)
    val content: @Composable RowScope.() -> Unit = {
        Text(label, fontSize = 28.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium)
    }
    if (primary) Button(onClick, modifier, enabled = enabled, shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = TravelBlue, contentColor = Color(0xFF0A2342),
            disabledContainerColor = Color(0xFF343D49), disabledContentColor = Color(0xFF8A96A6)), content = content)
    else OutlinedButton(onClick, modifier, shape = shape, border = BorderStroke(2.dp, Color(0xFF687383)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), content = content)
}
