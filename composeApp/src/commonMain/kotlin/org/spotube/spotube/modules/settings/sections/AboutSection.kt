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
import org.spotube.spotube.openUrlInBrowser
import org.spotube.spotube.resources.iconsax.CarbonGithubLogo
import org.spotube.spotube.resources.iconsax.Iconsax
import org.spotube.spotube.resources.iconsax.IconsaxInformation
import org.jetbrains.compose.resources.stringResource
import spotube.composeapp.generated.resources.Res
import spotube.composeapp.generated.resources.settings_about_description
import spotube.composeapp.generated.resources.settings_about_open_source_subtitle
import spotube.composeapp.generated.resources.settings_about_open_source_title
import spotube.composeapp.generated.resources.settings_about_repository_subtitle
import spotube.composeapp.generated.resources.settings_about_repository_title
import spotube.composeapp.generated.resources.settings_about_version
import spotube.composeapp.generated.resources.settings_section_about
import spotube.composeapp.generated.resources.welcome_app_name

/**
 * About Hodai: app identity and version, a short description, the open-source
 * credit (the one place the upstream project is named, as the AGPL requires)
 * and a link to the source repository. Replaces the former Support section —
 * Hodai ships without donation destinations.
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
                    title = stringResource(Res.string.settings_about_open_source_title),
                    subtitle = stringResource(Res.string.settings_about_open_source_subtitle),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.IconsaxInformation,
                            contentDescription = stringResource(
                                Res.string.settings_about_open_source_title))
                    })
            },
            {
                SettingCardItem(
                    title = stringResource(Res.string.settings_about_repository_title),
                    subtitle = stringResource(Res.string.settings_about_repository_subtitle),
                    icon = {
                        SettingsItemIcon(
                            imageVector = Iconsax.CarbonGithubLogo,
                            contentDescription = stringResource(
                                Res.string.settings_about_repository_title))
                    },
                    onClick = { openUrlInBrowser(AppBuildInfo.REPOSITORY_URL) })
            })
    )
}
