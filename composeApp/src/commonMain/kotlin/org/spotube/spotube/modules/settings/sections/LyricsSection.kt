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

package org.spotube.spotube.modules.settings.sections

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.spotube.spotube.modules.settings.SettingsViewModel
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.spotube.modules.settings.components.SwitchSettingCard
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxLanguageSquare
import org.spotube.spotube.resources.iconsax.IconsaxMicrophone
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_lyrics_romaji_subtitle
import spotube.composeapp.generated.resources.settings_lyrics_romaji_title
import spotube.composeapp.generated.resources.settings_lyrics_translation_subtitle
import spotube.composeapp.generated.resources.settings_lyrics_translation_title
import spotube.composeapp.generated.resources.settings_section_lyrics

/** Hodai Lyrics Plus preferences: the romaji and translation layers. */
internal fun LazyListScope.lyricsSection(
    settings: UserSettings,
    settingsViewModel: SettingsViewModel) {

    val cards: List<@Composable () -> Unit> = buildList {
        add {
            SwitchSettingCard(
                title = stringResource(Res.string.settings_lyrics_romaji_title),
                subtitle = stringResource(Res.string.settings_lyrics_romaji_subtitle),
                icon = {
                    SettingsItemIcon(
                        Iconsax.IconsaxMicrophone,
                        stringResource(Res.string.settings_lyrics_romaji_title)
                    )
                },
                checked = settings.lyricsRomajiEnabled,
                onCheckedChange = { enabled ->
                    settingsViewModel.updateSettings {
                        copy(lyricsRomajiEnabled = enabled)
                    }
                }
            )
        }
        add {
            SwitchSettingCard(
                title = stringResource(Res.string.settings_lyrics_translation_title),
                subtitle = stringResource(Res.string.settings_lyrics_translation_subtitle),
                icon = {
                    SettingsItemIcon(
                        Iconsax.IconsaxLanguageSquare,
                        stringResource(Res.string.settings_lyrics_translation_title)
                    )
                },
                checked = settings.lyricsTranslationEnabled,
                onCheckedChange = { enabled ->
                    settingsViewModel.updateSettings {
                        copy(lyricsTranslationEnabled = enabled)
                    }
                }
            )
        }
    }

    settingsSectionHeader(Res.string.settings_section_lyrics)
    settingsSectionCard(items = cards)
}
