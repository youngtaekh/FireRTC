package kr.young.firertc.fragment

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.AddChatActivity
import kr.young.firertc.MessageActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.ChatAdapter
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.Chat
import kr.young.firertc.vm.ChatViewModel
import kr.young.firertc.vm.MessageVM

class ChatFragment : Fragment(), OnClickListener {
    private val chatVM = ChatViewModel.instance
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
            chatVM.getChats()
            swipe.isRefreshing = false
        }

        chatVM.chatList.observe(viewLifecycleOwner) {
            d(TAG, "chatList observe size ${it.size}")
            chatAdapter.submitList(it)
            tvEmpty.visibility = if (it.isEmpty()) VISIBLE else INVISIBLE
            recyclerView.visibility = if (it.isEmpty()) INVISIBLE else VISIBLE
        }

        MessageVM.instance.receiveMessage.observe(viewLifecycleOwner) {
            it?.let {
                for (i in chatVM.chatList.value!!.indices) {
                    if (chatVM.chatList.value!![i].id == it.chatId) {
                        chatVM.chatList.value!![i].lastMessage = it.body!!
                        chatVM.chatList.value!![i].lastSequence = it.sequence
                        chatVM.chatList.value!![i].modifiedAt = it.createdAt
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

    private fun removeChat(pos: Int) {
        val chat = chatAdapter.currentList[pos]
        Observable.just(chat)
            .observeOn(Schedulers.io())
            .doOnNext { AppRoomDatabase.getInstance()!!.chatDao().delete(chat) }
            .subscribe()
        val list = mutableListOf<Chat>()
        list.addAll(chatVM.chatList.value!!)
        list.removeAt(pos)
        chatVM.setChatListLiveData(list)
    }

    private val listener = { it: Int ->
        val chat = chatAdapter.currentList[it]
        d(TAG, "clickListener $chat")
        MessageVM.instance.setChatLiveData(chat)
        val intent = Intent(this@ChatFragment.context, MessageActivity::class.java)
        intent.flags = FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private val longListener = { it: Int, _: View ->
        d(TAG, "longClickListener $it")
        val chat = chatAdapter.currentList[it]
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(chat.localTitle)
            .setMessage(getString(R.string.quit_chat))
            .setCancelable(true)
            .setPositiveButton(
                R.string.confirm
            ) { _, _ -> removeChat(it) }
        builder.show()
    }

    companion object {
        private const val TAG = "ChatFragment"
    }
}