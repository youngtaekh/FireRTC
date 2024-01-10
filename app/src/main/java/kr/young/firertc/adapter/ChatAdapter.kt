package kr.young.firertc.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kr.young.common.DateUtil
import kr.young.firertc.R
import kr.young.firertc.databinding.LayoutChatBinding
import kr.young.firertc.model.Chat
import kr.young.firertc.vm.ChatViewModel
import kr.young.firertc.vm.UserViewModel
import java.lang.System.currentTimeMillis

class ChatAdapter : Adapter<ViewHolder>() {
    private var context: Context? = null
    private val chatViewModel = ChatViewModel.instance

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ChatHolder(LayoutChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as ChatHolder).bind(chatViewModel.chatList[position])
    }

    override fun getItemCount() = chatViewModel.chatList.size

    fun setOnItemClickListener(listener: ClickListener, longListener: LongClickListener) {
        mClickListener = listener
        mLongClickListener = longListener
    }

    fun setOnItemClickListener(listener: (Int) -> Unit, longListener: (Int, View) -> Unit) {
        this.mClickListener = object: ClickListener {
            override fun onClick(pos: Int) {
                listener(pos)
            }
        }
        this.mLongClickListener = object: LongClickListener {
            override fun onLongClick(pos: Int, v: View) {
                longListener(pos, v)
            }
        }
    }

    lateinit var mClickListener: ClickListener
    lateinit var mLongClickListener: LongClickListener
    interface ClickListener {
        fun onClick(pos: Int)
    }
    interface LongClickListener {
        fun onLongClick(pos: Int, v: View)
    }

    inner class ChatHolder(val binding: LayoutChatBinding): ViewHolder(binding.root), OnClickListener, OnLongClickListener {
        fun bind(chat: Chat) {
            binding.ivProfile.setImageResource(UserViewModel.instance.selectImage(chat.id))
            if (chat.participants.isEmpty()) {
                binding.tvName.text = context!!.getString(R.string.no_one)
            } else if (chat.isGroup) {
                binding.tvName.text = chat.title
            } else {
                binding.tvName.text = chat.localTitle
            }
            binding.tvMessage.text = chat.lastMessage
            val now = DateUtil.toFormattedString(currentTimeMillis(), "yy-MM-dd")
            val modifiedDate = DateUtil.toFormattedString(chat.modifiedAt!!, "yy-MM-dd")
            binding.tvTime.text = if (now == modifiedDate) {
                DateUtil.toFormattedString(chat.modifiedAt!!, "aa hh:mm")
            } else {
                modifiedDate
            }
        }

        override fun onClick(v: View?) {
            mClickListener.onClick(adapterPosition)
        }

        override fun onLongClick(v: View?): Boolean {
            mLongClickListener.onLongClick(adapterPosition, itemView)
            return true
        }

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }
    }
}