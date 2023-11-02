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
import kr.young.firertc.model.FirebaseMessage
import kr.young.firertc.model.Space
import kr.young.firertc.repo.AppSP
import kr.young.firertc.repo.CallRepository
import kr.young.firertc.repo.SpaceRepository
import kr.young.firertc.util.NotificationUtil
import kr.young.firertc.vm.*
import kr.young.rtp.RTPManager
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val fm = FirebaseMessage(data)
            d(TAG, "Message data payload: ${fm.userId}, ${fm.type}, ${fm.callType}")
            if (fm.type != null) {
                when (FCMType.valueOf(fm.type!!)) {
                    FCMType.New, FCMType.Leave -> receivedSpaceMessage(fm)
                    FCMType.Offer -> receiveOfferMessage(fm)
                    FCMType.Sdp -> receiveSDPMessage(fm)
                    FCMType.Ice -> receiveICEMessage(fm)
                    FCMType.Answer -> receiveAnswerMessage(fm)
                    FCMType.Bye, FCMType.Cancel, FCMType.Decline, FCMType.Busy -> receiveEndMessage(fm)
                    FCMType.Message -> onReceiveChatMessage(fm)
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

    private fun receiveOfferMessage(fm: FirebaseMessage) {
        d(TAG, "receiveOfferMessage(${fm.userId}, ${fm.callType}, sdp: ${fm.sdp != null})")
        if (CallVM.instance.space != null) {
            SendFCM.sendMessage(
                toToken = fm.fcmToken!!,
                type = FCMType.Busy,
                callType = Call.Type.valueOf(fm.callType ?: "AUDIO"),
                targetOS = fm.targetOS
            )
            SpaceRepository.getSpace(fm.spaceId!!) {
                val space = it.toObject<Space>()!!
                CallVM.instance.busy(space)
            }
            val call = Call(
                spaceId = fm.spaceId,
                type = Call.Type.valueOf(fm.callType!!),
                direction = Call.Direction.Answer,
                terminated = true
            )
            SpaceRepository.addCallList(fm.spaceId!!, call.id)
            CallRepository.post(call) {
                CallRepository.updateTerminatedAt(call)
            }
        } else if (fm.callType == Call.Type.MESSAGE.toString()) {
            MessageViewModel.instance.onIncomingCall(fm.userId, fm.chatId, fm.message, fm.sdp, fm.fcmToken)
        } else {
            CallRepository.getCall(fm.callId!!) {
                val call = it.toObject<Call>()
                CallVM.instance.onIncomingCall(this, fm.userId, fm.spaceId, fm.callType, call!!.sdp)
            }
        }
    }

    private fun receiveAnswerMessage(fm: FirebaseMessage) {
        d(TAG, "receiveAnswerMessage")
        if (fm.callType == Call.Type.MESSAGE.toString()) {
            MessageViewModel.instance.onAnswerCall(fm.sdp)
        } else {
            CallRepository.getCall(fm.callId!!) {
                val call = it.toObject<Call>()!!
                CallVM.instance.onAnswerCall(call.sdp)
            }
        }
    }

    private fun receiveEndMessage(fm: FirebaseMessage) {
        d(TAG, "receiveEndMessage")
        if (fm.callType == Call.Type.MESSAGE.toString()) {
            MessageViewModel.instance.onTerminatedCall()
        } else {
            CallVM.instance.onTerminatedCall()
        }
    }

    private fun receivedSpaceMessage(fm: FirebaseMessage) {
        val spaceViewModel = SpaceViewModel.instance
        if (spaceViewModel.checkSpaceId(fm.spaceId)) {
            SpaceViewModel.instance.readSpace()
            CallViewModel.instance.refreshCalls(fm.spaceId!!)
        }
    }

    private fun receiveSDPMessage(fm: FirebaseMessage) {
        if (fm.sdp != null) {
            val remote = SessionDescription(SessionDescription.Type.ANSWER, fm.sdp!!)
            RTPManager.instance.setRemoteDescription(remote)
        }
    }

    private fun receiveICEMessage(fm: FirebaseMessage) {
        if (fm.sdp != null) {
            if (RTPManager.instance.isInit && RTPManager.instance.isCreatedPCFactory) {
                d(TAG, "receiveIceMessage")
                val remote = IceCandidate("0", 0, fm.sdp!!)
                RTPManager.instance.addRemoteIceCandidate(remote)
            } else if (fm.callType == Call.Type.MESSAGE.toString()) {
                if (MessageViewModel.instance.remoteIce == null) {
                    MessageViewModel.instance.remoteIce = fm.sdp!!
                } else {
                    MessageViewModel.instance.remoteIce += ";${fm.sdp!!}"
                }
            } else {
                if (CallVM.instance.remoteIce == null) {
                    CallVM.instance.remoteIce = fm.sdp!!
                } else {
                    CallVM.instance.remoteIce += ";${fm.sdp!!}"
                }
            }
        }
    }

    private fun onReceiveChatMessage(fm: FirebaseMessage) {
        d(TAG, "onReceiveChatMessage(${fm.chatId != null}, ${fm.userId}, ${fm.name}, ${fm.messageId != null}, ${fm.message})")
        MessageViewModel.instance.onMessageReceived(fm.chatId, fm.userId, fm.messageId, fm.message)
        NotificationUtil.messageNotification(this, fm.chatId!!, fm.name!!, fm.message!!)
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