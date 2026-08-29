package com.zombietime.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zombietime.app.Notifications
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.UsageRepository

/** 재부팅/앱 업데이트 후에도 배너가 계속 뜨도록 서비스를 다시 켠다. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON" &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val ctx = context.applicationContext
        Notifications.ensureChannels(ctx)
        BriefingAlarm.schedule(ctx)
        if (Prefs.monitorEnabled(ctx) && UsageRepository.hasUsagePermission(ctx)) {
            ZombieMonitorService.start(ctx)
        }
    }
}
