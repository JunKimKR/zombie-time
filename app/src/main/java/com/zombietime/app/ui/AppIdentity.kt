package com.zombietime.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.zombietime.app.R
import com.zombietime.app.data.TrackedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIdentity(app: TrackedApp, size: Dp = 40.dp) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, app.pkg) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(app.pkg).toBitmap(144, 144).asImageBitmap() }.getOrNull()
        }
    }
    if (icon != null) {
        Image(icon!!, app.label, Modifier.size(size).clip(RoundedCornerShape(12.dp)))
    } else {
        val (drawable, colors) = when (app.pkg) {
            "com.instagram.android" -> R.drawable.brand_instagram to listOf(Color(0xFF833AB4), Color(0xFFE1306C), Color(0xFFFCAF45))
            "com.instagram.barcelona" -> R.drawable.brand_threads to listOf(Color.Black, Color.Black)
            "com.facebook.katana" -> R.drawable.brand_facebook_f to listOf(Color(0xFF0866FF), Color(0xFF0866FF))
            else -> R.drawable.brand_youtube to listOf(Color(0xFFFF0033), Color(0xFFFF0033))
        }
        Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
            Image(painterResource(drawable), app.label, Modifier.size(size).padding(9.dp))
        }
    }
}
