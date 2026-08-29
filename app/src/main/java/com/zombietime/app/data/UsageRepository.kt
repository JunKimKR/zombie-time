package com.zombietime.app.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

object UsageRepository {

    /** '사용 정보 접근' 권한이 켜져 있는지 */
    fun hasUsagePermission(ctx: Context): Boolean {
        return try {
            val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            @Suppress("DEPRECATION")
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ctx.packageName
            )
            if (mode == AppOpsManager.MODE_DEFAULT) {
                ctx.checkCallingOrSelfPermission(
                    android.Manifest.permission.PACKAGE_USAGE_STATS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        } catch (e: Exception) {
            false
        }
    }

    fun usageSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** 기기에 설치된 추적 대상 앱 목록 */
    fun installedTrackedApps(ctx: Context): List<TrackedApp> {
        val pm = ctx.packageManager
        return TrackedApps.ALL.filter { app ->
            try {
                pm.getPackageInfo(app.pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /** 오늘 0시부터 지금까지의 사용시간 */
    fun queryToday(ctx: Context): DayUsage {
        val now = System.currentTimeMillis()
        val start = Prefs.startOfDay(now)
        return DayUsage(Prefs.dateOf(now), aggregate(ctx, start, now))
    }

    /**
     * 이벤트 기반 포그라운드 시간 집계.
     * 화면에 실제로 떠 있던 시간만 더하므로 백그라운드 재생/알림은 포함되지 않는다.
     */
    @Suppress("DEPRECATION")
    private fun aggregate(ctx: Context, start: Long, end: Long): Map<String, Long> {
        val result = HashMap<String, Long>()
        if (!hasUsagePermission(ctx)) return result

        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return result

        try {
            val events = usm.queryEvents(start, end)
            val event = UsageEvents.Event()
            val openedAt = HashMap<String, Long>()

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val pkg = event.packageName ?: continue
                if (!TrackedApps.PACKAGES.contains(pkg)) continue

                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        openedAt[pkg] = event.timeStamp
                    }

                    UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                        val from = openedAt.remove(pkg)
                        if (from != null && event.timeStamp > from) {
                            result[pkg] = (result[pkg] ?: 0L) + (event.timeStamp - from)
                        }
                    }
                }
            }

            // 지금 이 순간까지도 켜져 있는 앱
            val now = if (end < System.currentTimeMillis()) end else System.currentTimeMillis()
            for (entry in openedAt) {
                if (now > entry.value) {
                    result[entry.key] = (result[entry.key] ?: 0L) + (now - entry.value)
                }
            }
        } catch (e: Exception) {
            // 권한이 방금 꺼졌거나 제조사 제한 - 조용히 빈 값
        }

        // 말도 안 되는 값 방어 (하루 24시간 초과 등)
        val span = end - start
        val cleaned = HashMap<String, Long>()
        for (entry in result) {
            val v = entry.value
            if (v > 0) cleaned[entry.key] = if (v > span) span else v
        }
        return cleaned
    }

    /**
     * 오늘 사용량을 읽고, 로컬 기록에도 저장한 뒤 돌려준다.
     * (기기 이벤트 로그는 며칠 뒤 사라지므로 우리가 매일 스냅샷을 남긴다)
     */
    fun refreshAndStoreToday(ctx: Context): DayUsage {
        val today = queryToday(ctx)
        val stored = Prefs.loadDay(ctx, today.date)
        // 같은 날 안에서 값이 줄어드는 일은 없어야 하므로 큰 쪽을 남긴다
        val merged = HashMap<String, Long>()
        for (app in TrackedApps.ALL) {
            val a = today.ms(app.pkg)
            val b = stored.ms(app.pkg)
            val v = if (a > b) a else b
            if (v > 0) merged[app.pkg] = v
        }
        val result = DayUsage(today.date, merged)
        Prefs.saveDay(ctx, result)
        return result
    }
}
