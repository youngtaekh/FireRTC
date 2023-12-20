package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kr.young.firertc.db.AppRoomDatabase
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

    fun setResponseCode(code: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = code }
    }

    fun getChats() {
        var list = Observable.just(0)
            .observeOn(Schedulers.io())
            .flatMap { AppRoomDatabase.getInstance()!!.chatDao().getChats().toObservable() }
            .toList().blockingGet()
        addChats(list)

        if (MyDataViewModel.instance.myData != null) {
            ChatRepository.getChats(MyDataViewModel.instance.getMyId()) {
                list = it.toObservable()
                    .observeOn(Schedulers.computation())
                    .map { doc -> doc.toObject<Chat>() }
                    .observeOn(Schedulers.io())
                    .map { chat ->
                        AppRoomDatabase.getInstance()!!.chatDao().setChat(chat)
                        chat
                    }
                    .toList().blockingGet()
                addChats(list)
            }
        }
    }

    fun getChat(id: String, success: OnSuccessListener<DocumentSnapshot>) {
        ChatRepository.getChat(id, success = success)
    }

    fun updateChatLastMessage(chat: Chat? = null) {
        if (chat != null) {
            ChatRepository.updateLastMessage(chat)
        } else if (selectedChat != null) {
            ChatRepository.updateLastMessage(selectedChat!!)
        }
    }

    private fun addChats(list: List<Chat>) {
        var i = 0
        var j = 0
        val copies = mutableListOf<Chat>()
        copies.addAll(chatList)
        copies.sortBy { chat -> chat.id }
        val sortedList = list.sortedBy { chat -> chat.id }
        while (j < sortedList.size) {
            if (i == copies.size || copies[i].id != sortedList[j].id) {
                copies.add(i++, sortedList[j++])
            } else {
                copies[i++] = sortedList[j++]
            }
        }
        chatList.removeAll { true }
        chatList.addAll(copies)
        chatList.sortBy { chat -> chat.modifiedAt }
        setResponseCode(CHAT_READ_SUCCESS)
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