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

package org.spotube.spotube.core.ui.component.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.composeunstyled.Text
import org.spotube.spotube.core.ui.theming.AppTheme

/**
 * Wide photo card for the Hodai home "Featured" row (owner-locked mock v11):
 * the artwork fills the whole card and a caption chip ("Title - Artist") sits
 * on a dark translucent pill at the bottom start. Unlike [PlayableCard] there
 * is no text block below the artwork.
 */
@Composable
fun FeaturedPhotoCard(
    title: String,
    subtitle: String? = null,
    imageURL: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(264.dp)
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.surfaceVariant)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        if (imageURL != null) {
            AsyncImage(
                model = imageURL,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A1A14).copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = if (subtitle.isNullOrEmpty()) title else "$title - $subtitle",
                style = AppTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE3DAC9),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
