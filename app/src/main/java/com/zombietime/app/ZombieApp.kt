package com.zombietime.app

import android.app.Application
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.UsageRepository
import com.zombietime.app.service.BriefingAlarm
import com.zombietime.app.service.ZombieMonitorService

class ZombieApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
        BriefingAlarm.schedule(this)
        if (Prefs.isOnboarded(this) &&
            Prefs.monitorEnabled(this) &&
            UsageRepository.hasUsagePermission(this)
        ) {
            ZombieMonitorService.start(this)
        }
    }
}
