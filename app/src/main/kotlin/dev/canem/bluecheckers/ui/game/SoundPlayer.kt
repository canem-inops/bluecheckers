package dev.canem.bluecheckers.ui.game

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays short UI feedback sounds using Android's built-in system UI sound effects.
 * No bundled audio files, no extra permissions. The caller decides whether to play
 * based on the user's preference; this class always plays when asked.
 *
 * Future revision: swap for SoundPool + res/raw assets without changing the API.
 */
@Singleton
class SoundPlayer @Inject constructor(@ApplicationContext context: Context) {

    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun playMove() {
        audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, MOVE_VOLUME)
    }

    fun playCapture() {
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, CAPTURE_VOLUME)
    }

    fun playOutcome() {
        audioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, OUTCOME_VOLUME)
    }

    companion object {
        private const val MOVE_VOLUME = 0.4f
        private const val CAPTURE_VOLUME = 0.7f
        private const val OUTCOME_VOLUME = 0.8f
    }
}
