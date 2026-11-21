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

package org.spotube.spotube.core.ui.theming

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import org.spotube.spotube.modules.settings.Theme
import org.spotube.spotube.modules.settings.UserSettings
import org.spotube.millennium.composables.MillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.MillenniumTooltipHost
import org.spotube.millennium.composables.MillenniumTopAppBarTheme
import org.spotube.millennium.composables.MillenniumTopAppBarThemeProperties
import org.spotube.millennium.composables.defaultMillenniumModalBottomSheetTheme
import org.spotube.millennium.composables.defaultMillenniumTopAppBarTheme
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumColors
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumLightThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.MillenniumTheme
import org.spotube.millennium.theme.MillenniumThemeConfig

/**
 * Semantic color roles used across the app, resolved from the active Millennium palette.
 *
 * This is not a second palette: every role is derived from [MillenniumColors], so the
 * whole app follows the selected Millennium accent family and light/dark appearance.
 */
@Immutable
data class AppColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val scrim: Color
)

/** Maps Millennium's semantic palette onto the app's role names. */
fun appColorsFrom(m: MillenniumColors): AppColors = AppColors(
    primary = m.accent,
    onPrimary = m.onAccent,
    primaryContainer = m.raised,
    onPrimaryContainer = m.text,
    secondary = m.accent,
    onSecondary = m.onAccent,
    secondaryContainer = m.raised,
    onSecondaryContainer = m.text,
    tertiaryContainer = m.raised,
    onTertiaryContainer = m.text,
    background = m.background,
    onBackground = m.text,
    surface = m.surface,
    onSurface = m.text,
    surfaceVariant = m.inset,
    onSurfaceVariant = m.muted,
    surfaceContainer = m.surface,
    surfaceContainerHigh = m.raised,
    surfaceContainerHighest = m.raised,
    inverseSurface = m.text,
    inverseOnSurface = m.background,
    outline = m.border,
    outlineVariant = m.border,
    error = m.danger,
    onError = m.background,
    errorContainer = m.danger.copy(alpha = 0.16f),
    onErrorContainer = m.danger,
    scrim = m.scrim
)


/**
 * Hodai brand colour sets for the Millennium library itself, locked by the
 * owner on 2026-11-22: deep green #183630, bone cream #E3DAC9, soft gold
 * #E5C690. These replace the Lime family in [MillenniumColorSchemes], so the
 * library's own components (buttons, sliders, sidebar gel) wear the brand
 * colours too, not just screens reading AppTheme.colors. The gold gel runs
 * from #EFDBAC (top) to #C79F5D (bottom), matching the logo graduation.
 */
private val HodaiDarkColors = MillenniumColors(
    background = Color(0xFF122A21), surface = Color(0xFF183630),
    raised = Color(0xFF1F443B), inset = Color(0xFF0E241C),
    text = Color(0xFFE3DAC9), muted = Color(0xFF93AC9E),
    border = Color(0xFF2A5546), highlight = Color(0xFF3E7A63),
    accent = Color(0xFFE5C690), accentTop = Color(0xFFEFDBAC),
    accentBottom = Color(0xFFC79F5D), onAccent = Color(0xFF14332A),
    success = Color(0xFF8FD0A8), danger = Color(0xFFFF98A9),
    scrim = Color(0xB30A1F18)
)

private val HodaiLightColors = MillenniumColors(
    background = Color(0xFFE3DAC9), surface = Color(0xFFEFE9DC),
    raised = Color(0xFFF7F3E8), inset = Color(0xFFD8CDB6),
    text = Color(0xFF183630), muted = Color(0xFF5F7A6E),
    border = Color(0xFFB4A888), highlight = Color(0xFFFFFFFF),
    accent = Color(0xFF8C6D1F), accentTop = Color(0xFFEFDBAC),
    accentBottom = Color(0xFFC79F5D), onAccent = Color(0xFF14332A),
    success = Color(0xFF257B51), danger = Color(0xFFB42332),
    scrim = Color(0x66183630)
)

/**
 * Hodai brand palette, locked by the owner on 2026-11-22: deep green #183630,
 * bone cream #E3DAC9, soft gold #E5C690. Applied over the Millennium semantic
 * roles so every screen that reads AppTheme.colors wears the brand colours.
 */
private fun AppColors.withHodaiPalette(dark: Boolean): AppColors = if (dark) {
    copy(
        primary = Color(0xFFE5C690),
        onPrimary = Color(0xFF14332A),
        primaryContainer = Color(0xFF1F443B),
        onPrimaryContainer = Color(0xFFE3DAC9),
        secondary = Color(0xFFE5C690),
        onSecondary = Color(0xFF14332A),
        secondaryContainer = Color(0xFF1F443B),
        onSecondaryContainer = Color(0xFFE3DAC9),
        tertiaryContainer = Color(0xFF1F443B),
        onTertiaryContainer = Color(0xFFE3DAC9),
        background = Color(0xFF183630),
        onBackground = Color(0xFFE3DAC9),
        surface = Color(0xFF183630),
        onSurface = Color(0xFFE3DAC9),
        surfaceVariant = Color(0xFF122A21),
        onSurfaceVariant = Color(0xFF93AC9E),
        surfaceContainer = Color(0xFF183630),
        surfaceContainerHigh = Color(0xFF1F443B),
        surfaceContainerHighest = Color(0xFF26503F),
        inverseSurface = Color(0xFFE3DAC9),
        inverseOnSurface = Color(0xFF183630),
        outline = Color(0xFF2A5546),
        outlineVariant = Color(0xFF2A5546)
    )
} else {
    copy(
        primary = Color(0xFFE5C690),
        onPrimary = Color(0xFF14332A),
        primaryContainer = Color(0xFFEFE9DC),
        onPrimaryContainer = Color(0xFF183630),
        secondary = Color(0xFFE5C690),
        onSecondary = Color(0xFF14332A),
        secondaryContainer = Color(0xFFEFE9DC),
        onSecondaryContainer = Color(0xFF183630),
        tertiaryContainer = Color(0xFFEFE9DC),
        onTertiaryContainer = Color(0xFF183630),
        background = Color(0xFFE3DAC9),
        onBackground = Color(0xFF183630),
        surface = Color(0xFFE3DAC9),
        onSurface = Color(0xFF183630),
        surfaceVariant = Color(0xFFD8CDB6),
        onSurfaceVariant = Color(0xFF5F7A6E),
        surfaceContainer = Color(0xFFE3DAC9),
        surfaceContainerHigh = Color(0xFFEFE9DC),
        surfaceContainerHighest = Color(0xFFF7F3E8),
        inverseSurface = Color(0xFF183630),
        inverseOnSurface = Color(0xFFE3DAC9),
        outline = Color(0xFFB4A888),
        outlineVariant = Color(0xFFB4A888)
    )
}

/** Named text styles used by the app, independent of Material typography. */
@Immutable
data class AppTypography(
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal
): TextStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight
)

val AppDefaultTypography = AppTypography(
    displaySmall = style(36, 44),
    headlineLarge = style(32, 40),
    headlineMedium = style(28, 36),
    headlineSmall = style(24, 32),
    titleLarge = style(22, 28, FontWeight.Medium),
    titleMedium = style(16, 24, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium)
)

/** Corner shapes used by the app. */
@Immutable
data class AppShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape
)

val AppDefaultShapes = AppShapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

val LocalAppColors =
    staticCompositionLocalOf { appColorsFrom(org.spotube.millennium.theme.MillenniumLightColors) }
val LocalAppTypography = staticCompositionLocalOf { AppDefaultTypography }
val LocalAppShapes = staticCompositionLocalOf { AppDefaultShapes }

/** Reads the active app theme tokens, all backed by Millennium. */
object AppTheme {
    val colors: AppColors @Composable get() = LocalAppColors.current
    val typography: AppTypography @Composable get() = LocalAppTypography.current
    val shapes: AppShapes @Composable get() = LocalAppShapes.current
}

/**
 * The app's single source of truth for colour.
 *
 * Installs [MillenniumTheme] with the user's selected [UserSettings.palette] and light/dark
 * [UserSettings.theme], then derives the app's semantic [AppColors], [AppTypography] and
 * [AppShapes] tokens from the active Millennium theme.
 */
@Composable
fun SpotubeTheme(
    settings: UserSettings,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (settings.theme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemDarkTheme()
    }
    val palette: MillenniumPalette = settings.palette

    val darkTheme = MillenniumDarkThemeComponents
    val lightTheme = MillenniumLightThemeComponents

    MillenniumTheme(
        dark = isDarkTheme,
        config = MillenniumThemeConfig(
            palette = palette,
            colorSchemes = MillenniumColorSchemes(
                limeLight = HodaiLightColors,
                limeDark = HodaiDarkColors
            ),
            dark = darkTheme.copy(
                topAppBarTheme = MillenniumTopAppBarTheme(
                    properties = darkTheme.topAppBarTheme.properties.copy(
                        separatorColor = Color.Transparent
                    )
                ),
                modalBottomSheetTheme = compactModalBottomSheetTheme()
            ),
            light = lightTheme.copy(
                topAppBarTheme = MillenniumTopAppBarTheme(
                    properties = lightTheme.topAppBarTheme.properties.copy(
                        separatorColor = Color.Transparent
                    )
                ),
                modalBottomSheetTheme = compactModalBottomSheetTheme()
            )
        )
    ) {
        val colors = appColorsFrom(MillenniumTheme.colors).withHodaiPalette(isDarkTheme)
        MillenniumTooltipHost {
            CompositionLocalProvider(
                LocalAppColors provides colors,
                LocalAppTypography provides AppDefaultTypography,
                LocalAppShapes provides AppDefaultShapes,
                content = content
            )
        }
    }
}

/**
 * Forces the Hodai dark appearance for one region of the tree, whatever the
 * app-wide appearance is. The sidebar and the large player are brand anchors:
 * they stay deep green in light mode too (owner lock, 2026-11-22).
 *
 * The nested [MillenniumTheme] re-skins the library components in the region,
 * but [LocalAppColors] does not follow a nested theme, so the dark app tokens
 * are provided explicitly alongside it.
 */
@Composable
fun HodaiDarkRegion(content: @Composable () -> Unit) {
    MillenniumTheme(dark = true, config = MillenniumTheme.config) {
        CompositionLocalProvider(
            LocalAppColors provides appColorsFrom(MillenniumTheme.colors).withHodaiPalette(true),
            content = content
        )
    }
}

/** Library default is 24dp padding + 16dp spacing, which is far too roomy on phones. */
private fun compactModalBottomSheetTheme(): MillenniumModalBottomSheetTheme {
    val padding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)
    fun org.spotube.millennium.composables.MillenniumModalBottomSheetThemeProperties.compact() =
        copy(contentPadding = padding, contentSpacing = 8.dp)
    val states = defaultMillenniumModalBottomSheetTheme().states
    return MillenniumModalBottomSheetTheme(
        states = org.spotube.millennium.theme.MillenniumControlStateProperty(
            default = states.default.compact(),
            hovered = states.hovered.compact(),
            focused = states.focused.compact(),
            pressed = states.pressed.compact(),
            disabled = states.disabled.compact(),
            selected = states.selected.compact(),
            error = states.error.compact()))
}

/** Whether the active Millennium appearance is light. */
@Composable
fun isLightAppearance(): Boolean = MillenniumTheme.colors.background.luminance() > 0.5f
