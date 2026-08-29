package com.zombietime.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zombietime.app.data.TimeFmt
import com.zombietime.app.data.ZombieStages

@Composable
fun SettingsScreen(
    goalMinutes: Int,
    onGoalChange: (Int) -> Unit,
    briefingHour: Int,
    onBriefingHourChange: (Int) -> Unit,
    stageAlert: Boolean,
    onStageAlertChange: (Boolean) -> Unit,
    monitorOn: Boolean,
    onMonitorChange: (Boolean) -> Unit,
    hasUsagePermission: Boolean,
    onOpenUsageSettings: () -> Unit,
    hasNotifPermission: Boolean,
    onRequestNotif: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "설정",
            color = Pastel.Ink,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )

        // ------------------------------------------------------------ 목표 시간
        SoftCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("완전 좀비가 되는 시간")
                Text(
                    TimeFmt.human(goalMinutes * 60_000L),
                    color = Pastel.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "하루 SNS 사용시간이 이만큼 쌓이면 캐릭터가 완전 좀비가 돼요.",
                color = Pastel.InkSoft,
                fontSize = 12.sp
            )
            Slider(
                value = goalMinutes.toFloat(),
                onValueChange = { onGoalChange(it.toInt()) },
                valueRange = 60f..480f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = Pastel.Primary,
                    activeTrackColor = Pastel.Primary,
                    inactiveTrackColor = Pastel.Line
                )
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1시간", color = Pastel.InkFaint, fontSize = 11.sp)
                Text("8시간", color = Pastel.InkFaint, fontSize = 11.sp)
            }
        }

        // ---------------------------------------------------------- 알림 스위치
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("알림")
            Spacer(Modifier.height(10.dp))
            SwitchRow(
                title = "상시 배너 보여주기",
                desc = "잠금화면과 알림창에 캐릭터 현황을 항상 표시",
                checked = monitorOn,
                onChange = onMonitorChange
            )
            Spacer(Modifier.height(14.dp))
            SwitchRow(
                title = "단계가 바뀔 때 알려주기",
                desc = "인간 → 좀비 단계가 넘어갈 때마다 알림",
                checked = stageAlert,
                onChange = onStageAlertChange
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "하루 브리핑 시각",
                        color = Pastel.Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "이 시각에 오늘의 리포트 카드를 보내줘요",
                        color = Pastel.InkSoft,
                        fontSize = 12.sp
                    )
                }
                Text(
                    "${briefingHour}시",
                    color = Pastel.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = briefingHour.toFloat(),
                onValueChange = { onBriefingHourChange(it.toInt()) },
                valueRange = 18f..23f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = Pastel.Pink,
                    activeTrackColor = Pastel.Pink,
                    inactiveTrackColor = Pastel.Line
                )
            )
        }

        // ------------------------------------------------------------ 권한 상태
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("권한")
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                title = "사용 정보 접근",
                granted = hasUsagePermission,
                actionLabel = "설정 열기",
                onAction = onOpenUsageSettings
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                title = "알림 표시",
                granted = hasNotifPermission,
                actionLabel = "허용하기",
                onAction = onRequestNotif
            )
        }

        // -------------------------------------------------------- 단계 미리보기
        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("좀비화 단계")
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ZombieStages.ALL.forEach { stage ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CharacterCanvas(
                            progress = stage.sample,
                            animate = false,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stage.title,
                            color = Pastel.InkSoft,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        SoftCard(Modifier.fillMaxWidth()) {
            Text("좀비타임 v1.0", color = Pastel.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "인스타그램 · 스레드 · 페이스북 · 유튜브의 화면에 실제로 떠 있던 시간만 더해요. " +
                    "모든 기록은 이 폰 안에만 저장되고 어디에도 전송되지 않아요.",
                color = Pastel.InkSoft,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Pastel.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = Pastel.InkSoft, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Pastel.Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Pastel.InkFaint
            )
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (granted) "✅" else "⚠️", fontSize = 16.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Pastel.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                if (granted) "허용됨" else "허용 필요",
                color = if (granted) Pastel.Mint else Pastel.Pink,
                fontSize = 12.sp
            )
        }
        if (!granted) {
            GhostButton(actionLabel, onClick = onAction)
        }
    }
}
