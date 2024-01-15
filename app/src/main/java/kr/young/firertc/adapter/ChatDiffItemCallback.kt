package kr.young.firertc.adapter

import androidx.recyclerview.widget.DiffUtil
import kr.young.firertc.model.Chat

class ChatDiffItemCallback: DiffUtil.ItemCallback<Chat>() {
    override fun areItemsTheSame(oldItem: Chat, newItem: Chat) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Chat, newItem: Chat) =
        oldItem == newItem
}