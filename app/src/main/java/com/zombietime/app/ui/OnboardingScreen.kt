package com.zombietime.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun OnboardingScreen(
    hasUsagePermission: Boolean,
    hasNotifPermission: Boolean,
    onGrantUsage: () -> Unit,
    onGrantNotif: () -> Unit,
    onStart: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "demo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "demoProgress"
    )
    // 0 → 1 → 0 으로 왔다갔다 (사람 ↔ 좀비 데모)
    val demo = 1f - abs(1f - t)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Pastel.BgAlt, Pastel.Bg))
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Text(
            "좀비타임",
            color = Pastel.Ink,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "SNS를 볼수록\n귀여운 내가 좀비가 돼요",
            color = Pastel.InkSoft,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(10.dp))

        Box(
            Modifier
                .fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White.copy(alpha = 0.7f))
        ) {
            CharacterCanvas(
                progress = demo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        SoftCard(Modifier.fillMaxWidth()) {
            InfoRow("📸", "인스타 · 스레드 · 페북 · 유튜브", "네 앱의 화면에 떠 있던 시간만 합산해요")
            Spacer(Modifier.height(14.dp))
            InfoRow("🔔", "상시 배너", "알림창에 캐릭터 상태와 사용시간이 항상 보여요")
            Spacer(Modifier.height(14.dp))
            InfoRow("📮", "하루 브리핑", "밤이 되면 오늘의 리포트 카드를 만들어 스토리에 공유")
            Spacer(Modifier.height(14.dp))
            InfoRow("🔒", "전부 폰 안에서만", "기록은 어디에도 전송되지 않아요")
        }

        Spacer(Modifier.height(16.dp))

        SoftCard(Modifier.fillMaxWidth()) {
            SectionTitle("시작하려면 두 가지만 켜주세요")
            Spacer(Modifier.height(14.dp))

            StepRow(
                index = 1,
                title = "사용 정보 접근 허용",
                desc = "설정 화면에서 '좀비타임'을 찾아 켜주세요",
                done = hasUsagePermission,
                actionLabel = "설정 열기",
                onAction = onGrantUsage
            )
            Spacer(Modifier.height(14.dp))
            StepRow(
                index = 2,
                title = "알림 허용",
                desc = "상시 배너를 띄우려면 필요해요",
                done = hasNotifPermission,
                actionLabel = "허용하기",
                onAction = onGrantNotif
            )
        }

        Spacer(Modifier.height(18.dp))

        PillButton(
            text = if (hasUsagePermission) "시작하기" else "권한을 먼저 켜주세요",
            modifier = Modifier.fillMaxWidth(),
            enabled = hasUsagePermission,
            onClick = onStart
        )

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun InfoRow(emoji: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Pastel.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(desc, color = Pastel.InkSoft, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StepRow(
    index: Int,
    title: String,
    desc: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (done) Pastel.MintSoft else Pastel.PrimarySoft)
                .padding(horizontal = 11.dp, vertical = 7.dp)
        ) {
            Text(
                if (done) "✓" else index.toString(),
                color = if (done) Pastel.Mint else Pastel.Primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Pastel.Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(desc, color = Pastel.InkSoft, fontSize = 12.sp)
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            GhostButton(actionLabel, onClick = onAction)
        }
    }
}
