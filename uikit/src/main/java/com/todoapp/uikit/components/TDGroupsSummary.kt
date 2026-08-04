package com.todoapp.uikit.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.uikit.R
import com.todoapp.uikit.image.rememberPixelImageModel
import com.todoapp.uikit.image.tdPainter
import com.todoapp.uikit.image.tdPixelFilterQuality
import com.todoapp.uikit.modifier.tdShadow
import com.todoapp.uikit.previews.TDPreview
import com.todoapp.uikit.theme.TDTheme
import com.todoapp.uikit.theme.tdOutlineColor

@Composable
fun TDFamilyGroupCard(
    name: String,
    role: String,
    description: String,
    memberCount: Int,
    pendingTaskCount: Int,
    createdDate: String,
    @DrawableRes membersIcon: Int,
    @DrawableRes tasksIcon: Int,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null,
    isDragging: Boolean = false,
    isAnyDragging: Boolean = false,
    onViewDetailsClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    // Distinct target for the members stat box (e.g. jump straight to the Members tab).
    // Null keeps the box inert so existing call sites are unaffected.
    onMembersClick: (() -> Unit)? = null,
) {
    val initials =
        name
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

    val roleColor =
        when (role.uppercase()) {
            "ADMIN" -> TDTheme.colors.orange
            else -> TDTheme.colors.pendingGray
        }

    val dragScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "dragScale",
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (isAnyDragging && !isDragging) 0.72f else 1f,
        animationSpec = tween(200),
        label = "cardAlpha",
    )
    val activeBorderColor by animateColorAsState(
        targetValue = if (isDragging) TDTheme.colors.pendingGray else Color.Transparent,
        animationSpec = tween(150),
        label = "activeBorderColor",
    )
    val activeBorderWidth by animateDpAsState(
        targetValue = if (isDragging) 2.dp else 0.dp,
        animationSpec = tween(150),
        label = "activeBorderWidth",
    )

    val isDark = TDTheme.isDark
    val cardShape = TDTheme.shapes.large

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
                alpha = cardAlpha
            }.border(activeBorderWidth, activeBorderColor, cardShape)
            .padding(activeBorderWidth),
    ) {
        Card(
            modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (isDark) {
                        Modifier.border(
                            TDTheme.style.borderWidth,
                            tdOutlineColor(TDTheme.colors.lightGray.copy(alpha = 0.18f)),
                            cardShape,
                        )
                    } else {
                        Modifier.tdShadow(
                            lightShadow = TDTheme.colors.white.copy(alpha = 0.88f),
                            darkShadow = TDTheme.colors.lightGray.copy(alpha = 0.35f),
                            cornerRadius = 16.dp,
                            elevation = 7.dp,
                        )
                    },
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = TDTheme.colors.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box {
                val isAdmin = role.equals("Admin", ignoreCase = true)
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                            Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(TDTheme.colors.pendingGray),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!avatarUrl.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = rememberPixelImageModel(avatarUrl, 56.dp),
                                    filterQuality = tdPixelFilterQuality(),
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.size(56.dp).clip(CircleShape),
                                )
                            } else {
                                TDText(
                                    text = initials,
                                    color = TDTheme.colors.white,
                                    style = TDTheme.typography.heading1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            TDText(
                                text = name,
                                style = TDTheme.typography.heading3,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                color = TDTheme.colors.onBackground,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier =
                                Modifier
                                    .border(1.dp, roleColor, TDTheme.shapes.tiny)
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                            ) {
                                TDText(
                                    text =
                                    if (role.uppercase() == "ADMIN") {
                                        stringResource(R.string.role_admin)
                                    } else {
                                        stringResource(R.string.role_member)
                                    },
                                    style = TDTheme.typography.subheading4,
                                    overflow = TextOverflow.Ellipsis,
                                    maxLines = 1,
                                    color = roleColor,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TDText(
                        text = "\"$description\"",
                        style = TDTheme.typography.subheading3,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 2,
                        color = TDTheme.colors.statusCardGray,
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatBox(
                            iconRes = membersIcon,
                            iconTint = TDTheme.colors.pendingGray,
                            value = "$memberCount",
                            label = stringResource(R.string.group_card_members_label),
                            modifier = Modifier.weight(1f),
                            onClick = onMembersClick,
                        )
                        StatBox(
                            iconRes = tasksIcon,
                            iconTint = TDTheme.colors.pendingGray,
                            value = "$pendingTaskCount",
                            label = stringResource(R.string.group_card_pending_label),
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TDText(
                            text = stringResource(R.string.group_card_created_label, createdDate),
                            style = TDTheme.typography.subheading4,
                            color = TDTheme.colors.statusCardGray,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        TDButton(
                            onClick = onViewDetailsClick,
                            text = stringResource(R.string.group_card_view_details),
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                            type = TDButtonType.OUTLINE,
                            size = TDButtonSize.SMALL,
                        )
                    }
                }
                if (isAdmin) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            painter = tdPainter(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.group_card_delete_cd),
                            modifier = Modifier.size(25.dp),
                            tint = TDTheme.colors.crossRed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    @DrawableRes iconRes: Int,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val isDark = TDTheme.isDark
    val statBoxShape = TDTheme.shapes.medium
    Surface(
        modifier =
        modifier
            .then(
                if (isDark) {
                    Modifier.border(
                        TDTheme.style.borderWidth,
                        tdOutlineColor(TDTheme.colors.lightGray.copy(alpha = 0.2f)),
                        statBoxShape,
                    )
                } else {
                    Modifier.tdShadow(
                        lightShadow = TDTheme.colors.white.copy(alpha = 0.85f),
                        darkShadow = TDTheme.colors.darkPending.copy(alpha = 0.15f),
                        cornerRadius = 12.dp,
                        elevation = 4.dp,
                    )
                },
            ).then(
                // clip first so the ripple stays inside the rounded shape
                if (onClick != null) {
                    Modifier.clip(statBoxShape).clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        shape = statBoxShape,
        color = TDTheme.colors.lightPending,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                painter = tdPainter(id = iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
            Column {
                TDText(
                    text = value,
                    style = TDTheme.typography.heading3,
                    color = TDTheme.colors.onBackground,
                )
                TDText(
                    text = label,
                    style = TDTheme.typography.subheading4,
                    color = TDTheme.colors.statusCardGray,
                )
            }
        }
    }
}

@TDPreview
@Composable
private fun TdFamilyGroupCardAdminPreview() {
    TDTheme {
        TDFamilyGroupCard(
            name = "The Smith Family",
            role = "Admin",
            description = "Daily chores, grocery lists, and vacation planning for 2024.",
            memberCount = 5,
            pendingTaskCount = 9,
            createdDate = "Jan 12, 2023",
            membersIcon = R.drawable.ic_members,
            tasksIcon = R.drawable.ic_tasks_done,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdFamilyGroupCardMemberPreview() {
    TDTheme {
        TDFamilyGroupCard(
            name = "Extended Cousins",
            role = "Member",
            description = "Planning the annual reunion, potluck coordination, and secret santa gifts.",
            memberCount = 14,
            pendingTaskCount = 5,
            createdDate = "Mar 05, 2023",
            membersIcon = R.drawable.ic_members,
            tasksIcon = R.drawable.ic_tasks_done,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdFamilyGroupCardWithAvatarPreview() {
    TDTheme {
        TDFamilyGroupCard(
            name = "Design Crew",
            role = "Admin",
            description = "Sprint cadence, design reviews, and weekly sync notes.",
            memberCount = 7,
            pendingTaskCount = 4,
            createdDate = "May 10, 2026",
            membersIcon = R.drawable.ic_members,
            tasksIcon = R.drawable.ic_tasks_done,
            avatarUrl = "https://example.com/avatar.png",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdFamilyGroupCardNoTasksPreview() {
    TDTheme {
        TDFamilyGroupCard(
            name = "Roommates",
            role = "Admin",
            description = "Shared chores and groceries for the apartment.",
            memberCount = 3,
            pendingTaskCount = 0,
            createdDate = "Apr 01, 2026",
            membersIcon = R.drawable.ic_members,
            tasksIcon = R.drawable.ic_tasks_done,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdFamilyGroupCardDraggingPreview() {
    TDTheme {
        TDFamilyGroupCard(
            name = "Work Squad",
            role = "Member",
            description = "Coordinating sprint planning and code reviews.",
            memberCount = 6,
            pendingTaskCount = 11,
            createdDate = "Feb 22, 2026",
            membersIcon = R.drawable.ic_members,
            tasksIcon = R.drawable.ic_tasks_done,
            isDragging = true,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@TDPreview
@Composable
private fun TdFamilyGroupCardDimmedPreview() {
    TDTheme {
        TDFamilyGroupCard(
            name = "Book Club",
            role = "Member",
            description = "Monthly reading list and discussion notes.",
            memberCount = 8,
            pendingTaskCount = 2,
            createdDate = "Jan 03, 2026",
            membersIcon = R.drawable.ic_members,
            tasksIcon = R.drawable.ic_tasks_done,
            isAnyDragging = true,
            isDragging = false,
            modifier = Modifier.padding(16.dp),
        )
    }
}
