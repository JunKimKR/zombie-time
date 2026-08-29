package com.zombietime.app.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.zombietime.app.character.CharacterRenderer
import com.zombietime.app.data.DayUsage
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.TimeFmt
import com.zombietime.app.data.TrackedApps
import com.zombietime.app.data.ZombieStages
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 인스타 스토리용 1080x1920 브리핑 카드 이미지 생성 */
object StoryImageBuilder {

    private const val W = 1080
    private const val H = 1920

    private const val INK = 0xFF443C5E.toInt()
    private const val INK_SOFT = 0xFF8B82A6.toInt()

    fun build(ctx: Context, usage: DayUsage): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        val goalMs = Prefs.goalMs(ctx)
        val progress = ZombieStages.progress(usage.totalMs, goalMs)
        val stage = ZombieStages.stage(ZombieStages.stageIndex(progress))

        drawBackground(c, progress)
        drawCard(c)

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textAlign = Paint.Align.CENTER

        // 제목
        p.color = INK
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 70f
        c.drawText("오늘의 좀비 리포트", W / 2f, 268f, p)

        // 날짜
        p.typeface = Typeface.DEFAULT
        p.textSize = 40f
        p.color = INK_SOFT
        c.drawText(dateLabel(usage.date), W / 2f, 330f, p)

        // 캐릭터
        CharacterRenderer.draw(
            c,
            RectF(320f, 380f, 760f, 820f),
            progress
        )

        // 단계 칩
        drawStageChip(c, stage.emoji + "  " + stage.title, progress)

        // 총 사용시간
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT
        p.textSize = 40f
        p.color = INK_SOFT
        c.drawText("오늘 SNS 사용시간", W / 2f, 1032f, p)

        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 108f
        p.color = INK
        c.drawText(TimeFmt.human(usage.totalMs), W / 2f, 1148f, p)

        // 진행 바
        drawProgressBar(c, progress)

        p.typeface = Typeface.DEFAULT
        p.textSize = 36f
        p.color = INK_SOFT
        val pct = (progress * 100f).toInt()
        c.drawText(
            "목표 ${TimeFmt.human(goalMs)} 중 ${pct}% 사용",
            W / 2f,
            1276f,
            p
        )

        // 구분선
        p.color = 0xFFF1EDF9.toInt()
        c.drawRect(120f, 1328f, 960f, 1331f, p)

        // 앱별 상세
        drawAppRows(c, usage)

        // 한 줄 코멘트
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.DEFAULT
        p.textSize = 38f
        p.color = INK_SOFT
        c.drawText(stage.quote, W / 2f, 1704f, p)

        // 푸터
        p.textSize = 34f
        p.color = 0xFFB4ABCB.toInt()
        c.drawText("🧟  좀비타임 · ZombieTime", W / 2f, 1758f, p)

        return bmp
    }

    private fun dateLabel(date: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).parse(date) ?: Date()
            val c = Calendar.getInstance()
            c.time = parsed
            val days = arrayOf("일", "월", "화", "수", "목", "금", "토")
            val dow = days[c.get(Calendar.DAY_OF_WEEK) - 1]
            "${c.get(Calendar.YEAR)}. ${c.get(Calendar.MONTH) + 1}. ${c.get(Calendar.DAY_OF_MONTH)} ($dow)"
        } catch (e: Exception) {
            date
        }
    }

    private fun drawBackground(c: Canvas, progress: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // 진행도에 따라 배경도 살짝 초록빛으로
        val topColor = lerpColor(0xFFFFE1EC.toInt(), 0xFFDCEFD8.toInt(), progress)
        val midColor = lerpColor(0xFFEDE2FF.toInt(), 0xFFDDEFE6.toInt(), progress)
        val botColor = lerpColor(0xFFD9F0EC.toInt(), 0xFFCFE6D2.toInt(), progress)

        p.shader = LinearGradient(
            0f, 0f, 0f, H.toFloat(),
            intArrayOf(topColor, midColor, botColor),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), p)
        p.shader = null

        // 몽글몽글 장식
        p.color = 0x33FFFFFF
        c.drawCircle(120f, 210f, 170f, p)
        c.drawCircle(980f, 470f, 130f, p)
        c.drawCircle(150f, 1650f, 200f, p)
        c.drawCircle(960f, 1780f, 150f, p)
    }

    private fun drawCard(c: Canvas) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0x14000000
        c.drawRoundRect(RectF(56f, 156f, 1024f, 1800f), 64f, 64f, p)
        p.color = 0xF2FFFFFF.toInt()
        c.drawRoundRect(RectF(50f, 148f, 1030f, 1792f), 64f, 64f, p)
    }

    private fun drawStageChip(c: Canvas, label: String, progress: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.textAlign = Paint.Align.CENTER
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textSize = 44f

        val textW = p.measureText(label)
        val chipW = textW + 96f
        val cx = W / 2f
        val top = 862f
        val bottom = 946f

        p.color = stageColor(progress)
        c.drawRoundRect(RectF(cx - chipW / 2f, top, cx + chipW / 2f, bottom), 42f, 42f, p)

        p.color = Color.WHITE
        c.drawText(label, cx, bottom - 28f, p)
    }

    private fun drawProgressBar(c: Canvas, progress: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val left = 150f
        val right = 930f
        val top = 1190f
        val bottom = 1230f

        p.color = 0xFFEFEAF8.toInt()
        c.drawRoundRect(RectF(left, top, right, bottom), 20f, 20f, p)

        val fillRight = left + (right - left) * progress.coerceIn(0.02f, 1f)
        p.shader = LinearGradient(
            left, 0f, right, 0f,
            intArrayOf(0xFFFFA6C1.toInt(), 0xFF9B8BEA.toInt(), 0xFF6FBF83.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRoundRect(RectF(left, top, fillRight, bottom), 20f, 20f, p)
        p.shader = null
    }

    private fun drawAppRows(c: Canvas, usage: DayUsage) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        var maxMs = 1L
        for (app in TrackedApps.ALL) {
            if (usage.ms(app.pkg) > maxMs) maxMs = usage.ms(app.pkg)
        }

        var y = 1382f
        for (app in TrackedApps.ALL) {
            val ms = usage.ms(app.pkg)

            // 이름
            p.textAlign = Paint.Align.LEFT
            p.typeface = Typeface.DEFAULT
            p.textSize = 38f
            p.color = INK
            c.drawText("${app.emoji}  ${app.label}", 120f, y + 12f, p)

            // 막대
            val barLeft = 400f
            val barRight = 700f
            p.color = 0xFFF1EDF9.toInt()
            c.drawRoundRect(RectF(barLeft, y - 22f, barRight, y + 12f), 17f, 17f, p)

            val ratio = if (maxMs > 0) ms.toFloat() / maxMs.toFloat() else 0f
            if (ms > 0) {
                p.color = app.color
                val w = (barRight - barLeft) * ratio
                c.drawRoundRect(
                    RectF(barLeft, y - 22f, barLeft + w.coerceAtLeast(34f), y + 12f),
                    17f, 17f, p
                )
            }

            // 시간
            p.textAlign = Paint.Align.RIGHT
            p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            p.textSize = 34f
            p.color = if (ms > 0) INK else 0xFFC3BCD6.toInt()
            c.drawText(if (ms > 0) TimeFmt.short(ms) else "0분", 960f, y + 10f, p)

            y += 76f
        }
    }

    private fun stageColor(progress: Float): Int = when {
        progress < 0.30f -> 0xFF6FC5A6.toInt()
        progress < 0.50f -> 0xFFE8B65C.toInt()
        progress < 0.72f -> 0xFFE89A6B.toInt()
        progress < 0.92f -> 0xFF8FBF6B.toInt()
        else -> 0xFF5E9E62.toInt()
    }

    private fun lerpColor(c1: Int, c2: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        return Color.argb(
            255,
            (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * tt).toInt(),
            (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * tt).toInt(),
            (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * tt).toInt()
        )
    }

    /** 비트맵을 캐시에 저장하고 공유 가능한 content:// URI 로 돌려준다 */
    fun saveForShare(ctx: Context, bmp: Bitmap): Uri? {
        return try {
            val dir = File(ctx.cacheDir, "shared")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "zombie_briefing.png")
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
        } catch (e: Exception) {
            null
        }
    }
}
