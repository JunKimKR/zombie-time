package com.zombietime.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zombietime.app.Notifications
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.UsageRepository

/** 하루가 끝나면 오늘의 브리핑 알림을 보낸다. */
class DailyBriefingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val ctx = context.applicationContext
        Notifications.ensureChannels(ctx)

        val today = Prefs.today()
        val usage = if (UsageRepository.hasUsagePermission(ctx)) {
            UsageRepository.refreshAndStoreToday(ctx)
        } else {
            Prefs.loadDay(ctx, today)
        }

        if (Prefs.lastBriefingDate(ctx) != today) {
            Prefs.setLastBriefingDate(ctx, today)
            Notifications.postBriefing(ctx, usage)
        }

        // 다음 날 다시 예약 (setInexactRepeating 이 취소된 경우 대비)
        BriefingAlarm.schedule(ctx)
    }
}
