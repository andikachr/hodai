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

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
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
                // Capsule.
                path(fill = SolidColor(Color.White)) {
                    addRoundRect(RoundRect(9f, 2f, 15f, 14f, CornerRadius(3f, 3f)))
                }
                // Cradle: a U-shaped band under the capsule (outer arc swept
                // clockwise, inner arc swept back, even-odd keeps the band).
                path(
                    fill = SolidColor(Color.White),
                    pathFillType = PathFillType.EvenOdd
                ) {
                    moveTo(19f, 10f)
                    arcTo(Rect(5f, 3f, 19f, 17f), 0f, 180f, false)
                    lineTo(7f, 10f)
                    arcTo(Rect(7f, 5f, 17f, 15f), 180f, -180f, false)
                    close()
                }
                // Stem and foot.
                path(fill = SolidColor(Color.White)) {
                    addRoundRect(RoundRect(11f, 16f, 13f, 20f, CornerRadius(1f, 1f)))
                    addRoundRect(RoundRect(8f, 20f, 16f, 22f, CornerRadius(1f, 1f)))
                }
            }
        }.build()

        return _IconsaxMicrophone!!
    }

@Suppress("ObjectPropertyName")
private var _IconsaxMicrophone: ImageVector? = null
