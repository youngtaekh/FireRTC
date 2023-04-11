package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kr.young.firertc.model.Chat
import kr.young.firertc.model.Message
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.MessageRepository

class ChatViewModel private constructor(): ViewModel() {

    val responseCode = MutableLiveData<Int>()
    var chatList: List<Chat>? = null
    var selectedChat: Chat? = null
    var messageList: MutableList<Message>? = null

    fun setResponseCode(code: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = code }
    }

    fun createChat(chat: Chat) {
        ChatRepository.post(chat)
    }

    fun getChats() {
        if (MyDataViewModel.instance.myData != null) {
            ChatRepository.getChats(MyDataViewModel.instance.getMyId())
        }
    }

    fun getChat(id: String) {
        ChatRepository.getChat(id)
    }

    fun updateChatModifiedAt() {
        if (selectedChat != null) {
            ChatRepository.updateModifiedAt(selectedChat!!)
        }
    }

    fun createMessage(message: Message) {
        MessageRepository.post(message)
    }

    fun getMessages() {
        if (selectedChat != null) {
            MessageRepository.getMessages(selectedChat!!.id)
        }
    }

    fun release() {
        setResponseCode(0)
    }

    init {
        setResponseCode(0)
    }

    private object Holder {
        val INSTANCE = ChatViewModel()
    }

    companion object {
        private const val TAG = "ChatViewModel"
        val instance: ChatViewModel by lazy { Holder.INSTANCE }
    }
}