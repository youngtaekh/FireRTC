package kr.young.firertc.adapter

import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kr.young.firertc.R
import kr.young.firertc.databinding.LayoutAddChatBinding
import kr.young.firertc.model.User
import kr.young.firertc.vm.UserViewModel

class AddChatAdapter(private val contacts: MutableList<AddUser>): Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return AddChatHolder(LayoutAddChatBinding.inflate(from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as AddChatHolder).bind(contacts[position])
    }

    override fun getItemCount() = contacts.size

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        this.mClickListener = object: ClickListener {
            override fun onClick(pos: Int) {
                listener(pos)
            }
        }
    }

    lateinit var mClickListener: ClickListener
    interface ClickListener {
        fun onClick(pos: Int)
    }

    inner class AddChatHolder(
        private val binding: LayoutAddChatBinding
        ): ViewHolder(binding.root), View.OnClickListener {

        fun bind(contact: AddUser) {
            binding.ivProfile.setImageResource(UserViewModel.instance.selectImage(contact.user.id))
            binding.tvName.text = contact.user.name
            if (contact.checked) {
                binding.ivCheck.setImageResource(R.drawable.outline_check_box_24)
            } else {
                binding.ivCheck.setImageResource(R.drawable.outline_check_box_blank_24)
            }
        }

        override fun onClick(v: View?) {
            mClickListener.onClick(adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }

    data class AddUser(val user: User, var checked: Boolean = false)
}