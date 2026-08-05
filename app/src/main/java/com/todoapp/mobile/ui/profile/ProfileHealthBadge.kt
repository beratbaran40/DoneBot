package com.todoapp.mobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.common.heartsLabel
import com.todoapp.mobile.domain.repository.HEART_COUNT
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme

private val HEART_ICON_SIZE = 14.dp

/**
 * Heart + remaining-hearts count, sized to sit on the corner of the profile avatar.
 *
 * This replaced a segmented ring around the avatar. The ring drew one segment per half-heart — 24 of
 * them — so each was a 12° sliver, and rendering it in `heartFull` red made a full-health avatar
 * look like it was in an error state. A badge carries the same number without touching the portrait.
 *
 * Styled after `TDStatusChip`: same shape slot, same padding rhythm. The border matters here and
 * not on a normal chip because the badge overhangs the avatar, so it needs an edge against
 * arbitrary photo content underneath.
 *
 * The heart is `ic_heart`; [tdPainter] swaps in the 8-Bit sprite on a pixel kit on its own, so no
 * `when (palette)` is needed. Shown in every kit — hearts are a product feature, not a kit style.
 */
@Composable
fun ProfileHealthBadge(
    halfHearts: Int,
    modifier: Modifier = Modifier,
) {
    val label = heartsLabel(halfHearts)
    val description = stringResource(R.string.activity_health_content_description, label, HEART_COUNT)
    Row(
        modifier = modifier
            .clip(TDTheme.shapes.pill)
            .background(TDTheme.colors.background)
            .border(TDTheme.style.borderWidth, TDTheme.colors.lightGray, TDTheme.shapes.pill)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = tdPainter(com.example.uikit.R.drawable.ic_heart),
            contentDescription = null,
            tint = TDTheme.colors.heartFull,
            modifier = Modifier.size(HEART_ICON_SIZE),
        )
        TDText(
            text = label,
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.onBackground,
        )
    }
}

@TDPreview
@Composable
private fun ProfileHealthBadgeStatesPreview() {
    TDTheme {
        Row(
            modifier = Modifier
                .background(TDTheme.colors.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Full, an odd count (renders the ½), a low count, and empty.
            listOf(24, 13, 4, 0).forEach { hearts ->
                ProfileHealthBadge(halfHearts = hearts)
            }
        }
    }
}

@TDPreview
@Composable
private fun ProfileHealthBadgeOnAvatarPreview() {
    TDTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(TDTheme.shapes.circle)
                    .background(TDTheme.colors.lightPending),
                contentAlignment = Alignment.Center,
            ) {
                TDText(
                    text = "BB",
                    style = TDTheme.typography.heading4,
                    color = TDTheme.colors.pendingGray,
                )
            }
            ProfileHealthBadge(
                halfHearts = 13,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}
