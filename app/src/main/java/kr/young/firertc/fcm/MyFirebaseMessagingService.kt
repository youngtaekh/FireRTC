package kr.young.firertc.fcm

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
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.MainActivity
import kr.young.firertc.R
import kr.young.firertc.fcm.SendFCM.FCMType
import kr.young.firertc.model.Call
import kr.young.firertc.model.Space
import kr.young.firertc.repo.AppSP
import kr.young.firertc.repo.CallRepository
import kr.young.firertc.repo.SpaceRepository
import kr.young.firertc.util.Config.Companion.CALL_ID
import kr.young.firertc.util.Config.Companion.SDP
import kr.young.firertc.util.Config.Companion.SPACE_ID
import kr.young.firertc.util.Config.Companion.CALL_TYPE
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.util.Config.Companion.MESSAGE
import kr.young.firertc.util.Config.Companion.MESSAGE_ID
import kr.young.firertc.util.Config.Companion.NAME
import kr.young.firertc.util.Config.Companion.TYPE
import kr.young.firertc.util.Config.Companion.USER_ID
import kr.young.firertc.util.NotificationUtil
import kr.young.firertc.vm.*
import kr.young.rtp.RTPManager
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data[TYPE]
            d(TAG, "Message data payload: {userId: ${data[USER_ID]}, type: $type, callType: ${data[CALL_TYPE]}")
//            d(TAG, "Message data payload: $data")
            if (type != null) {
                when (FCMType.valueOf(type)) {
                    FCMType.New, FCMType.Leave -> {
                        receivedSpaceMessage(
                            data[SPACE_ID],
                            data[CALL_ID])
                    }
                    FCMType.Offer -> {
                        receiveOfferMessage(
                            data[USER_ID],
                            data[SPACE_ID],
                            data[CHAT_ID],
                            data[CALL_TYPE],
                            data[SDP],
                            data[MESSAGE],
                            data[FCM_TOKEN]
                        )
                    }
                    FCMType.Sdp -> { receiveSDPMessage(data[SDP]) }
                    FCMType.Ice -> { receiveICEMessage(data[SDP], data[CALL_TYPE]) }
                    FCMType.Answer -> { receiveAnswerMessage(data[SDP], data[CALL_TYPE]) }
                    FCMType.Bye, FCMType.Cancel, FCMType.Decline, FCMType.Busy -> { receiveEndMessage(data[CALL_TYPE]) }
                    FCMType.Message -> onReceiveMessage(data[CHAT_ID], data[USER_ID], data[NAME], data[MESSAGE_ID], data[MESSAGE])
                    else -> { sendNotification("else") }
                }
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    override fun onNewToken(token: String) {
        d(TAG, "Refreshed token $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun receiveOfferMessage(
        userId: String?,
        spaceId: String?,
        chatId: String?,
        type: String?,
        sdp: String?,
        message: String?,
        fcmToken: String?
    ) {
        d(TAG, "receiveOfferMessage($userId, $type)")
        if (CallVM.instance.space != null) {
            SendFCM.sendMessage(fcmToken!!, FCMType.Busy)
            SpaceRepository.getSpace(spaceId!!) {
                val space = it.toObject<Space>()!!
                CallVM.instance.busy(space)
            }
            val call = Call(
                spaceId = spaceId,
                type = Call.Type.valueOf(type!!),
                direction = Call.Direction.Answer,
                terminated = true
            )
            SpaceRepository.addCallList(spaceId, call.id)
            CallRepository.post(call) {
                CallRepository.updateTerminatedAt(call)
            }
        } else if (type == Call.Type.MESSAGE.toString()) {
            MessageViewModel.instance.onIncomingCall(userId, chatId, message, sdp, fcmToken)
        } else {
            CallVM.instance.onIncomingCall(this, userId, spaceId, type, sdp)
        }
    }

    private fun receiveAnswerMessage(sdp: String?, type: String?) {
        d(TAG, "receiveAnswerMessage")
        if (type == Call.Type.MESSAGE.toString()) {
            MessageViewModel.instance.onAnswerCall(sdp)
        } else {
            CallVM.instance.onAnswerCall(sdp)
        }
    }

    private fun receiveEndMessage(type: String?) {
        d(TAG, "receiveEndMessage")
        if (type == Call.Type.MESSAGE.toString()) {
            MessageViewModel.instance.onTerminatedCall()
        } else {
            CallVM.instance.onTerminatedCall()
        }
    }

    private fun receivedSpaceMessage(spaceId: String?, callId: String?) {
        val spaceViewModel = SpaceViewModel.instance
        if (spaceViewModel.checkSpaceId(spaceId)) {
            SpaceViewModel.instance.readSpace()
            CallViewModel.instance.refreshCalls(spaceId!!)
        }
    }

    private fun receiveSDPMessage(sdp: String?) {
        if (sdp != null) {
            val remote = SessionDescription(SessionDescription.Type.ANSWER, sdp)
            RTPManager.instance.setRemoteDescription(remote)
        }
    }

    private fun receiveICEMessage(sdp: String?, type: String?) {
        if (sdp != null) {
            if (RTPManager.instance.isInit && RTPManager.instance.isCreatedPCFactory) {
                d(TAG, "receiveIceMessage")
                val remote = IceCandidate("0", 0, sdp)
                RTPManager.instance.addRemoteIceCandidate(remote)
            } else if (type == Call.Type.MESSAGE.toString()) {
                if (MessageViewModel.instance.remoteIce == null) {
                    MessageViewModel.instance.remoteIce = sdp
                } else {
                    MessageViewModel.instance.remoteIce += ";$sdp"
                }
            } else {
                if (CallVM.instance.remoteIce == null) {
                    CallVM.instance.remoteIce = sdp
                } else {
                    CallVM.instance.remoteIce += ";$sdp"
                }
            }
        }
    }

    private fun onReceiveMessage(chatId: String?, userId: String?, name: String?, messageId: String?, message: String?) {
        d(TAG, "onReceiveMessage($chatId, $userId, $name, $messageId, $message)")
        MessageViewModel.instance.onMessageReceived(chatId, userId, messageId, message)
        NotificationUtil.messageNotification(this, chatId!!, name!!, message!!)
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
            if (token != AppSP.instance.getFCMToken()) {
                AppSP.instance.setFCMToken(token)
                MyDataViewModel.instance.updateFCMToken(token)
            }
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