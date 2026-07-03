package com.todoapp.mobile.data.source.remote.fcm

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.uikit.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.todoapp.mobile.MainActivity
import com.todoapp.mobile.data.notification.NotificationService
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.repository.FCMTokenPreferences
import com.todoapp.mobile.domain.repository.GroupRepository
import com.todoapp.mobile.domain.repository.NotificationRepository
import com.todoapp.mobile.domain.repository.TaskSyncRepository
import com.todoapp.mobile.navigation.CurrentRouteTracker
import com.todoapp.mobile.navigation.RouteArgs
import com.todoapp.mobile.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class TDFireBaseMessagingService : FirebaseMessagingService() {
    @Inject lateinit var fcmTokenPreferences: FCMTokenPreferences

    @Inject lateinit var groupRepository: GroupRepository

    @Inject lateinit var taskSyncRepository: TaskSyncRepository

    @Inject lateinit var notificationRepository: NotificationRepository

    @Inject lateinit var currentRouteTracker: CurrentRouteTracker

    @Inject @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val scope by lazy { CoroutineScope(SupervisorJob() + ioDispatcher) }

    // The notification large icon is the static app logo. Decode it once and reuse it: re-decoding
    // on every push wastes native memory, and the copies accumulate while notifications stay posted.
    // Never recycle it — the posted Notification keeps a reference, so recycling crashes the system UI.
    private val largeIconBitmap: Bitmap? by lazy {
        AppCompatResources
            .getDrawable(this, com.todoapp.mobile.R.mipmap.ic_app_logo)
            ?.toBitmap(width = 128, height = 128)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        fcmTokenPreferences.setPendingToken(token)
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val payload = PushPayloadParser.parse(message.data)
        if (payload == null) {
            message.notification?.let { showGenericNotification(it.title, it.body) }
            return
        }

        // Refresh inbox/badge for any push that has a 'type' (i.e. backend-published).
        scope.launch { runCatching { notificationRepository.refresh(force = true) } }

        // Localize title/body from the raw data map using the current device locale, falling
        // back to the server-provided English strings.
        val localized = localize(payload, message.data)
        val title = localized?.title ?: payload.title
        val body = localized?.body ?: payload.body

        when (payload) {
            is PushPayload.TaskAssigned -> {
                scope.launch { groupRepository.syncGroupTasks(payload.groupId, force = true) }
                if (!isOnGroupTask(payload.groupId, payload.taskId)) {
                    showGroupTaskNotification(title, body, payload.groupId, payload.taskId, urgent = true)
                }
            }
            is PushPayload.TaskCompleted -> {
                scope.launch { groupRepository.syncGroupTasks(payload.groupId, force = true) }
                if (!isOnGroupTask(payload.groupId, payload.taskId)) {
                    showGroupTaskNotification(title, body, payload.groupId, payload.taskId)
                }
            }
            is PushPayload.TaskDueSoon -> {
                if (!isOnGroupTask(payload.groupId, payload.taskId)) {
                    showGroupTaskNotification(title, body, payload.groupId, payload.taskId, urgent = true)
                }
            }
            is PushPayload.InvitationReceived -> {
                if (!isOnRoute(Screen.Invitations::class.qualifiedName)) {
                    showTargetedNotification(title, body, PUSH_TARGET_INVITATIONS)
                }
            }
            is PushPayload.InvitationAccepted -> {
                payload.groupId?.let {
                    scope.launch { runCatching { groupRepository.getGroups(force = true) } }
                }
                showGenericNotification(title, body)
            }
            is PushPayload.InvitationDeclined -> {
                showGenericNotification(title, body)
            }
            is PushPayload.GroupInvite -> {
                // Legacy payload; treat as InvitationReceived deep-link target.
                if (!isOnRoute(Screen.Invitations::class.qualifiedName)) {
                    showTargetedNotification(title, body, PUSH_TARGET_INVITATIONS)
                }
            }
            is PushPayload.GroupTaskChanged -> {
                scope.launch { groupRepository.syncGroupTasks(payload.groupId, force = true) }
                if (!payload.silent &&
                    !isOnGroupTask(payload.groupId, payload.taskId)
                ) {
                    showGroupTaskNotification(
                        title = title,
                        body = body,
                        groupId = payload.groupId,
                        taskId = payload.taskId,
                    )
                }
            }
            is PushPayload.TaskListChanged -> {
                taskSyncRepository.fetchTasks(force = true)
            }
            is PushPayload.GroupOwnershipTransferred -> {
                scope.launch { runCatching { groupRepository.getGroups(force = true) } }
                val resolvedTitle = payload.groupName?.let {
                    getString(com.todoapp.mobile.R.string.notification_group_ownership_title_format, it)
                } ?: title
                val resolvedBody = body
                    ?: getString(com.todoapp.mobile.R.string.notification_group_ownership_body)
                showGroupTaskNotification(resolvedTitle, resolvedBody, payload.groupId, taskId = null)
            }
            is PushPayload.Unknown -> {
                Timber.tag(TAG).d("Unknown push type=%s", payload.type)
                if (!payload.title.isNullOrBlank() || !payload.body.isNullOrBlank()) {
                    showGenericNotification(title, body)
                }
            }
        }
    }

    private fun localize(
        payload: PushPayload,
        data: Map<String, String>,
    ): com.todoapp.mobile.ui.notifications.NotificationContent.Rendered? {
        val type = when (payload) {
            is PushPayload.TaskAssigned -> com.todoapp.mobile.domain.model.NotificationType.TASK_ASSIGNED
            is PushPayload.TaskCompleted -> com.todoapp.mobile.domain.model.NotificationType.TASK_COMPLETED
            is PushPayload.TaskDueSoon -> com.todoapp.mobile.domain.model.NotificationType.TASK_DUE_SOON
            is PushPayload.InvitationReceived,
            is PushPayload.GroupInvite,
            -> com.todoapp.mobile.domain.model.NotificationType.INVITATION_RECEIVED
            is PushPayload.InvitationAccepted -> com.todoapp.mobile.domain.model.NotificationType.INVITATION_ACCEPTED
            is PushPayload.InvitationDeclined -> com.todoapp.mobile.domain.model.NotificationType.INVITATION_DECLINED
            else -> return null
        }
        return com.todoapp.mobile.ui.notifications.NotificationContent.render(
            context = this,
            type = type,
            payload = data,
            fallbackTitle = payload.title.orEmpty(),
            fallbackBody = payload.body.orEmpty(),
        )
    }

    private fun isOnRoute(route: String?): Boolean = route != null && currentRouteTracker.route.value == route

    private fun isOnGroupTask(groupId: Long, taskId: Long?): Boolean {
        val args = currentRouteTracker.args.value as? RouteArgs.GroupTaskDetail ?: return false
        return args.groupId == groupId && (taskId == null || args.taskId == taskId)
    }

    private fun showGroupTaskNotification(
        title: String?,
        body: String?,
        groupId: Long,
        taskId: Long?,
        urgent: Boolean = false,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_GROUP_ID, groupId)
            taskId?.let { putExtra(EXTRA_PUSH_TASK_ID, it) }
        }
        emit(title, body, buildPendingIntent(intent), urgent)
    }

    private fun showTargetedNotification(
        title: String?,
        body: String?,
        target: String,
        // Invitation pushes are time-sensitive (the user is expected to respond) → urgent by default.
        urgent: Boolean = true,
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_TARGET, target)
        }
        emit(title, body, buildPendingIntent(intent), urgent)
    }

    private fun showGenericNotification(title: String?, body: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_TARGET, PUSH_TARGET_NOTIFICATIONS)
        }
        // Generic notices are FYI-only → non-urgent (info channel).
        emit(title, body, buildPendingIntent(intent), urgent = false)
    }

    private fun buildPendingIntent(intent: Intent): PendingIntent = PendingIntent.getActivity(
        this,
        Random.nextInt(),
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        },
    )

    @SuppressLint("MissingPermission")
    private fun emit(title: String?, body: String?, pendingIntent: PendingIntent, urgent: Boolean) {
        val resolvedTitle = title ?: getString(com.todoapp.mobile.R.string.app_name)
        val resolvedBody = body.orEmpty()
        val accentColor = ContextCompat.getColor(this, com.todoapp.mobile.R.color.notification_accent)
        val channelId = if (urgent) NotificationService.CHANNEL_ID else NotificationService.CHANNEL_ID_INFO
        val notification =
            NotificationCompat
                .Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(resolvedTitle)
                .setContentText(resolvedBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(resolvedBody))
                .also { builder -> largeIconBitmap?.let { builder.setLargeIcon(it) } }
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setColor(accentColor)
                .setPriority(if (urgent) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setDefaults(if (urgent) (NotificationCompat.DEFAULT_VIBRATE or NotificationCompat.DEFAULT_LIGHTS) else 0)
                .build()
        runCatching {
            NotificationManagerCompat
                .from(this)
                .notify(Random.nextInt(), notification)
        }.onFailure { Timber.tag(TAG).w(it, "Failed to post notification") }
    }

    companion object {
        private const val TAG = "FCM"
        const val EXTRA_PUSH_GROUP_ID = "push_group_id"
        const val EXTRA_PUSH_TASK_ID = "push_task_id"
        const val EXTRA_PUSH_TARGET = "push_target"
        const val PUSH_TARGET_INVITATIONS = "invitations"
        const val PUSH_TARGET_NOTIFICATIONS = "notifications"
    }
}
