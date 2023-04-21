package kr.young.firertc.adapter

import android.view.LayoutInflater.from
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kr.young.firertc.databinding.LayoutAddedChatBinding
import kr.young.firertc.model.User

class AddedChatAdapter(private val contacts: MutableList<User>): Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return AddedChatHolder(LayoutAddedChatBinding.inflate(from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as AddedChatHolder).bind(contacts[position])
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

    inner class AddedChatHolder(
        private val binding: LayoutAddedChatBinding
        ): ViewHolder(binding.root), View.OnClickListener {

        fun bind(contact: User) {
            binding.tvName.text = contact.name
        }

        override fun onClick(v: View?) {
            mClickListener.onClick(adapterPosition)
        }

        init {
            itemView.setOnClickListener(this)
        }
    }
}