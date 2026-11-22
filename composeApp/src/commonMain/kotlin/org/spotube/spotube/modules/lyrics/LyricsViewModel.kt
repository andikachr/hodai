/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.modules.lyrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsResponse
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.LoopState
import org.spotube.spotube.core.audioplayer.PlayerState
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.lyrics.plus.LyricsPlusRepository
import org.spotube.spotube.modules.lyrics.plus.systemLanguageCode
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.settings.SettingsProvider
import org.spotube.spotube.modules.settings.SupportedLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Serializable
enum class LyricType {
    STATIC,
    SYNCED
}

data class LyricsUiState(
    val mode: LyricType = LyricType.SYNCED,
    val syncedLyrics: List<LyricsLine>? = null,
    val plainLyrics: String? = null,
    /** Lirik+ romaji layer, aligned with the synced (or plain) lines; null = no layer. */
    val romajiLines: List<String?>? = null,
    /** Lirik+ translation layer, aligned the same way; null = no layer. */
    val translationLines: List<String?>? = null,
    val isLoading: Boolean = false,
    /** True while the plugin came back empty and fallback sources are being tried. */
    val isSearchingFallback: Boolean = false,
    val error: String? = null,
    val currentIndex: Int = -1,
    val trackTitle: String = "",
    val trackArtists: String = "",
    val coverUrl: String = "",
    /** Playback snapshot for the stage's player rail (mock layout). */
    val isPlaying: Boolean = false,
    val position: Duration = Duration.ZERO,
    val duration: Duration = Duration.ZERO,
    val shuffleEnabled: Boolean = false,
    val loopState: LoopState = LoopState.NONE,
    /** Lirik+ layer switches, mirrored from the user settings. */
    val romajiEnabled: Boolean = true,
    val translationEnabled: Boolean = true)

class LyricsViewModel(
    private val pluginManager: PluginManager,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val settingsProvider: SettingsProvider,
    private val lyricsPlusRepository: LyricsPlusRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())

    // Only re-emits when the active line changes, not on every position tick.
    private val currentIndexFlow = combine(
        _uiState.map { it.syncedLyrics }.distinctUntilChanged(),
        audioPlayer.positionFlow
    ) { lyrics, position -> activeLineIndex(lyrics, position.inWholeMilliseconds) }
        .distinctUntilChanged()

    private data class PlaybackSnapshot(
        val isPlaying: Boolean = false,
        val position: Duration = Duration.ZERO,
        val duration: Duration = Duration.ZERO,
        val shuffleEnabled: Boolean = false,
        val loopState: LoopState = LoopState.NONE)

    private val playbackFlow = combine(
        audioPlayer.playerStateFlow,
        audioPlayer.positionFlow,
        audioPlayer.durationFlow,
        audioPlayer.shuffleModeFlow,
        audioPlayer.loopStateFlow
    ) { state, position, duration, shuffle, loop ->
        PlaybackSnapshot(
            isPlaying = state == PlayerState.PLAYING,
            position = position,
            duration = duration,
            shuffleEnabled = shuffle,
            loopState = loop)
    }

    val uiState: StateFlow<LyricsUiState> = combine(
        _uiState, currentIndexFlow, playbackFlow
    ) { state, index, playback ->
        state.copy(
            currentIndex = index,
            isPlaying = playback.isPlaying,
            position = playback.position,
            duration = playback.duration,
            shuffleEnabled = playback.shuffleEnabled,
            loopState = playback.loopState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LyricsUiState())

    private var currentTrackId: String? = null

    fun setMode(mode: LyricType) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun loadLyrics(
        forceReload: Boolean = false,
        romajiEnabled: Boolean? = null,
        translationEnabled: Boolean? = null) {
        viewModelScope.launch {
            val entry = audioPlayerQueue.currentQueueEntryFlow.value ?: return@launch
            val track = when (entry) {
                is QueueEntry.StreamingTrack -> entry.track
                is QueueEntry.LocalTrack -> return@launch
            }

            if (!forceReload && currentTrackId == track.id && _uiState.value.syncedLyrics != null) {
                return@launch
            }

            currentTrackId = track.id
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    isSearchingFallback = false,
                    romajiLines = null,
                    translationLines = null)
            }
            try {
                var pluginLyrics: LyricsResponse? = null
                pluginManager.selectedLyricsPlugin.value?.let { pluginService ->
                    pluginService.use {
                        pluginLyrics = lyricsAPI.getLyrics(track)
                    }
                }

                val hasPluginLyrics =
                    pluginLyrics?.syncedLyrics?.isNotEmpty() == true ||
                        !pluginLyrics?.plainLyrics.isNullOrBlank()
                if (!hasPluginLyrics) {
                    _uiState.update { it.copy(isSearchingFallback = true) }
                }

                // Lirik+: layered fallback coverage + romaji/translation
                // layers, cached per track. Any failure inside resolves to
                // null and the plugin result (if any) is used as-is.
                val currentSettings = settingsProvider.settingsState.value
                val useRomaji = romajiEnabled ?: currentSettings?.lyricsRomajiEnabled ?: true
                val useTranslation = translationEnabled
                    ?: currentSettings?.lyricsTranslationEnabled ?: true
                val plusResult = try {
                    lyricsPlusRepository.resolve(
                        track,
                        pluginLyrics,
                        effectiveAppLanguage(),
                        romajiEnabled = useRomaji,
                        translationEnabled = useTranslation)
                } catch (e: Exception) {
                    null
                }

                if (currentTrackId != track.id) return@launch

                _uiState.update {
                    it.copy(
                        syncedLyrics = plusResult?.syncedLyrics ?: pluginLyrics?.syncedLyrics,
                        plainLyrics = plusResult?.plainLyrics ?: pluginLyrics?.plainLyrics,
                        romajiLines = plusResult?.romajiLines,
                        translationLines = plusResult?.translationLines,
                        romajiEnabled = useRomaji,
                        translationEnabled = useTranslation,
                        isLoading = false,
                        isSearchingFallback = false)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, isSearchingFallback = false, error = e.message)
                }
            }
        }
    }

    /** The language the UI runs in: the settings pick, or the system language. */
    private fun effectiveAppLanguage(): String {
        val language = settingsProvider.settingsState.value?.language
            ?: SupportedLanguages.SYSTEM
        return if (language == SupportedLanguages.SYSTEM) {
            systemLanguageCode() ?: "en"
        } else {
            language.languageTag ?: language.locale
        }
    }

    init {
        viewModelScope.launch {
            audioPlayerQueue.currentQueueEntryFlow.collect { entry ->
                val trackId = when (entry) {
                    is QueueEntry.StreamingTrack -> entry.track.id
                    else -> null
                }
                if (entry is QueueEntry.StreamingTrack) {
                    val track = entry.track
                    _uiState.update {
                        it.copy(
                            trackTitle = track.title,
                            trackArtists = track.artists.joinToString(", ") { artist -> artist.name },
                            coverUrl = (track.album?.thumbnails ?: track.thumbnails)
                                ?.firstOrNull()?.url.orEmpty()
                        )
                    }
                }
                if (trackId != null && trackId != currentTrackId) {
                    loadLyrics()
                }
            }
        }
    }

    fun seekToLine(line: LyricsLine) {
        viewModelScope.launch {
            audioPlayer.seekTo(line.time.milliseconds)
        }
    }

    // Player rail controls (the stage carries its own transport, per mock).
    fun togglePlayPause() {
        viewModelScope.launch {
            if (audioPlayer.playerStateFlow.value == PlayerState.PLAYING) {
                audioPlayer.pause()
            } else {
                audioPlayer.play()
            }
        }
    }

    fun skipToNext() {
        viewModelScope.launch { audioPlayer.skipToNext() }
    }

    fun skipToPrevious() {
        viewModelScope.launch { audioPlayer.skipToPrevious() }
    }

    fun toggleShuffle() {
        viewModelScope.launch { audioPlayer.shuffle(!audioPlayer.shuffleModeFlow.value) }
    }

    fun cycleLoop() {
        viewModelScope.launch { audioPlayer.loop(audioPlayer.loopStateFlow.value.next()) }
    }

    /** Stage pill: flip the romaji layer and re-resolve with the new flag. */
    fun setRomajiEnabled(enabled: Boolean) {
        _uiState.update { it.copy(romajiEnabled = enabled) }
        loadLyrics(forceReload = true, romajiEnabled = enabled)
    }

    /** Stage pill: flip the translation layer and re-resolve likewise. */
    fun setTranslationEnabled(enabled: Boolean) {
        _uiState.update { it.copy(translationEnabled = enabled) }
        loadLyrics(forceReload = true, translationEnabled = enabled)
    }

    private fun activeLineIndex(lyrics: List<LyricsLine>?, positionMillis: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        val index = lyrics.indexOfLast { it.time <= positionMillis }
        return index.coerceAtLeast(0)
    }
}
