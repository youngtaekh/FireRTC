package kr.young.firertc.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.squareup.picasso.Picasso
import kr.young.common.ApplicationUtil
import kr.young.firertc.R
import kr.young.firertc.model.User
import kr.young.firertc.util.CircleTransform
import kr.young.firertc.util.ImageUtil.Companion.selectImageFromWeb

class ContactAdapter: ListAdapter<User, ViewHolder>(UserDiffItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflatedView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.layout_contact, parent, false)
        return ContactHolder(inflatedView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = getItem(position)
        (holder as ContactHolder).tvName.text = contact.name
        val requestOption = RequestOptions().transform(CenterCrop(), RoundedCorners(50))
        Glide.with(ApplicationUtil.getContext()!!)
            .load(selectImageFromWeb(contact.id))
            .placeholder(R.drawable.profile_placeholder)
            .error(R.drawable.outline_mood_24)
            .apply(requestOption)
            .into(holder.ivProfile)
    }

    override fun getItemCount() = currentList.size

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