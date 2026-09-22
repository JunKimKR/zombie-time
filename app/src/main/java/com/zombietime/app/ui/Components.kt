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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import kotlinx.coroutines.launch
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
    background: Brush = Brush.horizontalGradient(listOf(Pastel.Primary, Pastel.Primary)),
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
    animate: Boolean = true,
    reaction: Float = 0f,
    reactionType: Int = 0
) {
    val phase = if (animate) characterPhase() else 0f
    val morph by animateFloatAsState(progress, tween(900), label = "morph")
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            CharacterRenderer.draw(canvas.nativeCanvas,
                android.graphics.RectF(0f, 0f, size.width, size.height), morph, phase, reaction, reactionType)
        }
    }
}

@Composable
private fun characterPhase(): Float {
    val transition = rememberInfiniteTransition(label = "bob")
    val phase by transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    return phase
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
fun BottomTabs(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val icons = listOf(Icons.Default.Home, Icons.Default.DateRange, Icons.Default.Favorite, Icons.Default.Settings)
    val labels = listOf("오늘", "기록", "쉼터", "설정")
    NavigationBar(modifier.clip(RoundedCornerShape(28.dp)), containerColor = Color.White, tonalElevation = 0.dp) {
        labels.forEachIndexed { index, label ->
            NavigationBarItem(selected = selected == index, onClick = { onSelect(index) },
                icon = { Icon(icons[index], contentDescription = null) },
                label = { Text(label, fontWeight = if (selected == index) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Pastel.Primary,
                    selectedTextColor = Pastel.Primary, indicatorColor = Pastel.PrimarySoft,
                    unselectedIconColor = Pastel.InkSoft, unselectedTextColor = Pastel.InkSoft))
        }
    }
}
