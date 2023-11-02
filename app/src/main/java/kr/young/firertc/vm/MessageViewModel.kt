package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.ApplicationUtil
import kr.young.common.DateUtil
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
    val receivedMessage = MutableLiveData<Message>()

    val rtpManager = RTPManager.instance
    var isOffer = true
    var remoteSDP: SessionDescription? = null
    var remoteIce: String? = null
    var rtpConnected = false

    internal fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun release() {
        counterpart = null
//        if (chat != null && chat!!.id != null) {
//            messageMap[chat!!.id!!] = mutableListOf()
//        }
        chat = null
        remoteIce = null
        remoteSDP = null
        setResponseCode(0)
        rtpConnected = false
    }

    fun startChat(counterpart: User, chatCreateSuccess: OnSuccessListener<Void>) {
        this.isOffer = true
        this.counterpart = counterpart
        val participants = mutableListOf(MyDataViewModel.instance.getMyId())
        participants.add(counterpart.id)
        participants.sort()
        chat = Chat(participants = participants, title = counterpart.name)
        ChatRepository.getChat(chat!!.id!!) {
            if (it.toObject<Chat>() == null) {
                ChatRepository.post(chat!!, success = chatCreateSuccess)
            } else {
                chat = it.toObject()!!
                chatCreateSuccess.onSuccess(null)
            }
        }
    }

    fun sendOffer(sdp: String) {
        d(TAG, "sendOffer")
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Offer,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
            targetOS = counterpart!!.os,
            sdp = sdp
        )
    }

    fun sendAnswer(sdp: String) {
        d(TAG, "sendAnswer")
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Answer,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
            targetOS = counterpart!!.os,
            sdp = sdp
        )
    }

    fun sendData(msg: String): Message {
        d(TAG, "send($msg)")
        val message = Message(MyDataViewModel.instance.getMyId(), chat!!.id!!, body = msg, createdAt = Date(currentTimeMillis()))
        if (rtpConnected) {
            rtpManager.sendData(msg)
        } else {
            SendFCM.sendMessage(
                toToken = counterpart!!.fcmToken!!,
                type = SendFCM.FCMType.Message,
                callType = Call.Type.MESSAGE,
                chatId = chat!!.id,
                messageId = message.id,
                message = msg,
                targetOS = counterpart!!.os
            )
        }
        ChatViewModel.instance.selectedChat!!.lastMessage = msg
        ChatViewModel.instance.updateChatLastMessage()

        return message
    }

    fun addDateView(message: Message) {
        if (messageMap[message.chatId] == null) {
            messageMap[message.chatId] = mutableListOf()
        }

        val curDate = DateUtil.toFormattedString(message.createdAt!!, "yyMMdd")
        val lastDate = if (messageMap[message.chatId]!!.size == 0) {
            null
        } else {
            DateUtil.toFormattedString(messageMap[message.chatId]!!.last().createdAt!!, "yyMMdd")
        }

        if (lastDate == null || curDate != lastDate) {
            val dateMessage = Message(from = message.from, chatId = message.chatId, body = message.body, createdAt = message.createdAt, isDate = true)
            messageMap[message.chatId]!!.add(dateMessage)
        }
    }

    fun end(fcmType: SendFCM.FCMType = SendFCM.FCMType.Bye) {
        d(TAG, "end")
        if (fcmType == SendFCM.FCMType.Bye) {
            SendFCM.sendMessage(
                toToken = counterpart!!.fcmToken!!,
                type = fcmType,
                callType = Call.Type.MESSAGE,
                chatId = chat!!.id,
                targetOS = counterpart!!.os,
            )
        }
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
            SendFCM.sendMessage(
                toToken = fcmToken!!,
                type = SendFCM.FCMType.Decline,
                callType = Call.Type.MESSAGE,
                targetOS = counterpart!!.os
            )
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
    }

    fun onIceCandidate(ice: String?) {
        if (ice == null) return
        SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = SendFCM.FCMType.Ice,
            callType = Call.Type.MESSAGE,
            chatId = chat!!.id,
            targetOS = counterpart!!.os,
            sdp = ice
        )
    }

    fun onPCConnected() {
        d(TAG, "onPCConnected")
        rtpConnected = true
    }

    fun onPCClosed() {
        d(TAG, "onPCClosed")
        rtpConnected = false
    }

    fun onTerminatedCall() {
        d(TAG, "onTerminatedCall")
        RTPManager.instance.release()
    }

    fun onMessageReceived(chatId: String?, userId: String?, messageId: String?, msg: String?) {
        if (chatId == null || userId == null || messageId == null || msg == null) { return }
        if (messageMap[chatId] == null) {
            messageMap[chatId] = mutableListOf()
        }
        val message = Message(userId, chatId, messageId, msg, true, Date(currentTimeMillis()))
        addDateView(message)
        messageMap[chatId]!!.add(message)
        Handler(Looper.getMainLooper()).post { receivedMessage.value = message }
        ChatViewModel.instance.updateChatLastMessage(Chat(id = chatId, lastMessage = msg))
        if (chat != null && chat!!.id == chatId && ApplicationUtil.getContext() != null) {
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                val rtpManager = RTPManager.instance
                rtpManager.init(ApplicationUtil.getContext()!!,
                    isAudio = false,
                    isVideo = false,
                    isDataChannel = true,
                    enableStat = false,
                    recordAudio = false
                )

                rtpManager.startRTP(context = ApplicationUtil.getContext()!!, data = null, isOffer = isOffer, remoteSDP, remoteIce)
            }
        }
    }

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