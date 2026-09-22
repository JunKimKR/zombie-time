package com.zombietime.app.character

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.zombietime.app.R

/** Short locally bundled creature chirps. No network, TTS engine or microphone. */
class CharacterVoice(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
    private var stream = 0
    private var released = false
    private val sounds: List<Int>

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        sounds = listOf(R.raw.hello, R.raw.surprise, R.raw.giggle).map { pool.load(context, it, 1) }
    }

    fun play(kind: Int, zombie: Float) {
        if (released || audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val id = sounds[kind.mod(sounds.size)]
        if (id !in loaded) return
        pool.stop(stream)
        stream = pool.play(id, 0.55f, 0.55f, 1, 0, 1.05f - zombie.coerceIn(0f, 1f) * 0.22f)
    }

    fun stop() { if (!released) pool.stop(stream) }
    fun release() {
        if (released) return
        released = true
        pool.release()
    }
}
