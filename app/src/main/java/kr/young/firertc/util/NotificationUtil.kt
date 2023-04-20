package kr.young.firertc.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.*
import androidx.core.app.NotificationManagerCompat
import kr.young.firertc.*
import kr.young.firertc.model.Call
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.vm.CallVM

class NotificationUtil {
    companion object {
        fun getCallNotification(context: Context, isReceive: Boolean = false): Notification {
            val intent = Intent()
            if (isReceive) {
                intent.setClass(context, ReceiveActivity::class.java)
            } else if (CallVM.instance.callType == Call.Type.AUDIO) {
                intent.setClass(context, AudioCallActivity::class.java)
            } else {
                intent.setClass(context, VideoCallActivity::class.java)
            }
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)

            val builder = Builder(context, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.round_call_24)
                .setContentTitle("Call")
                .setContentText(if (isReceive) "Incoming from ${CallVM.instance.counterpart?.name}" else "Outgoing to ${CallVM.instance.counterpart?.name}")
                .setAutoCancel(false)
                .setOngoing(true)
                .setCategory(CATEGORY_CALL)
                .setContentIntent(pendingIntent)

            if (isReceive) {
                builder.setFullScreenIntent(pendingIntent, true)
                builder.priority = PRIORITY_HIGH
                createChannel(
                    context, RECEIVE_CHANNEL_ID,
                    context.getString(R.string.receive_notification),
                    context.getString(R.string.receive_notification_desc))
                builder.setChannelId(RECEIVE_CHANNEL_ID)
            } else {
                createChannel(
                    context, CALL_CHANNEL_ID,
                    context.getString(R.string.call_notification),
                    context.getString(R.string.call_notification_desc))
                builder.setChannelId(CALL_CHANNEL_ID)
            }

            return builder.build()
        }

        fun messageNotification(context: Context, chatId: String, name: String, message: String) {
            createChannel(context, MESSAGE_CHANNEL_ID, context.getString(R.string.message_notification), context.getString(R.string.message_notification_desc))
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(CHAT_ID, chatId)
            intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
            val builder = Builder(context, MESSAGE_CHANNEL_ID)
                .setSmallIcon(R.drawable.round_chat_24)
                .setContentTitle(name)
                .setContentText(message)
                .setAutoCancel(true)
                .setOngoing(false)
                .setCategory(CATEGORY_APP_MESSAGING)
                .setContentIntent(pendingIntent)
                .setPriority(PRIORITY_HIGH)
                .setChannelId(MESSAGE_CHANNEL_ID)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(123, builder.build())
        }

        private fun createChannel(
            context: Context,
            channelId: String,
            name: String,
            description: String,
            importance: Int = NotificationManager.IMPORTANCE_HIGH
        ) {
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        private const val TAG = "NotificationUtil"
        private const val CALL_CHANNEL_ID = "callChannelId"
        private const val MESSAGE_CHANNEL_ID = "messageChannelId"
        private const val RECEIVE_CHANNEL_ID = "receiveChannelId"
        const val CALL_NOTIFICATION_ID = 1234
    }
}