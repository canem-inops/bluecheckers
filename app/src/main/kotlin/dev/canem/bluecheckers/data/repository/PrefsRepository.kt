package dev.canem.bluecheckers.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.bluePrefs: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * User-facing preferences. Defaults align with what most players expect on a
 * fresh install: animations on, hints on, undo on, sound off, self-play off,
 * CBD draw counters (not FMJD-64 tournament tier).
 */
data class Prefs(
    val animationsEnabled: Boolean = true,
    val showCaptureHints: Boolean = true,
    val undoEnabled: Boolean = true,
    val tournamentRules: Boolean = false,
    val soundEnabled: Boolean = false,
    val selfPlayEnabled: Boolean = false,
) {
    companion object {
        val DEFAULT = Prefs()
    }
}

@Singleton
class PrefsRepository @Inject constructor(
    @ApplicationContext context: Context,
) {

    private val ds: DataStore<Preferences> = context.bluePrefs

    val prefsFlow: Flow<Prefs> = ds.data.map { p ->
        Prefs(
            animationsEnabled = p[KEY_ANIMATIONS] ?: Prefs.DEFAULT.animationsEnabled,
            showCaptureHints = p[KEY_HINTS] ?: Prefs.DEFAULT.showCaptureHints,
            undoEnabled = p[KEY_UNDO] ?: Prefs.DEFAULT.undoEnabled,
            tournamentRules = p[KEY_TOURNAMENT] ?: Prefs.DEFAULT.tournamentRules,
            soundEnabled = p[KEY_SOUND] ?: Prefs.DEFAULT.soundEnabled,
            selfPlayEnabled = p[KEY_SELF_PLAY] ?: Prefs.DEFAULT.selfPlayEnabled,
        )
    }

    suspend fun read(): Prefs = prefsFlow.first()

    suspend fun setAnimationsEnabled(v: Boolean) = update(KEY_ANIMATIONS, v)
    suspend fun setShowCaptureHints(v: Boolean) = update(KEY_HINTS, v)
    suspend fun setUndoEnabled(v: Boolean) = update(KEY_UNDO, v)
    suspend fun setTournamentRules(v: Boolean) = update(KEY_TOURNAMENT, v)
    suspend fun setSoundEnabled(v: Boolean) = update(KEY_SOUND, v)
    suspend fun setSelfPlayEnabled(v: Boolean) = update(KEY_SELF_PLAY, v)

    private suspend fun update(key: Preferences.Key<Boolean>, value: Boolean) {
        ds.edit { it[key] = value }
    }

    companion object {
        private val KEY_ANIMATIONS = booleanPreferencesKey("animations_enabled")
        private val KEY_HINTS = booleanPreferencesKey("show_capture_hints")
        private val KEY_UNDO = booleanPreferencesKey("undo_enabled")
        private val KEY_TOURNAMENT = booleanPreferencesKey("tournament_rules")
        private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        private val KEY_SELF_PLAY = booleanPreferencesKey("self_play_enabled")
    }
}
