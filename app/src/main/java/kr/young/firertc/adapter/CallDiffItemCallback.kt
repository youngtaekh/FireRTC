package kr.young.firertc.adapter

import androidx.recyclerview.widget.DiffUtil
import kr.young.firertc.model.Call

class CallDiffItemCallback: DiffUtil.ItemCallback<Call>() {
    override fun areItemsTheSame(oldItem: Call, newItem: Call) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Call, newItem: Call) =
        oldItem == newItem
}