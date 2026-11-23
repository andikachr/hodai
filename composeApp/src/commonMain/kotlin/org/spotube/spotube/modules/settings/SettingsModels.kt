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

package org.spotube.spotube.modules.settings

import org.spotube.plugin_interfaces.plugin_apis.audio.AudioFormat
import org.spotube.plugin_interfaces.plugin_apis.audio.AudioQuality
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.spotube.core.i18n.systemCountryCode

@Serializable
enum class Theme {
    LIGHT, DARK, SYSTEM
}

@Serializable
data class UserSettings(
    // Language and Region
    // Stored under a new name: the old "language" field was persisted as EN for everyone while
    // it had no effect, so reading it would force English on existing users.
    @SerialName("appLanguage")
    val language: SupportedLanguages = SupportedLanguages.SYSTEM,
    val country: CountryCode = defaultCountryCode(),

    // Appearance
    val theme: Theme = Theme.SYSTEM,
    val palette: MillenniumPalette = MillenniumPalette.Lime,

    // Playback
    val streamingMusicFormat: AudioFormat = defaultStreamingMusicFormat(),
    val streamingMusicQuality: AudioQuality = AudioQuality.Lossy(bitrate = 256_000),
    val enableMusicCaching: Boolean = true,
    val cacheFolder: String? = null,
    val cacheSizeLimitMB: Long = -1L,
    val enableEndlessPlayback: Boolean = true,
    val playbackProxyServerPort: Int = 14769,

    // Lyrics Plus (Hodai layered lyrics: romaji + per-line translation)
    val lyricsRomajiEnabled: Boolean = true,
    val lyricsTranslationEnabled: Boolean = true,
    // Karaoke: word-timed lyrics (KuGou KRC) as the preferred Lirik+ source
    val lyricsKaraokeEnabled: Boolean = true,

    // Desktop equalizer (VLC presets, applied by the JVM player)
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: String = "Flat",

    // Floating lyrics overlay window (desktop)
    val floatingLyricsEnabled: Boolean = false,

    // Remote Control (LAN)
    val allowRemoteControl: Boolean = false,
    val allowedRemoteDevices: List<String> = emptyList(),
    val remoteControlDeviceName: String = "",
    val remoteControlDeviceId: String = "",

    // Group Jam
    val jamParticipantName: String = "",
    /** Room server (https://github.com/team-spotube/jam-server); self-hosters point this at their own deployment. */
    val jamServerUrl: String = DEFAULT_JAM_SERVER_URL,
    val lastJamCode: String = "",
    /** Random per-install secret behind this device's jam identities (see `jamParticipantKey`). */
    val jamDeviceKey: String = "",

    // Downloads
    val overloadedDownloadFolder: String? = null, // When null, uses default music folder
    val localMediaFolders: List<String> = emptyList(),
    val downloadMusicFormat: AudioFormat = AudioFormat(
        codec = "aac",
        container = "mp4",
        qualities = listOf(
            AudioQuality.Lossy(bitrate = 44_000),
            AudioQuality.Lossy(bitrate = 96_000),
            AudioQuality.Lossy(bitrate = 128_000),
            AudioQuality.Lossy(bitrate = 256_000))
    ),
    val downloadMusicQuality: AudioQuality = AudioQuality.Lossy(bitrate = 256_000),

    // Desktop
    val minimizeToTray: Boolean = false,
    val discordRichPresence: Boolean = true,

    // Updates
    val autoCheckForUpdates: Boolean = true,
    val ignoredAppUpdateVersion: String? = null,
    val ignoredPluginUpdateVersions: Map<String, String> = emptyMap(),

    // Support
    val supportRemindersEnabled: Boolean = true,
    val lastSupportReminderEpochMs: Long = 0L)

/** The public Group Jam room server. */
const val DEFAULT_JAM_SERVER_URL = "https://jam.spotube.cc"

/**
 * Region for fresh installs: the country of the device's system locale, so a
 * new user starts in their own region instead of the upstream default (BD).
 * Falls back to US when the system reports no usable country. Persisted
 * settings are unaffected — this only supplies the initial default.
 */
internal fun defaultCountryCode(): CountryCode =
    systemCountryCode()
        ?.let { code -> CountryCode.entries.firstOrNull { it.code.equals(code, ignoreCase = true) } }
        ?: CountryCode.US

/** Whether [url] can be a room server address (http(s), or ws(s) for self-hosters). */
internal fun isJamServerUrl(url: String): Boolean =
    Regex("^(https?|wss?)://[^\\s/?#]+(/\\S*)?$", RegexOption.IGNORE_CASE).matches(url.trim())

/** opus/webm on every platform: all of their players (ExoPlayer, VLC) can play it. */
internal fun defaultStreamingMusicFormat(): AudioFormat = AudioFormat(
    codec = "opus",
    container = "webm",
    qualities = listOf(
        AudioQuality.Lossy(bitrate = 44_000),
        AudioQuality.Lossy(bitrate = 96_000),
        AudioQuality.Lossy(bitrate = 128_000),
        AudioQuality.Lossy(bitrate = 256_000)))

/** LibVLC equalizer preset names, spelled the way LibVLC knows them. */
val EQUALIZER_PRESETS: List<String> = listOf(
    "Flat", "Classical", "Club", "Dance", "Full Bass", "Full Bass & Treble",
    "Full Treble", "Headphones", "Large Hall", "Live", "Party", "Pop",
    "Reggae", "Rock", "Ska", "Soft", "Soft Rock", "Techno")
