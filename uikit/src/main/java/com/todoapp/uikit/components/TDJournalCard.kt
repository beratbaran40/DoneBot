package com.todoapp.uikit.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.paperBackground

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TDJournalCard(
    dateLabel: String,
    moodEmoji: String?,
    title: String?,
    contentPreview: String,
    photoCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .paperBackground(lineSpacing = 22.dp, marginX = 20.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 28.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TDText(
                text = dateLabel,
                style = TDTheme.typography.subheading2,
                color = TDTheme.colors.darkPending,
            )
            if (moodEmoji != null) {
                TDText(
                    text = moodEmoji,
                    style = TDTheme.typography.heading3,
                )
            }
        }
        if (!title.isNullOrBlank()) {
            TDText(
                text = title,
                style = TDTheme.typography.heading4,
                color = TDTheme.colors.black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (contentPreview.isNotBlank()) {
            TDText(
                text = contentPreview,
                style = TDTheme.typography.regularTextStyle,
                color = TDTheme.colors.black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (photoCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_image),
                    contentDescription = null,
                    tint = TDTheme.colors.darkPending,
                    modifier = Modifier.size(14.dp),
                )
                TDText(
                    text = stringResource(R.string.td_journal_card_photo_count, photoCount),
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.darkPending,
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdJournalCardFullPreview() {
    TDTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            TDJournalCard(
                dateLabel = "Today",
                moodEmoji = "😊",
                title = "A great morning",
                contentPreview = "Started the day with a long walk and a fresh cup of coffee. Energy is high.",
                photoCount = 2,
                onClick = {},
            )
            Spacer(modifier = Modifier.height(12.dp))
            TDJournalCard(
                dateLabel = "Yesterday",
                moodEmoji = "😐",
                title = null,
                contentPreview = "Quiet evening. Read a chapter and called mom.",
                photoCount = 0,
                onClick = {},
            )
            Spacer(modifier = Modifier.height(12.dp))
            TDJournalCard(
                dateLabel = "11 May",
                moodEmoji = null,
                title = "Long, long entry",
                contentPreview = "Once upon a time there was a very long journal entry that went on and on and on and never seemed to end no matter how much you scrolled or how patient you were.",
                photoCount = 5,
                onClick = {},
            )
        }
    }
}
