package kr.young.firertc.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.AddChatActivity
import kr.young.firertc.MessageActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.ChatAdapter
import kr.young.firertc.model.Chat
import kr.young.firertc.model.User
import kr.young.firertc.repo.ChatRepository.Companion.CHAT_READ_SUCCESS
import kr.young.firertc.vm.ChatViewModel
import kr.young.firertc.vm.MessageViewModel
import kr.young.firertc.vm.MyDataViewModel
import kr.young.firertc.vm.UserViewModel

class ChatFragment : Fragment(), OnClickListener {
    private val chatViewModel = ChatViewModel.instance
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var itemList :MutableList<Chat>

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
        itemList = chatViewModel.chatList

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = layoutManager

        swipe.setOnRefreshListener {
            d(TAG, "chat swipe refresh")
            chatViewModel.getChats()
            swipe.isRefreshing = false
        }

        chatViewModel.responseCode.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it == CHAT_READ_SUCCESS) {
                    d(TAG, "response code CHAT_READ_SUCCESS")
                    if (chatViewModel.chatList.isEmpty()) {
                        tvEmpty.visibility = VISIBLE
                        chatAdapter.notifyItemRangeRemoved(0, itemList.size)
                    } else {
                        tvEmpty.visibility = INVISIBLE
                        if (itemList.size != chatViewModel.chatList.size) {
                            chatAdapter.notifyItemRangeRemoved(0, itemList.size)
                            chatAdapter.notifyItemRangeInserted(0, chatViewModel.chatList.size)
                        } else {
                            for (i in chatViewModel.chatList.indices) {
                                if (itemList[i] != chatViewModel.chatList[i]) {
                                    d(TAG, "$i different")
                                    chatAdapter.notifyItemChanged(i)
                                } else {
                                    d(TAG, "$i same")
                                }
                            }
                        }
                    }
                    itemList = chatViewModel.chatList
                }
            }
        }

        return layout
    }

    override fun onResume() {
        super.onResume()
        chatViewModel.getChats()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab_add -> { startActivity(Intent(context, AddChatActivity::class.java)) }
        }
    }

    private val listener = { it: Int ->
        val chat = chatViewModel.chatList[it]
        d(TAG, "clickListener $chat")
        if (!chat.isGroup) {
            for (participant in chat.participants) {
                if (participant != MyDataViewModel.instance.getMyId()) {
                    val user = UserViewModel.instance.getLocalUser(participant)
                    if (user != null) {
                        MessageViewModel.instance.startOffer(user) {
                            val intent = Intent(this@ChatFragment.context, MessageActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            startActivity(intent)
                        }
                    } else {
                        UserViewModel.instance.readUser(participant) {
                            val userDoc = it.toObject<User>()
                            if (userDoc != null) {
                                MessageViewModel.instance.startOffer(userDoc) {
                                    val intent = Intent(
                                        this@ChatFragment.context,
                                        MessageActivity::class.java
                                    )
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    startActivity(intent)
                                }
                            }
                        }
                    }
                    break
                }
            }
        }
    }

    private val longListener = { it: Int, _: View ->
        d(TAG, "longClickListener $it")
    }

    companion object {
        private const val TAG = "ChatFragment"
    }
}