package com.zombietime.app.data

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.time.LocalDate

/** Earned, device-local rewards only. Never use this ledger for purchased currency. */
data class RecoveryState(
    val startedElapsed: Long = 0,
    val durationMinutes: Int = 0,
    val bootCount: Int = -1,
    val seeds: Int = 0,
    val sessions: Int = 0,
    val totalMinutes: Int = 0,
    val streak: Int = 0,
    val lastDate: String = "",
    val garden: String = "peach",
    val unlocked: Set<String> = setOf("peach")
) {
    val active get() = durationMinutes > 0
    fun remaining(now: Long): Long =
        (durationMinutes * 60_000L - (now - startedElapsed).coerceAtLeast(0)).coerceAtLeast(0)

    fun complete(now: Long, boot: Int, date: String): RecoveryState {
        if (!active) return this
        if (boot != bootCount || now < startedElapsed) return copy(durationMinutes = 0)
        if (remaining(now) > 0) return this
        val previous = runCatching { LocalDate.parse(lastDate) }.getOrNull()
        val today = LocalDate.parse(date)
        val nextStreak = when (previous) {
            today -> streak
            today.minusDays(1) -> streak + 1
            else -> 1
        }
        return copy(durationMinutes = 0, seeds = seeds + durationMinutes / 5,
            sessions = sessions + 1, totalMinutes = totalMinutes + durationMinutes,
            streak = nextStreak, lastDate = date)
    }

    fun selectGarden(id: String): RecoveryState {
        val cost = mapOf("peach" to 0, "forest" to 6, "moon" to 12)[id] ?: return this
        if (id in unlocked) return copy(garden = id)
        if (seeds < cost) return this
        return copy(seeds = seeds - cost, garden = id, unlocked = unlocked + id)
    }
}

object RecoveryStore {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("recovery", Context.MODE_PRIVATE)
    fun boot(ctx: Context) = Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, -1)
    fun load(ctx: Context): RecoveryState = prefs(ctx).let {
        RecoveryState(it.getLong("start", 0), it.getInt("duration", 0), it.getInt("boot", -1),
            it.getInt("seeds", 0), it.getInt("sessions", 0), it.getInt("minutes", 0),
            it.getInt("streak", 0), it.getString("date", "") ?: "",
            it.getString("garden", "peach") ?: "peach",
            it.getStringSet("unlocked", setOf("peach"))?.toSet() ?: setOf("peach"))
    }
    fun save(ctx: Context, s: RecoveryState) {
        prefs(ctx).edit().putLong("start", s.startedElapsed).putInt("duration", s.durationMinutes)
            .putInt("boot", s.bootCount).putInt("seeds", s.seeds).putInt("sessions", s.sessions)
            .putInt("minutes", s.totalMinutes).putInt("streak", s.streak).putString("date", s.lastDate)
            .putString("garden", s.garden).putStringSet("unlocked", s.unlocked).apply()
    }
    fun start(ctx: Context, state: RecoveryState, minutes: Int): RecoveryState {
        if (state.active || minutes !in listOf(5, 15, 25)) return state
        return state.copy(startedElapsed = SystemClock.elapsedRealtime(), durationMinutes = minutes,
            bootCount = boot(ctx)).also { save(ctx, it) }
    }
}
