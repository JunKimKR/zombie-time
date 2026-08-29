package com.zombietime.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zombietime.app.character.CharacterRenderer
import com.zombietime.app.data.DayUsage
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.Stage
import com.zombietime.app.data.TimeFmt
import com.zombietime.app.data.ZombieStages

object Notifications {

    const val CH_STATUS = "zombie_status"
    const val CH_ALERT = "zombie_alert"
    const val CH_BRIEFING = "zombie_briefing"

    const val ID_STATUS = 1001
    const val ID_ALERT = 1002
    const val ID_BRIEFING = 1003

    const val EXTRA_OPEN_BRIEFING = "open_briefing"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val status = NotificationChannel(
            CH_STATUS,
            ctx.getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = ctx.getString(R.string.channel_status_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val alert = NotificationChannel(
            CH_ALERT,
            ctx.getString(R.string.channel_alert_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ctx.getString(R.string.channel_alert_desc)
        }

        val briefing = NotificationChannel(
            CH_BRIEFING,
            ctx.getString(R.string.channel_briefing_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = ctx.getString(R.string.channel_briefing_desc)
        }

        nm.createNotificationChannel(status)
        nm.createNotificationChannel(alert)
        nm.createNotificationChannel(briefing)
    }

    fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ctx.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun openAppIntent(ctx: Context, openBriefing: Boolean): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openBriefing) putExtra(EXTRA_OPEN_BRIEFING, true)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(
            ctx,
            if (openBriefing) 21 else 20,
            intent,
            flags
        )
    }

    /** 상시 배너: 캐릭터 현황 + 사용시간 */
    fun buildStatus(ctx: Context, usage: DayUsage): android.app.Notification {
        val goalMs = Prefs.goalMs(ctx)
        val progress = ZombieStages.progress(usage.totalMs, goalMs)
        val stage = ZombieStages.stage(ZombieStages.stageIndex(progress))
        val pct = (progress * 100f).toInt()

        val title = "${stage.emoji} ${stage.title}  ·  ${TimeFmt.short(usage.totalMs)}"
        val body = buildString {
            append("목표 ")
            append(TimeFmt.short(goalMs))
            append(" 중 ")
            append(pct)
            append("% 사용")
        }

        val big = StringBuilder()
        big.append(stage.quote).append('\n')
        var any = false
        for (app in com.zombietime.app.data.TrackedApps.ALL) {
            val ms = usage.ms(app.pkg)
            if (ms >= 60000L) {
                if (any) big.append("   ")
                big.append(app.emoji).append(' ').append(TimeFmt.short(ms))
                any = true
            }
        }
        if (!any) big.append("아직 SNS를 열지 않았어요 👏")

        return NotificationCompat.Builder(ctx, CH_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(CharacterRenderer.renderBitmap(192, progress))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(big.toString()))
            .setProgress(100, pct.coerceIn(0, 100), false)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent(ctx, false))
            .build()
    }

    fun postStatus(ctx: Context, usage: DayUsage) {
        if (!canPost(ctx)) return
        try {
            NotificationManagerCompat.from(ctx).notify(ID_STATUS, buildStatus(ctx, usage))
        } catch (e: SecurityException) {
            // 권한 없음
        }
    }

    /** 단계 상승 알림 */
    fun postStageChange(ctx: Context, stage: Stage, usage: DayUsage) {
        if (!canPost(ctx)) return
        val (title, body) = ZombieStages.levelUpMessage(stage)
        val progress = ZombieStages.progress(usage.totalMs, Prefs.goalMs(ctx))
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(CharacterRenderer.renderBitmap(192, progress))
            .setContentTitle("${stage.emoji} $title")
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$body\n\n오늘 SNS ${TimeFmt.short(usage.totalMs)} 사용 중")
            )
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, false))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(ID_ALERT, n)
        } catch (e: SecurityException) {
        }
    }

    /** 하루 마감 브리핑 알림 */
    fun postBriefing(ctx: Context, usage: DayUsage) {
        if (!canPost(ctx)) return
        val goalMs = Prefs.goalMs(ctx)
        val progress = ZombieStages.progress(usage.totalMs, goalMs)
        val stage = ZombieStages.stage(ZombieStages.stageIndex(progress))
        val n = NotificationCompat.Builder(ctx, CH_BRIEFING)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(CharacterRenderer.renderBitmap(192, progress))
            .setContentTitle("📮 오늘의 좀비 리포트가 도착했어요")
            .setContentText("${stage.title} · SNS ${TimeFmt.short(usage.totalMs)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("오늘은 ${stage.title}로 마감!\nSNS ${TimeFmt.short(usage.totalMs)} 사용했어요.\n눌러서 브리핑 카드를 확인하고 스토리에 공유해보세요.")
            )
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(ctx, true))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(ID_BRIEFING, n)
        } catch (e: SecurityException) {
        }
    }

    fun cancelStatus(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancel(ID_STATUS)
    }
}
