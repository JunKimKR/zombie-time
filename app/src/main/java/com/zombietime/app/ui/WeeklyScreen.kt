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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zombietime.app.data.DayUsage
import com.zombietime.app.data.TimeFmt
import com.zombietime.app.data.TrackedApps
import com.zombietime.app.data.ZombieStages
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WeeklyScreen(
    week: List<DayUsage>,
    month: List<DayUsage>,
    goalMinutes: Int
) {
    val goalMs = goalMinutes * 60_000L

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "주간 리포트",
            color = Pastel.Ink,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )

        // ------------------------------------------------------------- 요약 3칸
        val total = week.sumOf { it.totalMs }
        val avg = if (week.isEmpty()) 0L else total / week.size
        val humanDays = week.count { ZombieStages.progress(it.totalMs, goalMs) < 0.50f }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniStat(Modifier.weight(1f), "하루 평균", TimeFmt.short(avg), Pastel.Primary)
            MiniStat(Modifier.weight(1f), "7일 합계", TimeFmt.short(total), Pastel.Pink)
            MiniStat(Modifier.weight(1f), "사람으로\n버틴 날", "${humanDays}일", Pastel.Mint)
        }

        // ------------------------------------------------------------ 막대 그래프
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("최근 7일")
            Spacer(Modifier.height(6.dp))
            Text(
                "막대가 위로 갈수록 좀비에 가까워요",
                color = Pastel.InkFaint,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(16.dp))

            var maxMs = goalMs
            for (d in week) if (d.totalMs > maxMs) maxMs = d.totalMs

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                week.forEach { day ->
                    val ratio = if (maxMs > 0) day.totalMs.toFloat() / maxMs.toFloat() else 0f
                    val progress = ZombieStages.progress(day.totalMs, goalMs)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            if (day.totalMs > 0) "${day.totalMs / 60000}" else "",
                            color = Pastel.InkFaint,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((14 + (96 * ratio.coerceIn(0f, 1f))).dp)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(
                                    if (day.totalMs > 0) stageColor(progress) else Pastel.Line
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            weekdayOf(day.date),
                            color = Pastel.InkSoft,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // ----------------------------------------------------------- 좀비 캘린더
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("좀비 캘린더")
            Spacer(Modifier.height(6.dp))
            Text("최근 4주 · 색이 진할수록 좀비였던 날", color = Pastel.InkFaint, fontSize = 11.sp)
            Spacer(Modifier.height(14.dp))

            val rows = month.chunked(7)
            rows.forEachIndexed { rowIndex, rowDays ->
                if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowDays.forEach { day ->
                        val progress = ZombieStages.progress(day.totalMs, goalMs)
                        val stage = ZombieStages.stage(ZombieStages.stageIndex(progress))
                        Box(
                            Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (day.totalMs > 0) stageColor(progress).copy(alpha = 0.85f)
                                    else Pastel.Line
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (day.totalMs > 0) stage.emoji else dayNumberOf(day.date),
                                fontSize = if (day.totalMs > 0) 15.sp else 11.sp,
                                color = Pastel.InkFaint
                            )
                        }
                    }
                    // 마지막 줄이 7칸보다 적으면 빈칸 채우기
                    var pad = 7 - rowDays.size
                    while (pad > 0) {
                        Spacer(Modifier.weight(1f))
                        pad--
                    }
                }
            }
        }

        // --------------------------------------------------------- 앱별 주간 합계
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("이번 주 앱별 합계")
            Spacer(Modifier.height(14.dp))

            val totals = HashMap<String, Long>()
            for (app in TrackedApps.ALL) {
                var sum = 0L
                for (d in week) sum += d.ms(app.pkg)
                totals[app.pkg] = sum
            }
            var maxApp = 1L
            for (v in totals.values) if (v > maxApp) maxApp = v

            TrackedApps.ALL.forEachIndexed { i, app ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                val ms = totals[app.pkg] ?: 0L
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(app.emoji, fontSize = 17.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        app.label,
                        color = Pastel.Ink,
                        fontSize = 13.sp,
                        modifier = Modifier.width(76.dp)
                    )
                    UsageBar(
                        ratio = ms.toFloat() / maxApp.toFloat(),
                        color = Color(app.color),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        TimeFmt.short(ms),
                        color = Pastel.Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(66.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun MiniStat(modifier: Modifier, label: String, value: String, accent: Color) {
    SoftCard(modifier, padding = 14.dp) {
        Text(label, color = Pastel.InkSoft, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = accent, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

private fun parse(date: String): Calendar? = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(date)
    val c = Calendar.getInstance()
    if (d != null) c.time = d
    c
} catch (e: Exception) {
    null
}

private fun weekdayOf(date: String): String {
    val c = parse(date) ?: return ""
    val days = arrayOf("일", "월", "화", "수", "목", "금", "토")
    return days[c.get(Calendar.DAY_OF_WEEK) - 1]
}

private fun dayNumberOf(date: String): String {
    val c = parse(date) ?: return ""
    return c.get(Calendar.DAY_OF_MONTH).toString()
}
