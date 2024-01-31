package kr.young.firertc.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.core.graphics.drawable.RoundedBitmapDrawable
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import kr.young.common.DateUtil
import kr.young.firertc.R
import kr.young.firertc.databinding.LayoutChatBinding
import kr.young.firertc.model.Chat
import kr.young.firertc.util.ImageUtil.Companion.selectImageFromWeb
import kr.young.firertc.vm.ChatViewModel
import kr.young.firertc.vm.UserViewModel
import java.lang.System.currentTimeMillis

class ChatAdapter : ListAdapter<Chat, ViewHolder>(ChatDiffItemCallback()) {
    private var context: Context? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        return ChatHolder(LayoutChatBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as ChatHolder).bind(getItem(position))
    }

    override fun getItemCount() = currentList.size

    fun setOnItemClickListener(listener: ClickListener, longListener: LongClickListener) {
        mClickListener = listener
        mLongClickListener = longListener
    }

    fun setOnItemClickListener(listener: (Int) -> Unit, longListener: (Int, View) -> Any) {
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
            val requestOption = RequestOptions().transform(CenterCrop(), RoundedCorners(50))
            Glide.with(context!!)
                .load(selectImageFromWeb(chat.id))
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.outline_mood_24)
                .apply(requestOption)
                .into(binding.ivProfile)
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