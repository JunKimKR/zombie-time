package com.zombietime.app.ui

import android.animation.ValueAnimator
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zombietime.app.character.CharacterVoice
import com.zombietime.app.data.ZombieStages
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZombieHero(progress: Float, modifier: Modifier = Modifier, garden: String = "peach") {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val prefs = remember(context) { context.getSharedPreferences("character", 0) }
    var sound by remember { mutableStateOf(prefs.getBoolean("sound", true)) }
    val voice = remember(context) { CharacterVoice(context.applicationContext) }
    DisposableEffect(voice, lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.stop() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); voice.release() }
    }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val reaction = remember { Animatable(0f) }
    var kind by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(-1000L) }
    var reactionJob by remember { mutableStateOf<Job?>(null) }
    var speaking by remember { mutableStateOf(false) }
    var stillPose by remember { mutableStateOf(false) }
    val stage = ZombieStages.stage(ZombieStages.stageIndex(progress))
    val reply = listOf("헤헤, 나 불렀어?", "앗! 깜짝이야!", "우리 잠깐 쉬어갈까?")
    val ink = Pastel.Ink

    Column(modifier.clip(RoundedCornerShape(36.dp)).background(gardenColor(garden)).padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stage.title, color = ink, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.75f)).padding(horizontal = 13.dp, vertical = 8.dp))
            TextButton(onClick = {
                sound = !sound
                prefs.edit().putBoolean("sound", sound).apply()
                if (!sound) voice.stop()
            }) { Text(if (sound) "소리 켜짐" else "소리 꺼짐", fontSize = 12.sp, color = ink) }
        }
        AnimatedContent(targetState = if (speaking) reply[kind] else stage.quote, label = "pet speech") { quote ->
            Text(quote, color = ink, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp))
        }
        Box(Modifier.fillMaxWidth().height(248.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawOval(Color.White.copy(alpha = 0.6f), Offset(size.width * .14f, size.height * .75f), Size(size.width * .72f, size.height * .18f))
                drawOval(Pastel.Primary.copy(alpha = 0.08f), Offset(size.width * .24f, size.height * .79f), Size(size.width * .52f, size.height * .095f))
                val t = reaction.value
                if (t > 0f) {
                    repeat(8) { i ->
                        val angle = i * Math.PI / 4
                        val distance = (0.24f + (1f - t) * .17f) * size.minDimension
                        val x = center.x + cos(angle).toFloat() * distance
                        val y = center.y + sin(angle).toFloat() * distance - 10.dp.toPx()
                        val radius = (3f + t * 4f).dp.toPx()
                        val path = Path().apply {
                            moveTo(x, y - radius); lineTo(x + radius * .35f, y - radius * .35f)
                            lineTo(x + radius, y); lineTo(x + radius * .35f, y + radius * .35f)
                            lineTo(x, y + radius); lineTo(x - radius * .35f, y + radius * .35f)
                            lineTo(x - radius, y); lineTo(x - radius * .35f, y - radius * .35f); close()
                        }
                        drawPath(path, (if (i % 2 == 0) Pastel.Primary else Pastel.Pink).copy(alpha = t))
                    }
                }
            }
            CharacterCanvas(progress,
                Modifier.size(226.dp).clip(CircleShape)
                    .semantics { contentDescription = "캐릭터와 놀기. 누르면 표정과 목소리로 반응해요" }
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null,
                        role = Role.Button, onClickLabel = "캐릭터와 놀기") {
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastTap >= 300) {
                            lastTap = now
                            kind = count++ % 3
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (sound) voice.play(kind, progress)
                            reactionJob?.cancel()
                            reactionJob = scope.launch {
                                speaking = true
                                if (ValueAnimator.areAnimatorsEnabled()) {
                                    stillPose = false
                                    reaction.snapTo(1f)
                                    reaction.animateTo(0f, tween(1100, easing = LinearEasing))
                                } else {
                                    reaction.snapTo(0f)
                                    stillPose = true
                                    delay(900)
                                    stillPose = false
                                }
                                delay(900)
                                speaking = false
                            }
                        }
                    }, reaction = if (stillPose) 0.5f else reaction.value, reactionType = kind)
        }
        Text("살짝 터치해서 인사해 주세요", color = ink.copy(alpha = .65f), fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp))
    }
}
