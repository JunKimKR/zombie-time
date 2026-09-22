package com.zombietime.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zombietime.app.data.RecoveryState
import java.time.LocalDate
import java.util.Locale

fun gardenColor(id: String): Color = when (id) {
    "forest" -> Color(0xFFD7F5EC)
    "moon" -> Color(0xFFE3E1FF)
    else -> Color(0xFFF4E4DD)
}

@Composable
fun RecoveryScreen(
    state: RecoveryState,
    now: Long,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onGarden: (String) -> Unit
) {
    var duration by remember { mutableStateOf(5) }
    val remaining = state.remaining(now)
    val seconds = (remaining + 999) / 1000
    val streak = if (state.lastDate in listOf(LocalDate.now().toString(), LocalDate.now().minusDays(1).toString())) state.streak else 0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("나의 쉼터", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Pastel.Ink)
        Text("스크롤을 멈추고, 나를 돌보는 시간", color = Pastel.InkSoft)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                .background(Brush.verticalGradient(listOf(Color.White, gardenColor(state.garden))))
                .padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (state.active) "지금은 회복 중" else "잠깐 쉬어도 괜찮아요", color = Pastel.Ink, fontWeight = FontWeight.Bold)
            RecoveryCompanion(state.active)
            Text(if (state.active) String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60) else "작은 쉼, 새로운 시작",
                fontSize = if (state.active) 40.sp else 20.sp, color = Pastel.Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (state.active) {
                UsageBar(1f - remaining.toFloat() / (state.durationMinutes * 60_000L), Pastel.Mint, Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Text("폰을 내려놓고 기지개를 켜볼까요?", color = Pastel.InkSoft)
                Spacer(Modifier.height(14.dp))
                GhostButton("이번 쉼 그만하기", onClick = onCancel)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 25).forEach { minutes ->
                        androidx.compose.material3.FilterChip(selected = duration == minutes,
                            onClick = { duration = minutes }, label = { Text("${minutes}분") })
                    }
                }
                PillButton("🌱  ${duration}분 쉬고 씨앗 ${duration / 5}개 받기", Modifier.fillMaxWidth()) { onStart(duration) }
            }
            Spacer(Modifier.height(12.dp))
            Text("스스로 지키는 휴식 타이머예요. 앱 사용을 차단하거나 검사하지 않으며, SNS 사용 기록은 그대로 유지돼요.",
                fontSize = 12.sp, color = Pastel.InkSoft)
        }
        AnimatedVisibility(visible = state.sessions > 0 && !state.active) {
            SoftCard(Modifier.fillMaxWidth(), color = Pastel.MintSoft) {
                SectionTitle("🌿  잘 쉬었어요!")
                Text("누적 ${state.totalMinutes}분의 쉼 · ${state.sessions}회 완료", color = Pastel.Ink)
            }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("나의 회복 기록")
            Spacer(Modifier.height(10.dp))
            Text("🌱 씨앗 ${state.seeds}개    🔥 연속 ${streak}일", color = Pastel.Primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            listOf(1 to "첫 번째 쉼", 5 to "쉬어가는 습관", 20 to "회복 전문가").forEach { (count, title) ->
                Text("${if (state.sessions >= count) "🏅" else "○"} $title · ${state.sessions.coerceAtMost(count)}/$count 회",
                    color = if (state.sessions >= count) Pastel.Ink else Pastel.InkSoft, modifier = Modifier.padding(vertical = 5.dp))
            }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("쉼터 꾸미기")
            Text("휴식으로 모은 씨앗으로 홈과 쉼터 배경을 바꿔요.", fontSize = 12.sp, color = Pastel.InkSoft)
            listOf(Triple("peach", "복숭아 정원", 0), Triple("forest", "초록 숲", 6), Triple("moon", "달빛 정원", 12)).forEach { (id, name, cost) ->
                Spacer(Modifier.height(10.dp))
                PillButton(
                    text = "$name · ${if (state.garden == id) "사용 중" else if (id in state.unlocked) "적용" else "씨앗 ${cost}개"}",
                    modifier = Modifier.fillMaxWidth(),
                    background = Brush.horizontalGradient(listOf(gardenColor(id), gardenColor(id))),
                    textColor = Pastel.Ink,
                    enabled = id in state.unlocked || state.seeds >= cost
                ) { onGarden(id) }
            }
            Spacer(Modifier.height(10.dp))
            Text("씨앗은 이 기기에 저장되는 무료 활동 보상이에요. 현금 구매나 환전은 지원하지 않아요.", fontSize = 12.sp, color = Pastel.InkSoft)
        }
    }
}

@Composable
private fun RecoveryCompanion(active: Boolean) {
    val breath = if (active) {
        val transition = rememberInfiniteTransition(label = "breathing")
        val value by transition.animateFloat(0.7f, 1f,
            infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "breathing halo")
        value
    } else 0.7f
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Pastel.Mint.copy(alpha = 0.12f), radius = size.minDimension * 0.48f * breath)
            drawCircle(Color.White.copy(alpha = 0.55f), radius = size.minDimension * 0.38f * breath)
        }
        CharacterCanvas(0f, Modifier.size(170.dp))
    }
}
