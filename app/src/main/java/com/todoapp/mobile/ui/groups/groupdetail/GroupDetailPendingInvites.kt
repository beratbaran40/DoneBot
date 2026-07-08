package com.todoapp.mobile.ui.groups.groupdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.todoapp.mobile.R
import com.todoapp.mobile.ui.groups.groupdetail.GroupDetailContract.PendingInviteUiItem
import com.todoapp.uikit.components.TDText
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.example.uikit.R as UiKitR

/**
 * "Invited" section of the Members tab: outgoing invitations that are still pending. Deliberately
 * faint next to the real member cards — these people aren't in the group yet (tester feedback:
 * pending invites were invisible anywhere in the app). View-only in v1 (no cancel affordance).
 */
@Composable
internal fun GroupDetailPendingInvitesSection(invites: List<PendingInviteUiItem>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))
        TDText(
            text = "${invites.size} ${stringResource(R.string.pending_invites_section).uppercase()}",
            style = TDTheme.typography.subheading1,
            color = TDTheme.colors.gray,
        )
        Spacer(modifier = Modifier.height(12.dp))
        invites.forEach { invite ->
            PendingInviteRow(invite = invite)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PendingInviteRow(invite: PendingInviteUiItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TDTheme.colors.lightPending.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(TDTheme.colors.pendingGray.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(UiKitR.drawable.ic_mail),
                contentDescription = null,
                tint = TDTheme.colors.darkPending,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            TDText(
                text = invite.email,
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.onBackground,
            )
            TDText(
                text = invite.invitedAt,
                style = TDTheme.typography.subheading4,
                color = TDTheme.colors.gray,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(TDTheme.colors.pendingGray.copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            TDText(
                text = stringResource(R.string.invite_pending_chip),
                style = TDTheme.typography.subheading1,
                color = TDTheme.colors.darkPending,
            )
        }
    }
}

@TDPreview
@Composable
private fun GroupDetailPendingInvitesSectionPreview() {
    TDTheme {
        GroupDetailPendingInvitesSection(invites = mockPendingInvites)
    }
}

@TDPreview
@Composable
private fun GroupDetailPendingInviteSingleRowPreview() {
    TDTheme {
        GroupDetailPendingInvitesSection(
            invites = listOf(
                PendingInviteUiItem(id = 9L, email = "very.long.email.address@example-domain.com", invitedAt = "8 Jul 2026"),
            ),
        )
    }
}
