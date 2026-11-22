package org.spotube.spotube.modules.lyrics

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsLine
import org.spotube.plugin_interfaces.plugin_apis.lyrics.LyricsResponse
import org.spotube.spotube.core.audioplayer.AudioPlayerInterface
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.modules.lyrics.plus.LyricsPlusRepository
import org.spotube.spotube.modules.lyrics.plus.systemLanguageCode
import org.spotube.spotube.modules.plugin.PluginManager
import org.spotube.spotube.modules.settings.SettingsProvider
import org.spotube.spotube.modules.settings.SupportedLanguages

data class FloatingLyricsState(
    val trackTitle: String = "",
    val currentLine: String? = null,
    val nextLine: String? = null,
    val romajiLine: String? = null,
    val translationLine: String? = null)

/**
 * Hodai floating lyrics (desktop): app-level line source for the overlay
 * window. Mirrors [LyricsViewModel]'s fetch (selected lyrics plugin, then
 * the Lirik+ layered resolve) but lives outside any screen, driven by the
 * queue's current entry and the player's position.
 */
class FloatingLyricsModel(
    private val pluginManager: PluginManager,
    private val audioPlayer: AudioPlayerInterface,
    private val audioPlayerQueue: AudioPlayerQueue,
    private val settingsProvider: SettingsProvider,
    private val lyricsPlusRepository: LyricsPlusRepository) : KoinComponent {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val syncedLines = MutableStateFlow<List<LyricsLine>?>(null)
    private val romajiLines = MutableStateFlow<List<String?>?>(null)
    private val translationLines = MutableStateFlow<List<String?>?>(null)
    private val title = MutableStateFlow("")

    val state: StateFlow<FloatingLyricsState> =
        combine(
            syncedLines,
            audioPlayer.positionFlow,
            romajiLines,
            translationLines,
            title) { lines, position, romaji, translation, trackTitle ->
            val index = activeLineIndex(lines, position.inWholeMilliseconds)
            FloatingLyricsState(
                trackTitle = trackTitle,
                currentLine = lines?.getOrNull(index)?.text,
                nextLine = lines?.getOrNull(index + 1)?.text,
                romajiLine = romaji?.getOrNull(index),
                translationLine = translation?.getOrNull(index))
        }.stateIn(scope, SharingStarted.Eagerly, FloatingLyricsState())

    private var currentTrackId: String? = null

    init {
        scope.launch {
            audioPlayerQueue.currentQueueEntryFlow.collect { entry ->
                val track = (entry as? QueueEntry.StreamingTrack)?.track
                if (track == null) {
                    currentTrackId = null
                    title.value = ""
                    syncedLines.value = null
                    romajiLines.value = null
                    translationLines.value = null
                    return@collect
                }
                if (track.id == currentTrackId) return@collect
                currentTrackId = track.id
                title.value = track.title
                syncedLines.value = null
                romajiLines.value = null
                translationLines.value = null
                load(track)
            }
        }
    }

    private suspend fun load(track: org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack) {
        try {
            var pluginLyrics: LyricsResponse? = null
            pluginManager.selectedLyricsPlugin.value?.let { pluginService ->
                pluginService.use {
                    pluginLyrics = lyricsAPI.getLyrics(track)
                }
            }
            val currentSettings = settingsProvider.settingsState.value
            val plusResult = try {
                lyricsPlusRepository.resolve(
                    track,
                    pluginLyrics,
                    effectiveAppLanguage(),
                    romajiEnabled = currentSettings?.lyricsRomajiEnabled ?: true,
                    translationEnabled = currentSettings?.lyricsTranslationEnabled ?: true)
            } catch (e: Exception) {
                null
            }
            if (currentTrackId != track.id) return
            syncedLines.value = plusResult?.syncedLyrics ?: pluginLyrics?.syncedLyrics
            romajiLines.value = plusResult?.romajiLines
            translationLines.value = plusResult?.translationLines
        } catch (e: Exception) {
            // The overlay simply stays on its waiting text.
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

    private fun activeLineIndex(lyrics: List<LyricsLine>?, positionMillis: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        val index = lyrics.indexOfLast { it.time <= positionMillis }
        return index.coerceAtLeast(0)
    }
}
