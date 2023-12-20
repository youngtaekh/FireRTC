package kr.young.firertc.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.AddChatActivity
import kr.young.firertc.MessageActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.ChatAdapter
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.User
import kr.young.firertc.repo.ChatRepository.Companion.CHAT_READ_SUCCESS
import kr.young.firertc.vm.ChatViewModel
import kr.young.firertc.vm.MessageViewModel
import kr.young.firertc.vm.MyDataViewModel
import kr.young.firertc.vm.UserViewModel

class ChatFragment : Fragment(), OnClickListener {
    private val chatViewModel = ChatViewModel.instance
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val layout = inflater.inflate(R.layout.fragment_home, container, false)
        val swipe = layout.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val recyclerView: RecyclerView = layout.findViewById(R.id.recycler_view)
        val tvEmpty: TextView = layout.findViewById(R.id.tv_empty)
        val fabAdd: FloatingActionButton = layout.findViewById(R.id.fab_add)
        fabAdd.setOnClickListener(this)

        chatAdapter = ChatAdapter()
        chatAdapter.setOnItemClickListener(listener, longListener)
        recyclerView.adapter = chatAdapter

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager

        swipe.setOnRefreshListener {
            d(TAG, "chat swipe refresh")
            chatViewModel.getChats()
            swipe.isRefreshing = false
        }

        chatViewModel.responseCode.observe(viewLifecycleOwner) {
            if (it != null && it == CHAT_READ_SUCCESS) {
                d(TAG, "response code CHAT_READ_SUCCESS")
                if (chatViewModel.chatList.isEmpty()) {
                    tvEmpty.visibility = VISIBLE
                    recyclerView.visibility = INVISIBLE
                } else {
                    tvEmpty.visibility = INVISIBLE
                    recyclerView.visibility = VISIBLE
                    chatAdapter.notifyItemRangeChanged(0, chatViewModel.chatList.size)
                }
            }
        }

        MessageViewModel.instance.receivedMessage.observe(viewLifecycleOwner) {
            if (it != null) {
                for (i in chatViewModel.chatList.indices) {
                    if (chatViewModel.chatList[i].id == it.chatId) {
                        chatViewModel.chatList[i].lastMessage = it.body!!
                        chatViewModel.chatList[i].lastSequence = it.sequence
                        chatViewModel.chatList[i].modifiedAt = it.createdAt
                        chatAdapter.notifyItemChanged(i)
                    }
                }
            }
        }

        return layout
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab_add -> { startActivity(Intent(context, AddChatActivity::class.java)) }
        }
    }

    private fun startMessageActivity() {
        val intent = Intent(this@ChatFragment.context, MessageActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private val listener = { it: Int ->
        val chat = chatViewModel.chatList[it]
        d(TAG, "clickListener $chat")
        if (!chat.isGroup) {
            var participant = ""
            for (i in chat.participants.indices) {
                participant = chat.participants[i]
                if (participant != MyDataViewModel.instance.getMyId()) {
                    break
                }
            }
            Observable.just(0)
                .observeOn(Schedulers.io())
                .map {
                    val user = AppRoomDatabase.getInstance()!!.userDao().getUser(participant)
                    if (user != null) {
                        MessageViewModel.instance.startChat(user) { startMessageActivity() }
                        user
                    } else {
                        User()
                    }
                }
                .filter { user -> user.id.isEmpty() }
                .map {
                    UserViewModel.instance.readUser(participant) {
                        MessageViewModel.instance.startChat(it.toObject<User>()) { startMessageActivity() }
                    }
                }
                .subscribe()
        }
    }

    private val longListener = { it: Int, _: View ->
        d(TAG, "longClickListener $it")
    }

    companion object {
        private const val TAG = "ChatFragment"
    }
}