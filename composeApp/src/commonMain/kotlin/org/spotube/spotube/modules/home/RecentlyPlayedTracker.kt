package org.spotube.spotube.modules.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.koin.core.component.KoinComponent
import org.spotube.plugin_interfaces.plugin_apis.metadata.track.MetadataTrack
import org.spotube.spotube.core.audioplayer.AudioPlayerQueue
import org.spotube.spotube.core.audioplayer.QueueEntry
import org.spotube.spotube.core.di.injectLogger
import org.spotube.spotube.core.paths.Paths
import kotlin.time.Clock

@Serializable
data class RecentPlayEntry(
    val track: MetadataTrack,
    val playedAtEpochMs: Long)

/**
 * Hodai "Terakhir diputar": watches the queue's current entry and keeps the
 * last [MAX_RECENTS] streaming tracks, most recent first, in a JSON file in
 * the app data dir. Local files are skipped (they have no stable identity to
 * replay). All IO failures are swallowed: history must never break playback.
 */
class RecentlyPlayedTracker(
    private val audioPlayerQueue: AudioPlayerQueue,
    private val paths: Paths,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) :
    KoinComponent {

    private val logger by injectLogger<RecentlyPlayedTracker>()
    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val json: Json = Json { ignoreUnknownKeys = true }

    private val entries = MutableStateFlow<List<RecentPlayEntry>>(emptyList())

    /** Most recent first. */
    val recents: StateFlow<List<MetadataTrack>> =
        entries
            .map { list -> list.map { it.track } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private var lastRecordedTrackId: String? = null

    init {
        scope.launch {
            entries.value = load()
            lastRecordedTrackId = entries.value.firstOrNull()?.track?.id
            audioPlayerQueue.currentQueueEntryFlow.collect { entry ->
                val track = (entry as? QueueEntry.StreamingTrack)?.track
                    ?: return@collect
                if (track.id == lastRecordedTrackId) return@collect
                lastRecordedTrackId = track.id
                val updated =
                    (listOf(
                        RecentPlayEntry(
                            track = track,
                            playedAtEpochMs = Clock.System.now().toEpochMilliseconds())) +
                        entries.value.filterNot { it.track.id == track.id })
                        .take(MAX_RECENTS)
                entries.value = updated
                save(updated)
            }
        }
    }

    private fun file(): Path =
        paths.getApplicationDataDirPath().toPath() / FILE_NAME

    private fun load(): List<RecentPlayEntry> =
        try {
            val file = file()
            if (!fileSystem.exists(file)) {
                emptyList()
            } else {
                fileSystem.source(file).buffer().use { source ->
                    json.decodeFromString<List<RecentPlayEntry>>(source.readUtf8())
                }
            }
        } catch (e: Exception) {
            logger.d { "Recently played load failed: ${e.message}" }
            emptyList()
        }

    private fun save(list: List<RecentPlayEntry>) {
        try {
            val file = file()
            file.parent?.let { fileSystem.createDirectories(it) }
            fileSystem.sink(file).buffer().use { sink ->
                sink.writeUtf8(json.encodeToString(list))
            }
        } catch (e: Exception) {
            logger.d { "Recently played save failed: ${e.message}" }
        }
    }

    companion object {
        private const val MAX_RECENTS = 20
        private const val FILE_NAME = "recently_played.json"
    }
}
