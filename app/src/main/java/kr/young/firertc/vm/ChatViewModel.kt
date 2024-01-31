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
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.Chat
import kr.young.firertc.repo.ChatRepository
import kr.young.firertc.repo.ChatRepository.Companion.CHAT_READ_SUCCESS

class ChatViewModel private constructor(): ViewModel() {

    val responseCode = MutableLiveData<Int>()
    var chatList = MutableLiveData(listOf<Chat>())
    val selectedChat: Chat? = null
        get() { return field ?: MessageVM.instance.chat.value }

    fun setChatListLiveData(list: List<Chat>) {
        Handler(Looper.getMainLooper()).post { chatList.value = list }
    }

    fun setResponseCode(code: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = code }
    }

    fun getChats() {
        var list = Observable.just(0)
            .observeOn(Schedulers.io())
            .flatMap { AppRoomDatabase.getInstance()!!.chatDao().getChats().toObservable() }
            .toList().blockingGet()
        addChats(list)

        if (MyDataViewModel.instance.myData.value != null) {
            ChatRepository.getChats(MyDataViewModel.instance.getMyId()) {
                list = it.toObservable()
                    .observeOn(Schedulers.computation())
                    .map { doc -> doc.toObject<Chat>() }
                    .map { chat -> chat.removeParticipants(MyDataViewModel.instance.getMyId()) }
                    .observeOn(Schedulers.io())
                    .map { chat -> chat.setLocalTitle(AppRoomDatabase.getInstance()!!.userDao().getUser(chat.participants.first())?.name) }
                    .doOnNext { chat -> AppRoomDatabase.getInstance()!!.chatDao().setChats(chat) }
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
        d(TAG, "addChats(${list.size})")
        setChatListLiveData(list.sortedByDescending { it.modifiedAt })
    }

    fun release() {
        setResponseCode(0)
    }

    init {
        release()
    }

    private object Holder {
        val INSTANCE = ChatViewModel()
    }

    companion object {
        private const val TAG = "ChatViewModel"
        val instance: ChatViewModel by lazy { Holder.INSTANCE }
    }
}