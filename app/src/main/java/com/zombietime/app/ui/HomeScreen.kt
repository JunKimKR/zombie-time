package com.zombietime.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zombietime.app.data.DayUsage
import com.zombietime.app.data.TimeFmt
import com.zombietime.app.data.TrackedApps
import com.zombietime.app.data.ZombieStages
import java.util.Calendar

@Composable
fun HomeScreen(
    usage: DayUsage,
    goalMinutes: Int,
    hasUsagePermission: Boolean,
    monitorOn: Boolean,
    onRequestUsagePermission: () -> Unit,
    onShareBriefing: () -> Unit
) {
    val goalMs = goalMinutes * 60_000L
    val progress = ZombieStages.progress(usage.totalMs, goalMs)
    val stage = ZombieStages.stage(ZombieStages.stageIndex(progress))
    val remain = (goalMs - usage.totalMs).coerceAtLeast(0L)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Header()

        if (!hasUsagePermission) {
            PermissionCard(onRequestUsagePermission)
        }

        // ---------------------------------------------------------- 캐릭터 카드
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, stageColor(progress).copy(alpha = 0.13f))
                    )
                )
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ZombieHero(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                )

                Spacer(Modifier.height(4.dp))

                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(stageColor(progress))
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        "${stage.emoji}  ${stage.title}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    stage.quote,
                    color = Pastel.InkSoft,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ------------------------------------------------------------ 요약 수치
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBox(
                modifier = Modifier.weight(1f),
                label = "오늘 SNS",
                value = TimeFmt.human(usage.totalMs),
                accent = Pastel.Primary
            )
            StatBox(
                modifier = Modifier.weight(1f),
                label = if (remain > 0) "좀비까지" else "목표 초과",
                value = if (remain > 0) TimeFmt.human(remain) else "+" + TimeFmt.human(usage.totalMs - goalMs),
                accent = if (remain > 0) Pastel.Mint else Pastel.Pink
            )
        }

        // ------------------------------------------------------------- 앱별 상세
        SoftCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("앱별 사용시간")
                Text(
                    "목표 ${TimeFmt.human(goalMs)}",
                    color = Pastel.InkFaint,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(14.dp))

            var maxMs = 1L
            for (app in TrackedApps.ALL) {
                if (usage.ms(app.pkg) > maxMs) maxMs = usage.ms(app.pkg)
            }

            TrackedApps.ALL.forEachIndexed { i, app ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                val ms = usage.ms(app.pkg)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.emoji, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        app.label,
                        color = Pastel.Ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(76.dp)
                    )
                    UsageBar(
                        ratio = if (maxMs > 0) ms.toFloat() / maxMs.toFloat() else 0f,
                        color = Color(app.color),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (ms > 0) TimeFmt.short(ms) else "0분",
                        color = if (ms > 0) Pastel.Ink else Pastel.InkFaint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(62.dp)
                    )
                }
            }
        }

        // ------------------------------------------------------------- 공유 버튼
        PillButton(
            text = "📮  오늘의 브리핑 카드 만들기",
            modifier = Modifier.fillMaxWidth(),
            onClick = onShareBriefing
        )

        if (!monitorOn) {
            SoftCard(Modifier.fillMaxWidth(), color = Pastel.PinkSoft) {
                Text(
                    "상시 배너가 꺼져 있어요. 설정에서 켜면 잠금화면에도 캐릭터가 계속 보여요.",
                    color = Pastel.Ink,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Header() {
    val c = Calendar.getInstance()
    val days = arrayOf("일", "월", "화", "수", "목", "금", "토")
    val label = "${c.get(Calendar.MONTH) + 1}월 ${c.get(Calendar.DAY_OF_MONTH)}일 (${days[c.get(Calendar.DAY_OF_WEEK) - 1]})"

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("좀비타임", color = Pastel.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Pastel.InkSoft, fontSize = 13.sp)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(50))
                .background(Pastel.PrimarySoft),
            contentAlignment = Alignment.Center
        ) {
            Text("🧟", fontSize = 20.sp)
        }
    }
}

@Composable
private fun StatBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color
) {
    SoftCard(modifier, padding = 16.dp) {
        Text(label, color = Pastel.InkSoft, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = accent, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    SoftCard(Modifier.fillMaxWidth(), color = Pastel.PinkSoft) {
        Text(
            "⚠️  사용 정보 접근 권한이 필요해요",
            color = Pastel.Ink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "SNS 사용시간을 읽으려면 안드로이드 '사용 정보 접근' 권한을 켜야 해요. 켜기 전까지는 시간이 0분으로 보여요.",
            color = Pastel.InkSoft,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))
        PillButton("권한 켜러 가기", Modifier.fillMaxWidth(), onClick = onRequest)
    }
}
