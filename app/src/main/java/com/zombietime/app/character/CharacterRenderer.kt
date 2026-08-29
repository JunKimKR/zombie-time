package com.zombietime.app.character

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * 인간 → 좀비로 "연속적으로" 변해가는 캐릭터를 android.graphics 캔버스에 직접 그린다.
 * 화면(Compose), 알림 아이콘, 인스타 스토리 이미지가 모두 이 렌더러 하나를 공유한다.
 *
 * p = 0f  : 완전 인간 (뽀얀 피부, 반짝이는 눈, 미소)
 * p = 1f  : 완전 좀비 (초록 피부, X 눈, 이빨, 꿰맨 자국, 팔 들기)
 */
object CharacterRenderer {

    /** 내부 좌표계 한 변 크기. 캐릭터는 이 정사각형 안에 그려진다. */
    const val UNIT = 200f

    private const val OUTLINE = 0xFF4A4160.toInt()
    private const val BLUSH = 0xFFFF8CA6.toInt()
    private const val EYE_DARK = 0xFF39304F.toInt()

    private val skinStops = intArrayOf(
        0xFFFFD2B4.toInt(), // 사람 살색
        0xFFEFE0B0.toInt(), // 창백
        0xFFA8DC92.toInt(), // 연두빛
        0xFF68B06B.toInt()  // 좀비 초록
    )
    private val skinPos = floatArrayOf(0f, 0.36f, 0.68f, 1f)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val tmpRect = RectF()
    private val tmpPath = Path()

    // ---------------------------------------------------------------- helpers

    private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

    /** a~b 구간에서 0→1로 부드럽게 올라가는 값 */
    private fun ramp(a: Float, b: Float, x: Float): Float {
        if (b <= a) return if (x >= b) 1f else 0f
        val t = clamp01((x - a) / (b - a))
        return t * t * (3f - 2f * t)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    private fun lerpColor(c1: Int, c2: Int, t: Float): Int {
        val tt = clamp01(t)
        return Color.argb(
            (Color.alpha(c1) + (Color.alpha(c2) - Color.alpha(c1)) * tt).toInt(),
            (Color.red(c1) + (Color.red(c2) - Color.red(c1)) * tt).toInt(),
            (Color.green(c1) + (Color.green(c2) - Color.green(c1)) * tt).toInt(),
            (Color.blue(c1) + (Color.blue(c2) - Color.blue(c1)) * tt).toInt()
        )
    }

    private fun sampleGradient(colors: IntArray, pos: FloatArray, t: Float): Int {
        val x = clamp01(t)
        if (x <= pos[0]) return colors[0]
        for (i in 0 until colors.size - 1) {
            if (x <= pos[i + 1]) {
                val local = (x - pos[i]) / (pos[i + 1] - pos[i])
                return lerpColor(colors[i], colors[i + 1], local)
            }
        }
        return colors[colors.size - 1]
    }

    /** 진행도에 맞는 피부색 */
    fun skinColor(p: Float): Int = sampleGradient(skinStops, skinPos, p)

    private fun darken(c: Int, f: Float): Int = Color.argb(
        Color.alpha(c),
        (Color.red(c) * f).toInt().coerceIn(0, 255),
        (Color.green(c) * f).toInt().coerceIn(0, 255),
        (Color.blue(c) * f).toInt().coerceIn(0, 255)
    )

    private fun withAlpha(c: Int, a: Float): Int =
        Color.argb((255 * clamp01(a)).toInt(), Color.red(c), Color.green(c), Color.blue(c))

    // ------------------------------------------------------------------ entry

    /**
     * 지정한 사각형 안에 캐릭터를 꽉 차게 그린다.
     * @param p 0f(사람) ~ 1f(좀비)
     * @param bobPhase 0~1. 살짝 위아래로 떠다니는 애니메이션 위상.
     */
    @Synchronized
    fun draw(canvas: Canvas, bounds: RectF, p: Float, bobPhase: Float = 0f) {
        val s = min(bounds.width(), bounds.height()) / UNIT
        canvas.save()
        canvas.translate(
            bounds.centerX() - UNIT * s / 2f,
            bounds.centerY() - UNIT * s / 2f
        )
        canvas.scale(s, s)
        drawUnit(canvas, clamp01(p), bobPhase)
        canvas.restore()
    }

    /** 캐릭터만 담긴 투명 배경 비트맵 (알림 큰 아이콘 등) */
    fun renderBitmap(sizePx: Int, p: Float): Bitmap {
        val size = max(24, sizePx)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        draw(c, RectF(0f, 0f, size.toFloat(), size.toFloat()), p)
        return bmp
    }

    // ------------------------------------------------------------- 실제 드로잉

    private fun drawUnit(canvas: Canvas, p: Float, bobPhase: Float) {
        val skin = skinColor(p)
        val skinShade = darken(skin, 0.90f)
        val zombie = ramp(0.45f, 1f, p)          // 좀비스러움 정도
        val tired = ramp(0.05f, 0.50f, p)        // 피곤함
        val bob = kotlin.math.sin(bobPhase * 6.2831853f) * 2.2f

        canvas.save()
        canvas.translate(0f, bob)

        // 좀비가 될수록 고개를 살짝 기울인다
        val tilt = lerp(0f, 7f, zombie)
        canvas.save()
        canvas.rotate(tilt, 100f, 150f)

        drawShadow(canvas)
        drawArms(canvas, skinShade, ramp(0.50f, 1.0f, p))
        drawBody(canvas, skin, skinShade, p, zombie)
        drawHead(canvas, skin, skinShade, p, zombie)
        drawFace(canvas, skin, p, zombie, tired)
        drawExtras(canvas, p, zombie)

        canvas.restore()
        canvas.restore()
    }

    private fun drawShadow(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = 0x22000000
        tmpRect.set(58f, 178f, 142f, 194f)
        canvas.drawOval(tmpRect, paint)
    }

    private fun drawArms(canvas: Canvas, shade: Int, raise: Float) {
        // 좀비가 될수록 팔이 위로 들린다
        strokePaint.strokeWidth = 15f
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = shade
        drawArm(canvas, 72f, 128f, -1f, raise)
        drawArm(canvas, 128f, 128f, 1f, raise)
    }

    private fun drawArm(canvas: Canvas, sx: Float, sy: Float, dir: Float, raise: Float) {
        val ex = sx + dir * lerp(15f, 34f, raise)
        val ey = sy + lerp(40f, -24f, raise)
        val cx = sx + dir * lerp(23f, 31f, raise)
        val cy = sy + lerp(21f, 3f, raise)

        tmpPath.reset()
        tmpPath.moveTo(sx, sy)
        tmpPath.quadTo(cx, cy, ex, ey)
        canvas.drawPath(tmpPath, strokePaint)

        paint.style = Paint.Style.FILL
        paint.color = strokePaint.color
        canvas.drawCircle(ex, ey, 9.5f, paint)
    }

    private fun drawBody(canvas: Canvas, skin: Int, shade: Int, p: Float, zombie: Float) {
        paint.style = Paint.Style.FILL

        // 발
        paint.color = shade
        tmpRect.set(66f, 168f, 96f, 184f)
        canvas.drawOval(tmpRect, paint)
        tmpRect.set(104f, 168f, 134f, 184f)
        canvas.drawOval(tmpRect, paint)

        // 몸통 (둥근 캡슐)
        paint.color = skin
        tmpRect.set(62f, 112f, 138f, 178f)
        canvas.drawRoundRect(tmpRect, 34f, 34f, paint)

        // 배 하이라이트
        paint.color = withAlpha(Color.WHITE, 0.22f)
        tmpRect.set(74f, 124f, 112f, 158f)
        canvas.drawOval(tmpRect, paint)

        // 좀비 얼룩 (몸통)
        if (zombie > 0.05f) {
            paint.color = withAlpha(darken(skin, 0.80f), 0.55f * zombie)
            tmpRect.set(104f, 136f, 128f, 156f)
            canvas.drawOval(tmpRect, paint)
        }
    }

    private fun drawHead(canvas: Canvas, skin: Int, shade: Int, p: Float, zombie: Float) {
        paint.style = Paint.Style.FILL

        // 머리
        paint.color = skin
        canvas.drawCircle(100f, 82f, 54f, paint)

        // 위쪽 광택
        paint.color = withAlpha(Color.WHITE, 0.28f)
        tmpRect.set(70f, 38f, 118f, 66f)
        canvas.drawOval(tmpRect, paint)

        // 머리 위 삐죽 머리카락
        strokePaint.strokeWidth = 7f
        strokePaint.color = darken(skin, 0.70f)
        tmpPath.reset()
        tmpPath.moveTo(96f, 30f)
        tmpPath.quadTo(108f, 16f, 120f, 26f)
        canvas.drawPath(tmpPath, strokePaint)

        // 좀비 얼룩 (얼굴)
        if (zombie > 0.05f) {
            paint.color = withAlpha(darken(skin, 0.82f), 0.5f * zombie)
            tmpRect.set(126f, 92f, 146f, 108f)
            canvas.drawOval(tmpRect, paint)
            tmpRect.set(58f, 60f, 74f, 74f)
            canvas.drawOval(tmpRect, paint)
        }
    }

    private fun drawFace(canvas: Canvas, skin: Int, p: Float, zombie: Float, tired: Float) {
        val leftEye = 80f
        val rightEye = 122f
        val eyeY = 82f
        val xEye = ramp(0.80f, 0.97f, p)   // 눈이 X 로 바뀌는 정도

        // 볼터치 (좀비가 될수록 옅어짐)
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(BLUSH, 0.75f * (1f - ramp(0.25f, 0.75f, p)))
        tmpRect.set(52f, 96f, 76f, 110f)
        canvas.drawOval(tmpRect, paint)
        tmpRect.set(126f, 96f, 150f, 110f)
        canvas.drawOval(tmpRect, paint)

        // 다크서클
        if (tired > 0.02f) {
            strokePaint.strokeWidth = 4f
            strokePaint.color = withAlpha(0xFF8E7BA6.toInt(), 0.55f * tired)
            tmpPath.reset()
            tmpPath.moveTo(leftEye - 13f, eyeY + 16f)
            tmpPath.quadTo(leftEye, eyeY + 23f, leftEye + 13f, eyeY + 16f)
            canvas.drawPath(tmpPath, strokePaint)
            tmpPath.reset()
            tmpPath.moveTo(rightEye - 13f, eyeY + 16f)
            tmpPath.quadTo(rightEye, eyeY + 23f, rightEye + 13f, eyeY + 16f)
            canvas.drawPath(tmpPath, strokePaint)
        }

        if (xEye < 0.99f) {
            drawEye(canvas, leftEye, eyeY, skin, p, tired, 1f - xEye)
            drawEye(canvas, rightEye, eyeY, skin, p, tired, 1f - xEye)
        }
        if (xEye > 0.01f) {
            strokePaint.strokeWidth = 6f
            strokePaint.color = withAlpha(EYE_DARK, xEye)
            drawX(canvas, leftEye, eyeY, 12f)
            drawX(canvas, rightEye, eyeY, 12f)
        }

        drawMouth(canvas, p, zombie)
    }

    private fun drawX(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawLine(cx - r, cy - r, cx + r, cy + r, strokePaint)
        canvas.drawLine(cx + r, cy - r, cx - r, cy + r, strokePaint)
    }

    private fun drawEye(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        skin: Int,
        p: Float,
        tired: Float,
        alpha: Float
    ) {
        val r = lerp(15f, 12f, tired)
        val droop = tired  // 눈꺼풀 내려오는 정도

        canvas.save()
        tmpPath.reset()
        tmpPath.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.clipPath(tmpPath)

        paint.style = Paint.Style.FILL

        // 흰자
        paint.color = withAlpha(0xFFFFFFFF.toInt(), alpha)
        canvas.drawCircle(cx, cy, r, paint)

        // 충혈
        if (p > 0.5f) {
            strokePaint.strokeWidth = 1.6f
            strokePaint.color = withAlpha(0xFFE05C6E.toInt(), 0.7f * ramp(0.5f, 0.9f, p) * alpha)
            canvas.drawLine(cx - r, cy - 4f, cx + 2f, cy + 2f, strokePaint)
            canvas.drawLine(cx - r + 3f, cy + 6f, cx + 4f, cy - 1f, strokePaint)
        }

        // 눈동자 (아래로 처지고 작아짐)
        val pupilR = lerp(9.5f, 6.5f, ramp(0.2f, 0.9f, p))
        val pupilY = cy + lerp(0f, 3.5f, droop)
        paint.color = withAlpha(EYE_DARK, alpha)
        canvas.drawCircle(cx, pupilY, pupilR, paint)

        // 반짝임 (인간일수록 크게)
        val shineA = (1f - ramp(0.05f, 0.6f, p)) * alpha
        if (shineA > 0.02f) {
            paint.color = withAlpha(Color.WHITE, shineA)
            canvas.drawCircle(cx + 3.4f, pupilY - 3.4f, 3.2f, paint)
            paint.color = withAlpha(Color.WHITE, shineA * 0.7f)
            canvas.drawCircle(cx - 3.6f, pupilY + 3.2f, 1.6f, paint)
        }

        // 눈꺼풀
        if (droop > 0.02f) {
            paint.color = withAlpha(skin, alpha)
            tmpRect.set(cx - r - 2f, cy - r - 2f, cx + r + 2f, cy - r + (2 * r + 2f) * droop * 0.55f)
            canvas.drawRect(tmpRect, paint)
        }

        canvas.restore()

        // 눈 테두리
        strokePaint.strokeWidth = 2.6f
        strokePaint.color = withAlpha(darken(skin, 0.72f), 0.9f * alpha)
        canvas.drawCircle(cx, cy, r, strokePaint)
    }

    private fun drawMouth(canvas: Canvas, p: Float, zombie: Float) {
        val cx = 100f
        val cy = 116f
        val open = ramp(0.55f, 1f, p)          // 입 벌어짐
        val smile = lerp(9f, -8f, ramp(0f, 0.6f, p))  // + 웃음 / - 찡그림
        val w = lerp(16f, 26f, open)
        val h = lerp(6f, 22f, open)

        if (open < 0.35f) {
            // 닫힌 입 (웃거나 시무룩)
            strokePaint.strokeWidth = 4.2f
            strokePaint.color = EYE_DARK
            tmpPath.reset()
            tmpPath.moveTo(cx - 14f, cy)
            tmpPath.quadTo(cx, cy + smile, cx + 14f, cy)
            canvas.drawPath(tmpPath, strokePaint)
        } else {
            // 벌어진 입 + 이빨 + 혀
            paint.style = Paint.Style.FILL
            paint.color = 0xFF5B2740.toInt()
            tmpRect.set(cx - w, cy - h * 0.35f, cx + w, cy + h)
            canvas.drawRoundRect(tmpRect, 10f, 10f, paint)

            // 혀
            paint.color = 0xFFE87393.toInt()
            tmpRect.set(cx - 9f, cy + h - 12f, cx + 9f, cy + h + 6f)
            canvas.drawRoundRect(tmpRect, 8f, 8f, paint)

            // 송곳니
            paint.color = Color.WHITE
            tmpPath.reset()
            tmpPath.moveTo(cx - w + 5f, cy - h * 0.3f)
            tmpPath.lineTo(cx - w + 13f, cy - h * 0.3f)
            tmpPath.lineTo(cx - w + 9f, cy - h * 0.3f + 9f * open)
            tmpPath.close()
            canvas.drawPath(tmpPath, paint)
            tmpPath.reset()
            tmpPath.moveTo(cx + w - 5f, cy - h * 0.3f)
            tmpPath.lineTo(cx + w - 13f, cy - h * 0.3f)
            tmpPath.lineTo(cx + w - 9f, cy - h * 0.3f + 9f * open)
            tmpPath.close()
            canvas.drawPath(tmpPath, paint)
        }

        // 침 한 방울
        if (zombie > 0.55f) {
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(0xFF9CD6E8.toInt(), ramp(0.55f, 0.9f, zombie))
            tmpRect.set(cx + w - 13f, cy + h - 6f, cx + w - 5f, cy + h + 12f)
            canvas.drawOval(tmpRect, paint)
        }
    }

    private fun drawExtras(canvas: Canvas, p: Float, zombie: Float) {
        // 꿰맨 자국 (볼)
        val st = ramp(0.5f, 0.8f, p)
        if (st > 0.02f) {
            strokePaint.strokeWidth = 2.6f
            strokePaint.color = withAlpha(0xFF6B5B4A.toInt(), st)
            canvas.drawLine(132f, 62f, 146f, 78f, strokePaint)
            var i = 0
            while (i < 4) {
                val t = 0.15f + i * 0.24f
                val x = lerp(132f, 146f, t)
                val y = lerp(62f, 78f, t)
                canvas.drawLine(x - 5f, y - 2f, x + 5f, y + 2f, strokePaint)
                i++
            }
        }

        // 붕대 (거의 좀비)
        val band = ramp(0.85f, 1f, p)
        if (band > 0.02f) {
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(0xFFFDF3E3.toInt(), band)
            canvas.save()
            canvas.rotate(-16f, 78f, 50f)
            tmpRect.set(52f, 40f, 110f, 58f)
            canvas.drawRoundRect(tmpRect, 8f, 8f, paint)
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 2f
            strokePaint.color = withAlpha(0xFFE3CFAE.toInt(), band)
            canvas.drawLine(70f, 40f, 62f, 58f, strokePaint)
            canvas.drawLine(92f, 40f, 84f, 58f, strokePaint)
            canvas.restore()
        }

        // 반짝임 (아직 사람일 때)
        val spark = 1f - ramp(0.05f, 0.35f, p)
        if (spark > 0.02f) {
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(0xFFFFC94D.toInt(), spark)
            drawSparkle(canvas, 40f, 50f, 11f)
            drawSparkle(canvas, 164f, 72f, 8f)
            drawSparkle(canvas, 152f, 34f, 6f)
        }
    }

    /** 네 갈래 반짝임 (채워진 별) */
    private fun drawSparkle(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val k = r * 0.2f
        tmpPath.reset()
        tmpPath.moveTo(cx, cy - r)
        tmpPath.quadTo(cx + k, cy - k, cx + r, cy)
        tmpPath.quadTo(cx + k, cy + k, cx, cy + r)
        tmpPath.quadTo(cx - k, cy + k, cx - r, cy)
        tmpPath.quadTo(cx - k, cy - k, cx, cy - r)
        tmpPath.close()
        canvas.drawPath(tmpPath, paint)
    }
}
