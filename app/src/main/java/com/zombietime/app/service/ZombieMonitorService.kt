package com.zombietime.app.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.zombietime.app.Notifications
import com.zombietime.app.data.DayUsage
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.UsageRepository
import com.zombietime.app.data.ZombieStages
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 항상 떠 있는 배너(상시 알림)를 유지하면서 1분마다 SNS 사용시간을 갱신하는 서비스.
 */
class ZombieMonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val cached = Prefs.loadDay(applicationContext, Prefs.today())
        startForegroundCompat(Notifications.buildStatus(applicationContext, cached))

        if (loopJob == null || loopJob?.isActive != true) {
            loopJob = scope.launch {
                while (isActive) {
                    tick()
                    delay(TICK_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        loopJob = null
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    Notifications.ID_STATUS,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(Notifications.ID_STATUS, notification)
            }
        } catch (e: Exception) {
            // 백그라운드 시작 제한 등 - 조용히 무시
        }
    }

    private fun tick() {
        val ctx = applicationContext
        if (!UsageRepository.hasUsagePermission(ctx)) {
            Notifications.postStatus(ctx, Prefs.loadDay(ctx, Prefs.today()))
            return
        }

        val usage = UsageRepository.refreshAndStoreToday(ctx)
        Notifications.postStatus(ctx, usage)
        checkStageChange(ctx, usage)
        checkBriefing(ctx, usage)
    }

    private fun checkStageChange(ctx: Context, usage: DayUsage) {
        val today = Prefs.today()
        val progress = ZombieStages.progress(usage.totalMs, Prefs.goalMs(ctx))
        val stageIndex = ZombieStages.stageIndex(progress)

        if (Prefs.lastStageDate(ctx) != today) {
            // 새로운 하루 - 기준만 새로 잡는다
            Prefs.setLastStage(ctx, stageIndex, today)
            return
        }

        val last = Prefs.lastStage(ctx)
        if (stageIndex > last) {
            if (Prefs.stageAlertEnabled(ctx)) {
                Notifications.postStageChange(ctx, ZombieStages.stage(stageIndex), usage)
            }
            Prefs.setLastStage(ctx, stageIndex, today)
        } else if (stageIndex < last) {
            Prefs.setLastStage(ctx, stageIndex, today)
        }
    }

    private fun checkBriefing(ctx: Context, usage: DayUsage) {
        val today = Prefs.today()
        if (Prefs.lastBriefingDate(ctx) == today) return
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < Prefs.briefingHour(ctx)) return
        Prefs.setLastBriefingDate(ctx, today)
        Notifications.postBriefing(ctx, usage)
    }

    companion object {
        private const val TICK_MS = 60_000L

        fun start(ctx: Context) {
            if (!Prefs.monitorEnabled(ctx)) return
            val intent = Intent(ctx, ZombieMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            } catch (e: Exception) {
                // 백그라운드에서 시작이 막힌 경우
            }
        }

        fun stop(ctx: Context) {
            try {
                ctx.stopService(Intent(ctx, ZombieMonitorService::class.java))
            } catch (e: Exception) {
            }
            Notifications.cancelStatus(ctx)
        }
    }
}
