package com.zombietime.app.data

data class Stage(
    val index: Int,
    val title: String,
    val quote: String,
    val emoji: String,
    /** 캐릭터 렌더링에 쓸 대표 진행도 */
    val sample: Float
)

object ZombieStages {

    val ALL: List<Stage> = listOf(
        Stage(0, "말짱한 사람", "눈이 아직 맑아! 오늘 컨디션 최고 ✨", "🙂", 0.02f),
        Stage(1, "살짝 몽롱", "음… 스크롤을 조금 많이 한 것 같은데?", "😌", 0.20f),
        Stage(2, "눈이 풀림", "눈꺼풀이… 스르륵… 내려가는 중…", "😵‍💫", 0.40f),
        Stage(3, "피부가 초록", "으… 손끝이 초록빛으로 변하고 있어…", "🫠", 0.62f),
        Stage(4, "거의 좀비", "끄어… 나… 아직… 사람… 이었나…?", "🧟‍♀️", 0.82f),
        Stage(5, "완전 좀비", "우어어어! 오늘은 여기까지! 폰 내려놔!", "🧟", 1.0f)
    )

    /** 목표 대비 진행도 (0~1.2 정도까지 넘어갈 수 있어 별도로 clamp) */
    fun rawProgress(totalMs: Long, goalMs: Long): Float {
        if (goalMs <= 0L) return 0f
        return totalMs.toFloat() / goalMs.toFloat()
    }

    fun progress(totalMs: Long, goalMs: Long): Float =
        rawProgress(totalMs, goalMs).coerceIn(0f, 1f)

    fun stageIndex(progress: Float): Int = when {
        progress < 0.10f -> 0
        progress < 0.30f -> 1
        progress < 0.50f -> 2
        progress < 0.72f -> 3
        progress < 0.92f -> 4
        else -> 5
    }

    fun stageOf(totalMs: Long, goalMs: Long): Stage =
        ALL[stageIndex(progress(totalMs, goalMs))]

    fun stage(index: Int): Stage = ALL[index.coerceIn(0, ALL.size - 1)]

    /** 단계가 올라갔을 때 띄울 알림 문구 */
    fun levelUpMessage(stage: Stage): Pair<String, String> = when (stage.index) {
        1 -> "살짝 몽롱해졌어요" to "아직 괜찮아요. 여기서 멈추면 오늘은 사람으로 퇴근! 🙂"
        2 -> "눈이 풀리기 시작했어요" to "눈꺼풀이 무거워요… 잠깐 폰을 내려놓을까요?"
        3 -> "피부가 초록빛이에요" to "으… 절반을 넘었어요. 산책 한 바퀴 어때요?"
        4 -> "거의 좀비가 됐어요" to "끄어… 조금만 더 보면 완전 좀비가 돼요!"
        5 -> "완전 좀비가 됐어요" to "우어어! 오늘 목표를 다 써버렸어요. 폰 내려놓기! 🧟"
        else -> "다시 사람이 됐어요" to "새로운 하루예요. 오늘도 사람으로 버텨봐요 ✨"
    }
}
