package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import kr.young.firertc.model.Chat
import kr.young.firertc.model.Message
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.ChatRepository.Companion.CHAT_READ_SUCCESS
import kr.young.firertc.repo.MessageRepository

class ChatViewModel private constructor(): ViewModel() {

    val responseCode = MutableLiveData<Int>()
    var chatList = mutableListOf<Chat>()
    val selectedChat: Chat? = null
        get() { return field ?: MessageViewModel.instance.chat }
    var messageList: MutableList<Message>? = null

    fun setResponseCode(code: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = code }
    }

    fun createChat(chat: Chat) {
        ChatRepository.post(chat)
    }

    fun getChats() {
        if (MyDataViewModel.instance.myData != null) {
            ChatRepository.getChats(MyDataViewModel.instance.getMyId()) {
                val list = mutableListOf<Chat>()
                for (chatDoc in it) {
                    list.add(chatDoc.toObject())
                }
                chatList = list
                setResponseCode(CHAT_READ_SUCCESS)
            }
        }
    }

    fun getChat(id: String, success: OnSuccessListener<DocumentSnapshot>) {
        ChatRepository.getChat(id, success = success)
    }

    fun updateChatModifiedAt() {
        if (selectedChat != null) {
            ChatRepository.updateModifiedAt(selectedChat!!)
        }
    }

    fun updateChatLastMessage() {
        if (selectedChat != null) {
            ChatRepository.updateLastMessage(selectedChat!!)
        }
    }

    fun createMessage(message: Message) {
        MessageRepository.post(message)
    }

    fun getMessages() {
        if (selectedChat != null) {
            MessageRepository.getMessages(selectedChat!!.id!!)
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