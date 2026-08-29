package com.zombietime.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.zombietime.app.data.Prefs
import java.util.Calendar

/** 매일 정해진 시각에 하루 브리핑을 띄우는 알람 */
object BriefingAlarm {

    private const val REQUEST = 3001

    private fun pending(ctx: Context): PendingIntent {
        val intent = Intent(ctx, DailyBriefingReceiver::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(ctx.applicationContext, REQUEST, intent, flags)
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val hour = Prefs.briefingHour(ctx)

        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= System.currentTimeMillis()) {
            c.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            // 정확 알람 권한 없이도 동작하도록 부정확 알람 사용
            am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                c.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pending(ctx)
            )
        } catch (e: Exception) {
        }
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        try {
            am.cancel(pending(ctx))
        } catch (e: Exception) {
        }
    }
}
