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
import org.spotube.spotube.AppBuildInfo
import org.spotube.spotube.modules.settings.components.SettingCardItem
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxDirectboxReceive
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.spotube.spotube.resources.iconsax.IconsaxMicrophone
import org.spotube.spotube.resources.iconsax.IconsaxMusicPlay
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_about_description
import spotube.composeapp.generated.resources.settings_about_feature_lyrics_subtitle
import spotube.composeapp.generated.resources.settings_about_feature_lyrics_title
import spotube.composeapp.generated.resources.settings_about_feature_offline_subtitle
import spotube.composeapp.generated.resources.settings_about_feature_offline_title
import spotube.composeapp.generated.resources.settings_about_feature_open_subtitle
import spotube.composeapp.generated.resources.settings_about_feature_open_title
import spotube.composeapp.generated.resources.settings_about_version
import spotube.composeapp.generated.resources.settings_section_about
import spotube.composeapp.generated.resources.welcome_app_name

/**
 * About Hodai: app identity, version and a short description. Nothing else —
 * the source-code and license links live in the Plugins (Advanced) section
 * under "Source code". Replaces the former Support section — Hodai ships
 * without donation destinations.
 */
internal fun LazyListScope.aboutSection(
    includeHeader: Boolean = true) {
    if (includeHeader) {
        settingsSectionHeader(Res.string.settings_section_about)
    }
    settingsSectionCard(
        items = listOf(
            {
                SettingCardItem(
                    title = stringResource(Res.string.welcome_app_name),
                    subtitle = stringResource(
                        Res.string.settings_about_version,
                        AppBuildInfo.VERSION),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxInformation,
                            contentDescription = stringResource(
                                Res.string.settings_section_about))
                    })
            },
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_about_description),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxInformation,
                            contentDescription = stringResource(
                                Res.string.settings_section_about))
                    })
            },
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_about_feature_lyrics_title),
                    subtitle = stringResource(Res.string.settings_about_feature_lyrics_subtitle),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxMicrophone,
                            contentDescription = stringResource(
                                Res.string.settings_about_feature_lyrics_title))
                    })
            },
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_about_feature_offline_title),
                    subtitle = stringResource(Res.string.settings_about_feature_offline_subtitle),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxDirectboxReceive,
                            contentDescription = stringResource(
                                Res.string.settings_about_feature_offline_title))
                    })
            },
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_about_feature_open_title),
                    subtitle = stringResource(Res.string.settings_about_feature_open_subtitle),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxMusicPlay,
                            contentDescription = stringResource(
                                Res.string.settings_about_feature_open_title))
                    })
            })
    )
}
