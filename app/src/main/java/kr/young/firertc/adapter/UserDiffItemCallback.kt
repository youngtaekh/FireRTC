package kr.young.firertc.adapter

import androidx.recyclerview.widget.DiffUtil
import kr.young.firertc.model.User

class UserDiffItemCallback: DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User) =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: User, newItem: User) =
        oldItem == newItem
}