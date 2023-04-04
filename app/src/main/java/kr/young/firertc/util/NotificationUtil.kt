package kr.young.firertc.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationCompat.CATEGORY_CALL
import kr.young.firertc.MainActivity
import kr.young.firertc.R
import kr.young.firertc.ReceiveActivity
import kr.young.firertc.vm.CallVM

class NotificationUtil {
    companion object {
        fun getCallNotification(context: Context, isReceive: Boolean = false): Notification {
            val intent = Intent()
            if (isReceive) {
                intent.setClass(context, ReceiveActivity::class.java)
            } else {
                intent.setClass(context, MainActivity::class.java)
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
                builder.priority = NotificationCompat.PRIORITY_HIGH
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
        private const val RECEIVE_CHANNEL_ID = "receiveChannelId"
        const val CALL_NOTIFICATION_ID = 1234
    }
}