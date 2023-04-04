package kr.young.firertc.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kr.young.firertc.R
import kr.young.firertc.model.User
import kr.young.firertc.vm.UserViewModel

class ContactAdapter(private val contacts: MutableList<User>): Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflatedView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.layout_contact, parent, false)
        return ContactHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        (holder as ContactHolder).tvName.text = contact.name
        holder.ivProfile.setImageResource(UserViewModel.instance.selectImage(contact.id))
    }

    override fun getItemCount() = contacts.size

    fun setOnItemClickListener(listener: ClickListener, longListener: LongClickListener) {
        mClickListener = listener
        mLongClickListener = longListener
    }

    lateinit var mClickListener: ClickListener
    lateinit var mLongClickListener: LongClickListener
    interface ClickListener {
        fun onClick(pos: Int, v: View)
    }
    interface LongClickListener {
        fun onLongClick(pos: Int, v: View)
    }

    inner class ContactHolder(itemView: View): ViewHolder(itemView), OnClickListener, OnLongClickListener {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val ivProfile: ImageView = itemView.findViewById(R.id.iv_profile)

        override fun onClick(v: View?) {
            mClickListener.onClick(adapterPosition, itemView)
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