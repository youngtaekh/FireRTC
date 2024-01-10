package kr.young.firertc.adapter

import androidx.recyclerview.widget.DiffUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.model.Message

class MessageDiffItemCallback: DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Message, newItem: Message) =
        oldItem == newItem
}