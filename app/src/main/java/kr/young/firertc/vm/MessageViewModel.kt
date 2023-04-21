package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.model.Chat
import kr.young.firertc.model.Message
import kr.young.firertc.model.User
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.ChatRepository.Companion.CHAT_READ_SUCCESS
import kr.young.firertc.repo.UserRepository
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.NotificationUtil
import kr.young.rtp.RTPManager
import org.webrtc.SessionDescription
import java.lang.System.currentTimeMillis
import java.util.*

class MessageViewModel private constructor(): ViewModel() {
    val vm = CallVM.instance
    var counterpart: User? = null
    var chat: Chat? = null
    var messageMap = mutableMapOf<String, MutableList<Message>>()
    var messageList: MutableList<Message>
        get() {
            return if (chat == null) {
                mutableListOf()
            } else if (messageMap[chat!!.id] == null) {
                messageMap[chat!!.id!!] = mutableListOf()
                messageMap[chat!!.id]!!
            } else {
                messageMap[chat!!.id]!!
            }
        }
        set(value) {
            if (chat != null) {
                messageMap[chat!!.id!!] = value
            }
        }

    val responseCode = MutableLiveData<Int>()

    var isOffer = true
    var remoteSDP: SessionDescription? = null
    var remoteIce: String? = null
    var rtpConnected = false

    internal fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun release() {
        counterpart = null
        if (chat != null && chat!!.id != null) {
            messageMap[chat!!.id!!] = mutableListOf()
        }
        chat = null
        remoteIce = null
        remoteSDP = null
        setResponseCode(0)
        rtpConnected = false
    }

    fun startOffer(counterpart: User, chatCreateSuccess: OnSuccessListener<Void>) {
        this.isOffer = true
        this.counterpart = counterpart
        val participants = mutableListOf(MyDataViewModel.instance.getMyId())
        participants.add(counterpart.id)
        participants.sort()
        chat = Chat(participants = participants, title = counterpart.name)
//        messageMap[chat!!.id!!] = mutableListOf()
        ChatRepository.getChat(chat!!.id!!) {
            if (it.toObject<Chat>() == null) {
                ChatRepository.post(chat!!, success = chatCreateSuccess)
            } else {
                chat = it.toObject()!!
                chatCreateSuccess.onSuccess(null)
            }
        }

        if (handler == null) {
            handler = Handler(Looper.myLooper()!!)
        }
        handler!!.postDelayed(cancelRunnable, 10 * 1000)
    }

    fun sendOffer(sdp: String) {
        d(TAG, "sendOffer")
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Offer,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
            sdp = sdp
        )
    }

    fun startAnswer() {
        d(TAG, "startAnswer")
    }

    fun sendAnswer(sdp: String) {
        d(TAG, "sendAnswer")
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Answer,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
            sdp = sdp
        )
    }

    fun end(fcmType: SendFCM.FCMType = SendFCM.FCMType.Bye) {
        d(TAG, "end")
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = fcmType,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
        )
        onTerminatedCall()
    }

    fun onIncomingCall(
        userId: String?,
        chatId: String?,
        message: String?,
        sdp: String?,
        fcmToken: String?
    ) {
        d(TAG, "onIncomingCall($userId, ${chat?.id}, $chatId, $message)")
        if (chatId == null || userId == null) { return }
        if (chat == null || chat!!.id != chatId) {
            SendFCM.sendMessage(fcmToken!!, SendFCM.FCMType.Decline, Call.Type.MESSAGE)
            return
        }

        this.isOffer = false
        this.remoteSDP = SessionDescription(SessionDescription.Type.OFFER, sdp!!)
        ChatRepository.getChat(chatId) {
            this.chat = it.toObject()!!
//            messageMap[chat!!.id!!] = mutableListOf()
            setResponseCode(CHAT_READ_SUCCESS)
            UserRepository.getUser(userId) { user ->
                this.counterpart = user.toObject()
                setResponseCode(USER_READ_SUCCESS)
            }
        }
    }

    fun onAnswerCall(sdp: String?) {
        RTPManager.instance.setRemoteDescription(SessionDescription(SessionDescription.Type.ANSWER, sdp!!))
        handler?.removeCallbacks(cancelRunnable)
    }

    fun onIceCandidate(ice: String?) {
        if (ice == null) return
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Ice,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
            sdp = ice
        )
    }

    fun onPCConnected() {
        d(TAG, "onPCConnected")
        rtpConnected = true
        handler?.removeCallbacks(cancelRunnable)
    }

    fun onPCClosed() {
        d(TAG, "onPCClosed")
        rtpConnected = false
    }

    fun onTerminatedCall() {
        d(TAG, "onTerminatedCall")
        handler?.removeCallbacks(cancelRunnable)
        RTPManager.instance.release()
    }

    fun onMessageReceived(chatId: String?, userId: String?, messageId: String?, message: String?) {
        if (messageMap[chatId] == null) {
            messageMap[chatId!!] = mutableListOf()
        }
        messageMap[chatId!!]!!.add(Message(userId!!, chatId, messageId!!, message!!, true, Date(currentTimeMillis())))
    }

    private var handler: Handler? = null
    private val cancelRunnable = Runnable { end(SendFCM.FCMType.Cancel) }

    init {
        setResponseCode(0)
    }

    private object Holder {
        val INSTANCE = MessageViewModel()
    }

    companion object {
        private const val TAG = "MessageViewModel"
        val instance: MessageViewModel by lazy { Holder.INSTANCE }
    }
}