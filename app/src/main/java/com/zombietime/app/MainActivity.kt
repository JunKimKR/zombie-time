package com.zombietime.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zombietime.app.data.DayUsage
import com.zombietime.app.data.Prefs
import com.zombietime.app.data.UsageRepository
import com.zombietime.app.service.BriefingAlarm
import com.zombietime.app.service.ZombieMonitorService
import com.zombietime.app.share.ShareHelper
import com.zombietime.app.share.StoryImageBuilder
import com.zombietime.app.ui.BottomTabs
import com.zombietime.app.ui.BriefingSheet
import com.zombietime.app.ui.HomeScreen
import com.zombietime.app.ui.OnboardingScreen
import com.zombietime.app.ui.Pastel
import com.zombietime.app.ui.SettingsScreen
import com.zombietime.app.ui.WeeklyScreen
import com.zombietime.app.ui.ZombieTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.ensureChannels(this)
        BriefingAlarm.schedule(this)

        val openBriefing = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_BRIEFING, false) ?: false

        setContent {
            ZombieTheme {
                AppRoot(openBriefing)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
private fun AppRoot(openBriefingInitially: Boolean) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var hasUsage by remember { mutableStateOf(UsageRepository.hasUsagePermission(ctx)) }
    var hasNotif by remember { mutableStateOf(Notifications.canPost(ctx)) }
    var onboarded by remember { mutableStateOf(Prefs.isOnboarded(ctx)) }

    var usage by remember { mutableStateOf(Prefs.loadDay(ctx, Prefs.today())) }
    var week by remember { mutableStateOf(Prefs.recentDays(ctx, 7)) }
    var month by remember { mutableStateOf(Prefs.recentDays(ctx, 28)) }

    var goalMinutes by remember { mutableStateOf(Prefs.goalMinutes(ctx)) }
    var briefingHour by remember { mutableStateOf(Prefs.briefingHour(ctx)) }
    var stageAlert by remember { mutableStateOf(Prefs.stageAlertEnabled(ctx)) }
    var monitorOn by remember { mutableStateOf(Prefs.monitorEnabled(ctx)) }

    var tab by remember { mutableStateOf(0) }
    var briefingBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showBriefing by remember { mutableStateOf(false) }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotif = granted
    }

    fun requestNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.parse("package:" + ctx.packageName))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e: Exception) {
            }
        }
    }

    fun openUsageSettings() {
        try {
            ctx.startActivity(UsageRepository.usageSettingsIntent())
            Toast.makeText(ctx, "목록에서 '좀비타임'을 켜주세요", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "설정 화면을 열 수 없어요", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildBriefing() {
        scope.launch {
            showBriefing = true
            briefingBitmap = null
            val current: DayUsage = usage
            briefingBitmap = withContext(Dispatchers.Default) {
                StoryImageBuilder.build(ctx, current)
            }
        }
    }

    fun share(toStory: Boolean) {
        val bmp = briefingBitmap ?: return
        scope.launch {
            val uri = withContext(Dispatchers.Default) { StoryImageBuilder.saveForShare(ctx, bmp) }
            if (uri == null) {
                Toast.makeText(ctx, "이미지를 저장하지 못했어요", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (toStory) ShareHelper.shareToInstagramStory(ctx, uri)
            else ShareHelper.shareChooser(ctx, uri)
        }
    }

    // 주기적으로 권한/사용시간 갱신
    LaunchedEffect(Unit) {
        while (true) {
            val perm = UsageRepository.hasUsagePermission(ctx)
            hasUsage = perm
            hasNotif = Notifications.canPost(ctx)
            val fresh = withContext(Dispatchers.Default) {
                if (perm) UsageRepository.refreshAndStoreToday(ctx)
                else Prefs.loadDay(ctx, Prefs.today())
            }
            usage = fresh
            week = withContext(Dispatchers.Default) { Prefs.recentDays(ctx, 7) }
            month = withContext(Dispatchers.Default) { Prefs.recentDays(ctx, 28) }
            delay(4000L)
        }
    }

    // 권한/설정이 갖춰지면 상시 배너 서비스 켜기
    LaunchedEffect(hasUsage, monitorOn, onboarded) {
        if (onboarded && monitorOn && hasUsage) {
            ZombieMonitorService.start(ctx)
        } else if (!monitorOn) {
            ZombieMonitorService.stop(ctx)
        }
    }

    // 알림에서 들어온 경우 브리핑 자동 오픈
    LaunchedEffect(openBriefingInitially) {
        if (openBriefingInitially) {
            delay(900L)
            buildBriefing()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Pastel.Bg)
    ) {
        if (!onboarded) {
            OnboardingScreen(
                hasUsagePermission = hasUsage,
                hasNotifPermission = hasNotif,
                onGrantUsage = { openUsageSettings() },
                onGrantNotif = { requestNotif() },
                onStart = {
                    Prefs.setOnboarded(ctx, true)
                    onboarded = true
                    ZombieMonitorService.start(ctx)
                }
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp)
                ) {
                    when (tab) {
                        0 -> HomeScreen(
                            usage = usage,
                            goalMinutes = goalMinutes,
                            hasUsagePermission = hasUsage,
                            monitorOn = monitorOn,
                            onRequestUsagePermission = { openUsageSettings() },
                            onShareBriefing = { buildBriefing() }
                        )

                        1 -> WeeklyScreen(
                            week = week,
                            month = month,
                            goalMinutes = goalMinutes
                        )

                        else -> SettingsScreen(
                            goalMinutes = goalMinutes,
                            onGoalChange = {
                                goalMinutes = it
                                Prefs.setGoalMinutes(ctx, it)
                            },
                            briefingHour = briefingHour,
                            onBriefingHourChange = {
                                briefingHour = it
                                Prefs.setBriefingHour(ctx, it)
                                BriefingAlarm.schedule(ctx)
                            },
                            stageAlert = stageAlert,
                            onStageAlertChange = {
                                stageAlert = it
                                Prefs.setStageAlertEnabled(ctx, it)
                            },
                            monitorOn = monitorOn,
                            onMonitorChange = {
                                monitorOn = it
                                Prefs.setMonitorEnabled(ctx, it)
                                if (it) ZombieMonitorService.start(ctx)
                                else ZombieMonitorService.stop(ctx)
                            },
                            hasUsagePermission = hasUsage,
                            onOpenUsageSettings = { openUsageSettings() },
                            hasNotifPermission = hasNotif,
                            onRequestNotif = { requestNotif() }
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                BottomTabs(
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }

        if (showBriefing) {
            BriefingSheet(
                bitmap = briefingBitmap,
                onShareStory = { share(true) },
                onShareOther = { share(false) },
                onClose = { showBriefing = false }
            )
        }
    }
}
