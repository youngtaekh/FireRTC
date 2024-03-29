package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kr.young.common.ApplicationUtil
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.fcm.SendFCM
import kr.young.firertc.fcm.SendFCM.FCMType
import kr.young.firertc.model.Call
import kr.young.firertc.model.Chat
import kr.young.firertc.model.Message
import kr.young.firertc.model.User
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.MessageRepository
import kr.young.firertc.repo.UserRepository
import kr.young.firertc.util.Config.Companion.LAST_SEQUENCE
import kr.young.firertc.util.Config.Companion.MAX_LONG
import kr.young.firertc.util.Config.Companion.MESSAGE_PAGE_SIZE
import kr.young.firertc.util.Config.Companion.MIN_LONG
import kr.young.rtp.RTPManager
import org.webrtc.SessionDescription
import org.webrtc.SessionDescription.Type
import java.lang.Integer.min
import java.lang.System.currentTimeMillis
import java.util.*

class MessageVM {
    private val roomDB = AppRoomDatabase.getInstance()!!

    var chatId: String? = null
    val chat = MutableLiveData<Chat>(null)
    val messageList = MutableLiveData(mutableListOf<Message>())
    var receiveMessage = MutableLiveData<Message>(null)
    var isNoAdditionalMessage = false
    var firstSequence = -1L

    var participantMap = linkedMapOf<String, User?>()
    val counterpart: User? get() = participantMap.values.firstOrNull()
    var pendingMessage: Message? = null

    private val rtpManager = RTPManager.instance
    private var remoteSDP: SessionDescription? = null
    var remoteICE: String? = null
    internal var isOffer = true
    var rtpConnected = false

    fun setChatLiveData(chat: Chat?) {
        d(TAG, "setChatLiveData(chat - $chat)")
        chatId = chat?.id
        if (Thread.currentThread().name.lowercase() == "main") {
            this.chat.value = chat
        } else {
            if (chat == null) {
                Handler(Looper.getMainLooper()).post { this.chat.value = null }
            } else {
                Observable.just(chat)
                    .observeOn(AndroidSchedulers.mainThread())
                    .map { this.chat.value = it }
                    .blockingSubscribe()
            }
        }
    }

    private fun setMessageList(messageList: MutableList<Message> = this.messageList.value!!) {
        d(TAG, "setMessageList(${messageList.size})")
        if (Thread.currentThread().name.lowercase() == "main") {
            this.messageList.value = messageList
        } else {
            Observable.just(messageList).observeOn(AndroidSchedulers.mainThread())
                .map { this.messageList.value = it }.subscribe()
        }
    }

    private fun setReceiveMessage(message: Message?) {
        d(TAG, "setReceiveMessage($message)")
        Handler(Looper.getMainLooper()).post { this.receiveMessage.value = message }
    }

    fun release() {
        d(TAG, "release()")
        chatId = null
        setChatLiveData(null)
        setMessageList(mutableListOf())
        setReceiveMessage(null)
        isNoAdditionalMessage = false
        firstSequence = -1L

        participantMap = linkedMapOf()
//        counterpart = participants.firstOrNull()
        pendingMessage = null

        remoteSDP = null
        remoteICE = null
        isOffer = true
        rtpConnected = false
    }

    fun start() {
        d(TAG, "start()")
        if (chatId != null) {
            //From Notification
            getChatDetail(chatId = chatId)
        } else if (chat.value != null) {
            //From ChatFragment
            getChatInfo(chat.value!!)
        } else if (participantMap.size == 1) {
            //From ProfileActivity
            getOneChatDetail()
        } else {
            //From AddChatActivity
        }
    }

    private fun getParticipantInfo() {
        d(TAG, "getParticipantInfo()")
        chat.value?.let {
            Observable.just(chat.value!!.participants)
                .observeOn(Schedulers.io())
                .concatMap { AppRoomDatabase.getInstance()!!.userDao().getUsers(it).toObservable() }
                .map { participantMap[it.id] = it }
                .blockingSubscribe()

            var start = 0
            val size = chat.value!!.participants.size
            while (size > start) {
                UserRepository.getUsers(chat.value!!.participants.subList(start, min(start + 10, size))) {
                    it.documents.toObservable()
                        .map { doc -> doc.toObject<User>() }
                        .map { user -> participantMap[user.id] = user }
                        .blockingSubscribe()
                }
                start += 10
            }
        }
    }

    private fun getOneChatDetail() {
        d(TAG, "getOneChatDetail")
        val participants = mutableListOf(MyDataViewModel.instance.getMyId(), counterpart!!.id)
        participants.sort()
        val chat = Chat(participants = participants, localTitle = counterpart!!.name)
        getChatDetail(chat = chat)
    }

    private fun getChatDetail(chat: Chat? = null, chatId: String? = null) {
        d(TAG, "getChatDetail(chat ${chat != null}, chatId ${chatId != null})")
        Observable.just(chatId ?: chat!!.id)
            .observeOn(Schedulers.io())
            .filter { existChat(it) }
            .map { roomDB.chatDao().getChat(it) }
            .doOnNext { setChatLiveData(it) }
            .doOnNext { getChatInfo(it!!) }
            .subscribe()

        Observable.just(chatId ?: chat!!.id)
            .observeOn(Schedulers.io())
            .filter { !existChat(it) }
            .doOnNext { getChatDetailFromServer(it, chat?.localTitle, true) }
            .subscribe()
    }

    fun createOneChat() {
        d(TAG, "createOneChat")
        val participants = mutableListOf(MyDataViewModel.instance.getMyId(), counterpart!!.id)
        participants.sort()
        val chat = Chat(participants = participants, localTitle = counterpart!!.name)
        setChatToServer(chat)
    }

    private fun existChat(id: String): Boolean {
        return roomDB.chatDao().getChat(id) != null
    }

    private fun getChatInfo(chat: Chat, getMessage: Boolean = true) {
        d(TAG, "getChatInfo $chat ${participantMap.isEmpty()}")
        ChatRepository.addChatListener(chat.id, chatChangeListener)
        if (participantMap.isEmpty()) {
            getParticipantInfo()
        }
        if (getMessage) {
            getMessages()
        }
    }

    fun getMessages(
        isAdditional: Boolean = false,
        min: Long = MIN_LONG,
        max: Long = MAX_LONG
    ) {
        d(TAG, "getMessages(isAdditional = $isAdditional, $min ~ $max)")
        val chatId = this.chatId ?: this.chat.value?.id
        chatId?.let {
            val list = Observable.just(chatId)
                .observeOn(Schedulers.io())
                .concatMap { id -> roomDB.messageDao().getMessages(id, min, max).asReversed().toObservable() }
                .toList().blockingGet()

            addDateMessage(list, isAdditional)

            if (isAdditional) {
                getMessagesFromServer(chatId, true, max - MESSAGE_PAGE_SIZE, list.firstOrNull()?.sequence ?: max)
            } else {
                if (list.isNotEmpty() && list.size < MESSAGE_PAGE_SIZE) {
                    getMessagesFromServer(chatId, true, list.last().sequence - MESSAGE_PAGE_SIZE, list.first().sequence)
                }
                getMessagesFromServer(chatId, false, list.lastOrNull()?.sequence ?: min, max)
            }
        }
    }

    fun addDateMessage(iterator: List<Message>, isAdditional: Boolean) {
        d(TAG, "addDateMessage(${iterator.size}, $isAdditional)")
        val list = iterator.toObservable()
            .buffer(2, 1)
            .concatMap { checkDateMessage(it.first(), it.last()).toObservable() }
            .toList().blockingGet() as MutableList

        list.firstOrNull()?.let {
            val existList = mutableListOf<Message>()
            existList.addAll(messageList.value!!)
            if (isAdditional) {
                list.add(0, Message(sequence = it.sequence, createdAt = it.createdAt, isDate = true))
                if (checkDateMessage(list.last(), existList.first()).size != 2) {
                    existList.removeFirst()
                }
                list.addAll(existList)
            } else {
                if (existList.isNotEmpty() && !checkTime(existList.last(), list.first())) {
                    val lastMessage = Message.copy(existList.last())
                    lastMessage.timeFlag = false
                    existList.removeLast()
                    existList.add(lastMessage)
                }
                if (existList.isEmpty() || checkDateMessage(existList.last(), list.first()).size == 2) {
                    list.add(0, Message(sequence = it.sequence, createdAt = it.createdAt, isDate = true))
                }
                list.addAll(0, existList)
            }
            setMessageList(list)
        }
    }

    private fun checkDateMessage(first: Message, last: Message): List<Message> {
        val firstDate = DateUtil.toFormattedString(first.createdAt!!, "yy MM dd")
        val lastDate = DateUtil.toFormattedString(last.createdAt!!, "yy MM dd")
        first.timeFlag = checkTime(first, last)
        val list = mutableListOf(first)
        if (firstDate != lastDate) {
            val dateMessage = Message(sequence = last.sequence, createdAt = last.createdAt, isDate = true)
            list.add(dateMessage)
        }
        return list
    }

    private fun checkTime(first: Message?, last: Message?): Boolean {
        if (first == null || last == null) {
            return true
        }
        val firstTime = DateUtil.toFormattedString(first.createdAt!!, "HH mm")
        val lastTime = DateUtil.toFormattedString(last.createdAt!!, "HH mm")
        if (first.id != last.id && first.from == last.from && firstTime == lastTime) {
            return false
        }
        return true
    }

    /** Firestore Access */
    private fun setChatToServer(chat: Chat?) { chat?.let {
        ChatRepository.post(it) { _ ->
            getChatDetailFromServer(it.id, chat.localTitle, false)
        }
    }}

    private fun getChatDetailFromServer(id: String, localTitle: String?, getMessage: Boolean) {
        ChatRepository.getChat(id) {
            it.toObject<Chat>()?.let { chat ->
                chat.removeParticipants(MyDataViewModel.instance.getMyId())
                if (!localTitle.isNullOrEmpty()) {
                    chat.setLocalTitle(localTitle)
                }
                Observable.just(0)
                    .observeOn(Schedulers.io())
                    .doOnNext { chat.setLocalTitle(roomDB.userDao().getUser(chat.participants.first())?.name) }
                    .doOnNext { roomDB.chatDao().setChats(chat) }
                    .doOnNext { setChatLiveData(chat) }
                    .doOnNext { getChatInfo(chat, getMessage) }
                    .subscribe()
            }
        }
    }

    private fun getMessagesFromServer(id: String, isAdditional: Boolean, min: Long, max: Long) {
        d(TAG, "getMessagesFromServer(isAdditional = $isAdditional, $min ~ $max)")
        if ((max != MAX_LONG || min != MIN_LONG) && max - min <= 1) return
        MessageRepository.getMessages(id, min, max) {
            val list = it.documents.asReversed().toObservable()
                .map { doc -> doc.toObject<Message>()!! }
                .observeOn(Schedulers.io())
                .doOnNext { message -> roomDB.messageDao().setMessages(message) }
                .toList().blockingGet()

            isNoAdditionalMessage = list.isEmpty() && isAdditional
            addDateMessage(list, isAdditional)
        }
    }

    fun readySendMessage(msg: String) {
        this.pendingMessage = Message(chatId = chat.value!!.id, body = msg, createdAt = Date(currentTimeMillis()))
        ChatViewModel.instance.selectedChat!!.lastMessage = msg
        ChatViewModel.instance.updateChatLastMessage()
    }

    fun sendFCMMessage(fcmType: FCMType, sdp: String? = null) {
        counterpart?.fcmToken?.let { SendFCM.sendMessage(
            toToken = counterpart!!.fcmToken!!,
            type = fcmType,
            callType = Call.Type.MESSAGE,
            chatId = chat.value?.id,
            messageId = pendingMessage?.id,
            sequence = if (pendingMessage == null) -1 else pendingMessage!!.sequence,
            message = pendingMessage?.body,
            targetOS = counterpart?.os,
            sdp = sdp,
        )}
    }

    private fun startRTP() {
        Observable.just(ApplicationUtil.getContext()!!)
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                rtpManager.init(
                    context = it,
                    isAudio = false,
                    isVideo = false,
                    isDataChannel = true,
                    enableStat = false,
                    recordAudio = false
                )
                rtpManager.startRTP(it, null, isOffer, remoteSDP, remoteICE)
            }.subscribe()
    }

    fun endRTP(fcmType: FCMType = FCMType.Bye) {
        if (fcmType == FCMType.Bye) {
            sendFCMMessage(FCMType.Bye)
        }
        onTerminatedCall()
    }

    fun onIncomingCall(userId: String?, chatId: String?, sdp: String?, fcmToken: String?) {
        userId?.let { chatId?.let {
            if (chat.value == null || chat.value!!.id != chatId) {
                SendFCM.sendMessage(
                    toToken = fcmToken!!,
                    type = FCMType.Decline,
                    callType = Call.Type.MESSAGE,
                    targetOS = counterpart!!.os
                )
                return
            }

            isOffer = false
            remoteSDP = SessionDescription(Type.OFFER, sdp)

            startRTP()
        }}
    }

    fun onAnsweredCall(sdp: String?) {
        sdp?.let { rtpManager.setRemoteDescription(SessionDescription(Type.ANSWER, sdp)) }
    }

    fun onTerminatedCall() {
        rtpManager.release()
    }

    fun onMessageReceived(chatId: String?, userId: String?, messageId: String?, msg: String?) {
        if (userId != null && messageId != null) {
            val message = Message(userId, chatId, messageId, msg, 0, Date(currentTimeMillis()))
            setReceiveMessage(message)
        }
        if (chat.value != null && chat.value!!.id == chatId && ApplicationUtil.getContext() != null) {
            isOffer = true
            startRTP()
        }
    }

    //RTP Event
    fun onIceCandidate(ice: String?) {
        ice?.let { sendFCMMessage(FCMType.Ice, ice) }
    }

    // sync message sequence
    private val chatChangeListener: (Map<String, Any>?) -> Unit = {
        if (pendingMessage?.sequence == -1L) {
            pendingMessage!!.sequence = it!![LAST_SEQUENCE] as Long
            MessageRepository.post(pendingMessage!!) {
                if (rtpConnected) {
                    rtpManager.sendData(pendingMessage!!.toString())
                } else {
                    sendFCMMessage(FCMType.Message)
                }
            }
        }
    }

    init {
        release()
    }

    private object Holder {
        val INSTANCE = MessageVM()
    }

    companion object {
        private const val TAG = "MessageVM"
        val instance: MessageVM by lazy { Holder.INSTANCE }
    }
}