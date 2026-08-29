package com.zombietime.app.data

/** 사용시간을 추적할 SNS 앱 */
data class TrackedApp(
    val pkg: String,
    val label: String,
    val emoji: String,
    val color: Int
)

object TrackedApps {
    val ALL: List<TrackedApp> = listOf(
        TrackedApp("com.instagram.android", "인스타그램", "📸", 0xFFE86FA0.toInt()),
        TrackedApp("com.instagram.barcelona", "스레드", "🧵", 0xFF8B7BE8.toInt()),
        TrackedApp("com.facebook.katana", "페이스북", "💙", 0xFF6C9BE8.toInt()),
        TrackedApp("com.google.android.youtube", "유튜브", "📺", 0xFFF07A7A.toInt())
    )

    val PACKAGES: Set<String> = ALL.map { it.pkg }.toSet()

    fun of(pkg: String): TrackedApp? = ALL.firstOrNull { it.pkg == pkg }
}

/** 하루치 사용 기록 */
data class DayUsage(
    val date: String,                 // yyyy-MM-dd
    val perApp: Map<String, Long>     // 패키지 -> 밀리초
) {
    val totalMs: Long get() = perApp.values.sum()

    fun ms(pkg: String): Long = perApp[pkg] ?: 0L

    companion object {
        fun empty(date: String) = DayUsage(date, emptyMap())
    }
}

object TimeFmt {
    /** 3720000 -> "1시간 2분" */
    fun human(ms: Long): String {
        val totalMin = ms / 60000L
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 && m > 0 -> "${h}시간 ${m}분"
            h > 0 -> "${h}시간"
            else -> "${m}분"
        }
    }

    /** 짧은 표기: "1h 02m" 대신 "1시간 2분" 을 알림에 쓰되 더 짧게 */
    fun short(ms: Long): String {
        val totalMin = ms / 60000L
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "${h}시간 ${m}분" else "${m}분"
    }
}
