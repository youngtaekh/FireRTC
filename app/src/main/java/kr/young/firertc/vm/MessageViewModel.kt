package kr.young.firertc.vm

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kr.young.common.ApplicationUtil
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.i
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.model.Call
import kr.young.firertc.model.Chat
import kr.young.firertc.model.Message
import kr.young.firertc.model.User
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.MessageRepository
import kr.young.firertc.util.RecyclerViewNotifier
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*
import kr.young.rtp.RTPManager
import org.webrtc.SessionDescription
import java.lang.System.currentTimeMillis
import java.util.*

class MessageViewModel private constructor(): ViewModel() {
    val vm = CallVM.instance
    private var messageDB = AppRoomDatabase.getInstance()
    private val rtpManager = RTPManager.instance

    var counterpart: User? = null
    var chat: Chat? = null
    var message: Message? = null

    val responseCode = MutableLiveData(0)
    val recyclerViewNotifier = MutableLiveData<RecyclerViewNotifier<Message>>(null)
    val receivedMessage = MutableLiveData<Message>(null)
    var isEndReload = false
    var firstSequence = -1L

    var isOffer = true
    var remoteSDP: SessionDescription? = null
    var remoteIce: String? = null
    var rtpConnected = false

    internal fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    private fun setRecyclerViewNotifier(notifier: RecyclerViewNotifier<Message>?) {
        Handler(Looper.getMainLooper()).post { recyclerViewNotifier.value = notifier }
    }

    fun setReceivedMessage(message: Message?) {
        Observable.just(0)
            .observeOn(AndroidSchedulers.mainThread())
            .map { receivedMessage.value = message }
            .subscribe()
    }

    fun release() {
        counterpart = null
        chat = null
        message = null

        setResponseCode(0)
        setRecyclerViewNotifier(null)
        setReceivedMessage(null)

        isEndReload = false
        firstSequence = -1L

        isOffer = true
        remoteIce = null
        remoteSDP = null
        rtpConnected = false
    }

    private val onChangeChat: (Map<String, Any>?) -> Unit = {
        i(TAG, "Current data $it")
        i(TAG, "message $message")
        if (message?.sequence == -1L) {
            message!!.sequence = it!!["lastSequence"] as Long
            MessageRepository.post(message!!)
            if (rtpConnected) {
                rtpManager.sendData(message!!.toString())
            } else {
                SendFCM.sendMessage(
                    toToken = counterpart!!.fcmToken!!,
                    type = SendFCM.FCMType.Message,
                    callType = Call.Type.MESSAGE,
                    chatId = chat!!.id,
                    messageId = message!!.id,
                    sequence = message!!.sequence,
                    message = message!!.body,
                    targetOS = counterpart!!.os
                )
            }
        }
    }

    @SuppressLint("CheckResult")
    private val onGetChatSuccess: (DocumentSnapshot, OnSuccessListener<Void>) -> Unit = { documentSnapshot, onSuccessListener ->
        d(TAG, "onGetChatSuccess")
        if (documentSnapshot.toObject<Chat>() == null) {
            ChatRepository.post(chat!!) {
                ChatRepository.addChatListener(chat!!.id, onChangeChat)
                onSuccessListener.onSuccess(null)
            }
        } else {
            chat = documentSnapshot.toObject()!!
            ChatRepository.addChatListener(chat!!.id, onChangeChat)
            onSuccessListener.onSuccess(null)
        }
    }

    @SuppressLint("CheckResult")
    private val onGetMessageSuccess: (MutableList<Message>, QuerySnapshot) -> Unit = { list, query ->
        val size = list.size
        d(TAG, "onGetMessageSuccess size $size ${query.documents.size}")
        Observable.fromIterable(query.documents.asReversed())
            .observeOn(Schedulers.computation())
            .map { it.toObject<Message>()!! }
            .observeOn(Schedulers.io())
            .map {
                messageDB?.messageDao()?.setMessage(it)
                addMessage(list, it, false)
            }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    val subList = list.subList(size, list.size)
                    d(TAG, "onGetMessageSuccess notifier($size, ${subList.size})")
                    val notifier = RecyclerViewNotifier(size, subList.size, Insert, true, subList)
                    setRecyclerViewNotifier(notifier)
                }
            )
    }

    fun startChat(counterpart: User?, chatCreateSuccess: OnSuccessListener<Void>) {
        d(TAG, "startChat counterpart is null ${counterpart == null}")
        if (counterpart == null) return
        this.counterpart = counterpart
        val participants = mutableListOf(MyDataViewModel.instance.getMyId())
        participants.add(counterpart.id)
        participants.sort()
        chat = Chat(participants = participants, title = counterpart.name)
        ChatRepository.getChat(chat!!.id) { onGetChatSuccess(it, chatCreateSuccess) }
    }

    @SuppressLint("CheckResult")
    fun getChatMessage() {
        val list = mutableListOf<Message>()
        Observable.just(chat!!.id)
            .observeOn(Schedulers.io())
            .concatMap { id -> messageDB!!.messageDao().getMessages(id).asReversed().toObservable() }
            .observeOn(Schedulers.computation())
            .map { message -> addMessage(list, message, false) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    d(TAG, "getChatMessage notifier(0, ${list.size})")
                    val notifier = RecyclerViewNotifier(0, list.size, Insert, true, list)
                    setRecyclerViewNotifier(notifier)

                    Observable.just(1)
                        .observeOn(Schedulers.io())
                        .map {
                            val last: Message? = messageDB!!.messageDao().getLastMessage(chat!!.id)
                            MessageRepository.getMessages(chatId = chat!!.id, min = last?.sequence ?: -1) { onGetMessageSuccess(list, it) }
                        }.observeOn(AndroidSchedulers.mainThread())
                        .subscribe()
                }
            )
    }

    @SuppressLint("CheckResult")
    fun getAdditionalMessages(list: MutableList<Message>, min: Long = -1, max: Long = 9_223_372_036_854_775_807) {
        d(TAG, "getAdditionalMessage($min, $max)")
        val copy = mutableListOf<Message>()
        copy.addAll(list)
        val size = copy.size
        Observable.just(max).observeOn(Schedulers.io())
            .flatMap { messageDB!!.messageDao().getMessages(chatId = chat!!.id, max = it).toObservable() }
            .map { addAdditionalMessage(copy, it) }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = {
                    val subList = copy.subList(0, copy.size - size)
                    d(TAG, "notifier(0, ${subList.size})")
                    val notifier = RecyclerViewNotifier(0, subList.size, Insert, false, subList)
                    setRecyclerViewNotifier(notifier)
                    getAdditionalMessageFromServer(copy, min, copy.first().sequence)
                })
    }

    @SuppressLint("CheckResult")
    private fun getAdditionalMessageFromServer(list: MutableList<Message>, min: Long, max: Long) {
        val size = list.size
        d(TAG, "getAdditionalMessageFromServer($min, $max)")
        MessageRepository.getMessages(chat!!.id, min = min, max = max) {
            d(TAG, "queryss size ${it.size()}")
            isEndReload = it.documents.isEmpty()
            val sub = it.documents.toObservable()
                .observeOn(Schedulers.io())
                .map { doc ->
                    val message = doc.toObject<Message>()!!
                    messageDB?.messageDao()?.setMessage(message)
                    addAdditionalMessage(list, message)
                    message
                }
                .toList().blockingGet()
            d(TAG, "sub size ${sub.size}")
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribeBy(onComplete = {
            d(TAG, "list size ${size}, ${list.size})")
            val subList = list.subList(0, list.size - size)
            d(TAG, "notifier(0, ${subList.size})")
            val notifier = RecyclerViewNotifier(0, subList.size, Insert, false, subList)
            setRecyclerViewNotifier(notifier)
//                })
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

        message = Message(chatId = chat!!.id, body = msg, createdAt = Date(currentTimeMillis()))
        ChatViewModel.instance.selectedChat!!.lastMessage = msg
        ChatViewModel.instance.updateChatLastMessage()

        return message!!
    }

    fun addMessage(list: MutableList<Message>, message: Message, isNotifier: Boolean = true) {
        // Check Date View
        val currentMessageDate = DateUtil.toFormattedString(message.createdAt!!, "yyMMdd")
        val lastMessageDate = if (list.size == 0) { null } else {
            DateUtil.toFormattedString(list.last().createdAt!!, "yyMMdd")
        }
        if (lastMessageDate == null || currentMessageDate != lastMessageDate) {
            val dateMessage = Message (
                sequence = message.sequence,
                createdAt = message.createdAt,
                isDate = true
            )
            list.add(dateMessage)
            if (isNotifier) { setRecyclerViewNotifier(RecyclerViewNotifier(list.size - 1, 1, Insert)) }
        }

        list.add(message)
        if (isNotifier) { setRecyclerViewNotifier(RecyclerViewNotifier(list.size - 1, 1, Insert, true)) }

        // Modifier Time View
        val currentMessageTime = DateUtil.toFormattedString(message.createdAt, "yy.MM.dd.aaHHmm")
        val lastMessageTime = DateUtil.toFormattedString(list[list.size - 2].createdAt!!, "yy.MM.dd.aaHHmm")
        if (currentMessageTime == lastMessageTime && message.from == list[list.size - 2].from) {
            list[list.size - 2].timeFlag = false
            if (isNotifier) { setRecyclerViewNotifier(RecyclerViewNotifier(list.size - 2, 1, Changed)) }
        }
    }

    private fun addAdditionalMessage(list: MutableList<Message>, message: Message) {
        if (
            list.first().isDate &&
            DateUtil.toFormattedString(list.first().createdAt!!, "yyMMdd") ==
            DateUtil.toFormattedString(message.createdAt!!, "yyMMdd")
        ) {
            list.removeAt(0)
        }

        if (list.first().from == message.from &&
            DateUtil.toFormattedString(list.first().createdAt!!, "yy.MM.dd.aaHHmm") ==
            DateUtil.toFormattedString(message.createdAt!!, "yy.MM.dd.aaHHmm")) {
            message.timeFlag = false
        }

        list.add(0, message)

        val dateMessage = Message(
            sequence = message.sequence,
            createdAt = message.createdAt,
            isDate = true
        )
        list.add(0, dateMessage)
    }

    private fun startRTP() {
        Handler(Looper.getMainLooper()).post {
            rtpManager.init(ApplicationUtil.getContext()!!,
                isAudio = false,
                isVideo = false,
                isDataChannel = true,
                enableStat = false,
                recordAudio = false
            )

            rtpManager.startRTP(ApplicationUtil.getContext()!!, null, isOffer, remoteSDP, remoteIce)
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

        startRTP()
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
        val message = Message(userId, chatId, messageId, msg, 0, Date(currentTimeMillis()))
        setReceivedMessage(message)
        if (chat != null && chat!!.id == chatId && ApplicationUtil.getContext() != null) {
            isOffer = true
            startRTP()
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