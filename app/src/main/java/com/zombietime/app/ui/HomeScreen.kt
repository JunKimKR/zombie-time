package com.zombietime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zombietime.app.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    usage: DayUsage, goalMinutes: Int, hasUsagePermission: Boolean, monitorOn: Boolean,
    onRequestUsagePermission: () -> Unit, onShareBriefing: () -> Unit,
    garden: String = "peach", onRecovery: () -> Unit = {}
) {
    val goalMs = goalMinutes * 60_000L
    val progress = ZombieStages.progress(usage.totalMs, goalMs)
    val remain = (goalMs - usage.totalMs).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("좀비타임", color = Pastel.Ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                    color = Pastel.InkSoft, fontSize = 12.sp)
            }
            Text("오늘의 나를 돌봐요", color = Pastel.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        if (!hasUsagePermission) {
            SoftCard(Modifier.fillMaxWidth(), color = Pastel.PinkSoft) {
                SectionTitle("사용시간을 연결해 주세요")
                Text("권한을 켜면 SNS 사용시간에 따라 친구의 모습이 바뀌어요.", color = Pastel.InkSoft, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                PillButton("사용 정보 접근 허용", Modifier.fillMaxWidth(), onClick = onRequestUsagePermission)
            }
        }
        ZombieHero(progress, Modifier.fillMaxWidth(), garden)
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("오늘 SNS 사용", color = Pastel.InkSoft, fontSize = 12.sp)
                    Text(if (hasUsagePermission) TimeFmt.human(usage.totalMs) else "연결 전", color = Pastel.Ink,
                        fontSize = 29.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("하루 목표 ${TimeFmt.human(goalMs)}", color = Pastel.InkSoft, fontSize = 12.sp)
                    Text(if (!hasUsagePermission) "권한이 필요해요" else if (remain > 0) "${TimeFmt.human(remain)} 남았어요" else "잠깐 쉬어갈까요?",
                        color = if (remain > 0) Pastel.Mint else Pastel.Pink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            UsageBar(progress, stageColor(progress), Modifier.fillMaxWidth(), height = 8.dp)
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Pastel.MintSoft)
            .clickable(onClick = onRecovery).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, null, tint = Pastel.Mint, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("스크롤 대신, 잠깐의 쉼", color = Pastel.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("5분 쉬고 나만의 정원을 키워요", color = Pastel.InkSoft, fontSize = 12.sp)
            }
            Text("쉬러 가기", color = Pastel.Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        SoftCard(Modifier.fillMaxWidth(), padding = 20.dp) {
            SectionTitle("어디에 시간을 썼을까요?")
            Spacer(Modifier.height(20.dp))
            val maxMs = usage.perApp.values.maxOrNull()?.coerceAtLeast(1L) ?: 1L
            TrackedApps.ALL.forEachIndexed { index, app ->
                if (index > 0) Spacer(Modifier.height(18.dp))
                val ms = usage.ms(app.pkg)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIdentity(app)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(app.label, color = Pastel.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(TimeFmt.short(ms), color = Pastel.InkSoft, fontSize = 13.sp)
                        }
                        UsageBar(ms.toFloat() / maxMs, Color(app.color), Modifier.fillMaxWidth(), height = 5.dp)
                    }
                }
            }
        }
        OutlinedButton(onClick = onShareBriefing, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(16.dp)) { Text("오늘의 브리핑 카드 만들기", fontWeight = FontWeight.SemiBold) }
        if (!monitorOn) Text("상시 알림은 설정에서 켤 수 있어요.", color = Pastel.InkSoft, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
    }
}
