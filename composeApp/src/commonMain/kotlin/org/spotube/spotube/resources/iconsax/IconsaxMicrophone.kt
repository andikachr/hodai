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

package org.spotube.spotube.resources.iconsax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Microphone glyph drawn for Hodai's lyrics button (the Iconsax set ships no
 * microphone). Same construction pattern as the other icons in this package:
 * white fills on a 24x24 viewport, clipped to the viewport group.
 */
val Iconsax.IconsaxMicrophone: ImageVector
    get() {
        if (_IconsaxMicrophone != null) {
            return _IconsaxMicrophone!!
        }
        _IconsaxMicrophone = ImageVector.Builder(
            name = "IconsaxMicrophone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            group(
                clipPathData = PathData {
                    moveTo(0f, 0f)
                    horizontalLineToRelative(24f)
                    verticalLineToRelative(24f)
                    horizontalLineToRelative(-24f)
                    close()
                }
            ) {
                // Capsule: a stadium (two semicircle caps joined by straight
                // sides), drawn with the vector builder's SVG-style arcs —
                // the builder has no addRoundRect.
                path(fill = SolidColor(Color.White)) {
                    moveTo(9f, 5f)
                    arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 5f)
                    lineTo(15f, 11f)
                    arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 11f)
                    close()
                }
                // Cradle: a U-shaped band under the capsule (outer arc swept
                // one way, inner arc swept back, even-odd keeps the band).
                path(
                    fill = SolidColor(Color.White),
                    pathFillType = PathFillType.EvenOdd
                ) {
                    moveTo(5f, 10f)
                    arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = false, 19f, 10f)
                    lineTo(17f, 10f)
                    arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 7f, 10f)
                    close()
                }
                // Stem and foot as one T outline.
                path(fill = SolidColor(Color.White)) {
                    moveTo(11f, 16f)
                    lineTo(13f, 16f)
                    lineTo(13f, 20f)
                    lineTo(16f, 20f)
                    lineTo(16f, 22f)
                    lineTo(8f, 22f)
                    lineTo(8f, 20f)
                    lineTo(11f, 20f)
                    close()
                }
            }
        }.build()

        return _IconsaxMicrophone!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMicrophone: ImageVector? = null
