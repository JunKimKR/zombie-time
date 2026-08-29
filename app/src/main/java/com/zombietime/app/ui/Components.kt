package com.zombietime.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zombietime.app.character.CharacterRenderer

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = Pastel.Card,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(color)
            .padding(padding),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Pastel.Ink,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun PillButton(
    text: String,
    modifier: Modifier = Modifier,
    background: Brush = Brush.horizontalGradient(listOf(Pastel.Pink, Pastel.Primary)),
    textColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(if (enabled) background else Brush.horizontalGradient(listOf(Pastel.InkFaint, Pastel.InkFaint)))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 15.dp, horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Pastel.PrimarySoft)
            .clickable { onClick() }
            .padding(vertical = 15.dp, horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Pastel.Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** 캐릭터만 그리는 캔버스 (둥실둥실 애니메이션 포함) */
@Composable
fun CharacterCanvas(
    progress: Float,
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "bob")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            CharacterRenderer.draw(
                canvas.nativeCanvas,
                android.graphics.RectF(0f, 0f, size.width, size.height),
                progress,
                if (animate) phase else 0f
            )
        }
    }
}

/** 진행 링 + 캐릭터 */
@Composable
fun ZombieHero(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900),
        label = "progress"
    )
    val ringColor = stageColor(progress)

    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.045f
            val inset = stroke / 2f + size.minDimension * 0.01f
            drawArc(
                color = Pastel.Line,
                startAngle = 130f,
                sweepAngle = 280f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    size.width - inset * 2,
                    size.height - inset * 2
                ),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Pastel.Pink, Pastel.Lemon, ringColor, ringColor)
                ),
                startAngle = 130f,
                sweepAngle = 280f * animated.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    size.width - inset * 2,
                    size.height - inset * 2
                ),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        CharacterCanvas(
            progress = progress,
            modifier = Modifier
                .fillMaxSize()
                .padding(26.dp)
        )
    }
}

/** 가로 막대 (앱별 사용시간) */
@Composable
fun UsageBar(
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp
) {
    val animated by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "bar"
    )
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Pastel.Line)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(if (animated <= 0f) 0f else animated.coerceAtLeast(0.06f))
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun BottomTabs(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf("🏠  홈", "📅  주간", "⚙️  설정")
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { index, label ->
            TabItem(
                label = label,
                selected = selected == index,
                modifier = Modifier.weight(1f)
            ) { onSelect(index) }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Pastel.PrimarySoft else Color.Transparent)
            .clickable { onClick() }
            .padding(PaddingValues(vertical = 12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) Pastel.Primary else Pastel.InkSoft,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
