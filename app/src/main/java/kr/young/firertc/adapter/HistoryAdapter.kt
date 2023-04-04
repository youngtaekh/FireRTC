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
import kr.young.common.DateUtil
import kr.young.firertc.R
import kr.young.firertc.model.Call
import java.util.*

class HistoryAdapter(private val calls: MutableList<Call>): Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            HistoryHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_history, parent, false))
        } else {
            HistoryDateHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_history_date, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val call = calls[position]
        if (call.isHeader) {
            (holder as HistoryDateHolder).tvDate.text = DateUtil.toFormattedString(call.createdAt!!, "yy.MM.dd", TimeZone.getDefault())
        } else {
            (holder as HistoryHolder).tvName.text = call.counterpartName ?: "No name"
            holder.tvTime.text =
                DateUtil.toFormattedString(call.createdAt!!, "aa hh:mm", TimeZone.getDefault())
            if (call.direction == Call.Direction.Offer) {
                holder.ivStatus.setImageResource(R.drawable.round_call_made_24)
            } else if (call.connected) {
                holder.ivStatus.setImageResource(R.drawable.round_call_received_24)
            } else {
                holder.ivStatus.setImageResource(R.drawable.round_call_missed_24)
            }
            if (call.type == Call.Type.AUDIO) {
                holder.ivCategory.setImageResource(R.drawable.round_call_24)
            } else {
                holder.ivCategory.setImageResource(R.drawable.round_videocam_24)
            }
        }
    }

    override fun getItemCount() = calls.size

    override fun getItemViewType(position: Int): Int {
        return if (calls[position].isHeader) {
            1
        } else {
            0
        }
    }

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

    inner class HistoryHolder(itemView: View): ViewHolder(itemView), OnClickListener, OnLongClickListener {
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val ivStatus: ImageView = itemView.findViewById(R.id.iv_status)
        val ivCategory: ImageView = itemView.findViewById(R.id.iv_category)

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

    inner class HistoryDateHolder(itemView: View): ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
    }
}