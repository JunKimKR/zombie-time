package com.zombietime.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Prefs {

    private const val NAME = "zombietime"

    private const val KEY_GOAL_MIN = "goal_minutes"
    private const val KEY_STAGE_ALERT = "stage_alert"
    private const val KEY_LAST_STAGE = "last_stage"
    private const val KEY_LAST_STAGE_DATE = "last_stage_date"
    private const val KEY_HISTORY = "history"
    private const val KEY_BRIEFING_HOUR = "briefing_hour"
    private const val KEY_LAST_BRIEFING = "last_briefing_date"
    private const val KEY_ONBOARDED = "onboarded"
    private const val KEY_MONITOR_ON = "monitor_on"

    const val DEFAULT_GOAL_MINUTES = 180   // 3시간

    private const val HISTORY_DAYS = 60

    private fun sp(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ 날짜

    private val dateFmt: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    fun today(): String = dateFmt.format(Date())

    fun dateOf(millis: Long): String = dateFmt.format(Date(millis))

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // ------------------------------------------------------------------ 설정

    fun goalMinutes(ctx: Context): Int =
        sp(ctx).getInt(KEY_GOAL_MIN, DEFAULT_GOAL_MINUTES)

    fun setGoalMinutes(ctx: Context, minutes: Int) {
        sp(ctx).edit().putInt(KEY_GOAL_MIN, minutes.coerceIn(30, 480)).apply()
    }

    fun goalMs(ctx: Context): Long = goalMinutes(ctx) * 60_000L

    fun stageAlertEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_STAGE_ALERT, true)

    fun setStageAlertEnabled(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean(KEY_STAGE_ALERT, on).apply()
    }

    fun briefingHour(ctx: Context): Int = sp(ctx).getInt(KEY_BRIEFING_HOUR, 22)

    fun setBriefingHour(ctx: Context, hour: Int) {
        sp(ctx).edit().putInt(KEY_BRIEFING_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun isOnboarded(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_ONBOARDED, false)

    fun setOnboarded(ctx: Context, v: Boolean) {
        sp(ctx).edit().putBoolean(KEY_ONBOARDED, v).apply()
    }

    fun monitorEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_MONITOR_ON, true)

    fun setMonitorEnabled(ctx: Context, on: Boolean) {
        sp(ctx).edit().putBoolean(KEY_MONITOR_ON, on).apply()
    }

    // -------------------------------------------------------------- 단계 상태

    fun lastStage(ctx: Context): Int = sp(ctx).getInt(KEY_LAST_STAGE, 0)

    fun lastStageDate(ctx: Context): String = sp(ctx).getString(KEY_LAST_STAGE_DATE, "") ?: ""

    fun setLastStage(ctx: Context, stage: Int, date: String) {
        sp(ctx).edit()
            .putInt(KEY_LAST_STAGE, stage)
            .putString(KEY_LAST_STAGE_DATE, date)
            .apply()
    }

    fun lastBriefingDate(ctx: Context): String = sp(ctx).getString(KEY_LAST_BRIEFING, "") ?: ""

    fun setLastBriefingDate(ctx: Context, date: String) {
        sp(ctx).edit().putString(KEY_LAST_BRIEFING, date).apply()
    }

    // ------------------------------------------------------------- 기록 저장

    /** 하루치 기록을 저장(덮어쓰기)하고 오래된 기록은 정리한다. */
    fun saveDay(ctx: Context, day: DayUsage) {
        val root = readHistory(ctx)
        val obj = JSONObject()
        for ((pkg, ms) in day.perApp) obj.put(pkg, ms)
        root.put(day.date, obj)

        // 오래된 기록 정리
        if (root.length() > HISTORY_DAYS) {
            val keys = ArrayList<String>()
            val it = root.keys()
            while (it.hasNext()) keys.add(it.next())
            keys.sort()
            var remove = keys.size - HISTORY_DAYS
            var i = 0
            while (remove > 0 && i < keys.size) {
                root.remove(keys[i])
                i++
                remove--
            }
        }
        sp(ctx).edit().putString(KEY_HISTORY, root.toString()).apply()
    }

    fun loadDay(ctx: Context, date: String): DayUsage {
        val root = readHistory(ctx)
        val obj = root.optJSONObject(date) ?: return DayUsage.empty(date)
        val map = HashMap<String, Long>()
        val it = obj.keys()
        while (it.hasNext()) {
            val k = it.next()
            map[k] = obj.optLong(k, 0L)
        }
        return DayUsage(date, map)
    }

    /** 오늘 포함 최근 n일 (과거 → 오늘 순서) */
    fun recentDays(ctx: Context, n: Int): List<DayUsage> {
        val out = ArrayList<DayUsage>()
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, -(n - 1))
        var i = 0
        while (i < n) {
            out.add(loadDay(ctx, dateFmt.format(c.time)))
            c.add(Calendar.DAY_OF_YEAR, 1)
            i++
        }
        return out
    }

    private fun readHistory(ctx: Context): JSONObject {
        val raw = sp(ctx).getString(KEY_HISTORY, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (e: Exception) {
            JSONObject()
        }
    }
}
