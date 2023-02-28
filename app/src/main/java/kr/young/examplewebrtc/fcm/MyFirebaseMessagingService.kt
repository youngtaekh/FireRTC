package kr.young.examplewebrtc.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.MainActivity
import kr.young.examplewebrtc.R
import kr.young.examplewebrtc.fcm.SendFCM.FCMType
import kr.young.examplewebrtc.repo.AppSP
import kr.young.examplewebrtc.util.Config.Companion.CALL_ID
import kr.young.examplewebrtc.util.Config.Companion.SPACE_ID
import kr.young.examplewebrtc.util.Config.Companion.TYPE
import kr.young.examplewebrtc.vm.CallViewModel
import kr.young.examplewebrtc.vm.SpaceViewModel

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            d(TAG, "Message data payload: ${remoteMessage.data}")
            val type = remoteMessage.data[TYPE]
            if (type != null) {
                when (FCMType.valueOf(type)) {
                    FCMType.New, FCMType.Leave -> {
                        receivedSpaceMessage(
                            remoteMessage.data[SPACE_ID],
                            remoteMessage.data[CALL_ID])
                    }
                    else ->{ sendNotification("else") }
                }
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun receivedSpaceMessage(spaceId: String?, callId: String?) {
        val spaceViewModel = SpaceViewModel.instance
        if (spaceViewModel.checkSpaceId(spaceId)) {
            SpaceViewModel.instance.getSpace()
            CallViewModel.instance.refreshCalls(spaceId!!)
        }
    }

    private fun scheduleJob() {
        // [START dispatch_job]
        val work = OneTimeWorkRequest.Builder(MyWorker::class.java)
            .build()
        WorkManager.getInstance(this)
            .beginWith(work)
            .enqueue()
        // [END dispatch_job]
    }

    private fun handleNow() {
        d(TAG, "Short lived task is done.")
    }

    private fun sendRegistrationToServer(token: String?) {
        if (token != null) {
            AppSP.instance.setFCMToken(token)
        }
        d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val channelId = "fcm_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FCM Message")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            channelId,
            "Default Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = "Default Channel Description"
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMessagingService"
    }

    internal class MyWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
        override fun doWork(): Result {
            // TODO(developer): add long running task here.
            return Result.success()
        }
    }
}