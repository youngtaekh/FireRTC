package kr.young.firertc.adapter

import android.view.LayoutInflater
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kr.young.common.DateUtil
import kr.young.firertc.databinding.LayoutRecvMessageBinding
import kr.young.firertc.databinding.LayoutSendMessageBinding
import kr.young.firertc.model.Message
import kr.young.firertc.vm.MyDataViewModel

class MessageAdapter(private val list: List<Message>): Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            SEND -> SendViewHolder(LayoutSendMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> RecvViewHolder(LayoutRecvMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is SendViewHolder -> holder.bind(list[position])
            is RecvViewHolder -> holder.bind(list[position])
        }
    }

    override fun getItemCount() = list.size

    override fun getItemViewType(position: Int): Int {
        return if (list[position].from == MyDataViewModel.instance.getMyId()) {
            SEND
        } else {
            RECV
        }
    }

    companion object {
        private const val TAG = "MessageAdapter"
        private const val SEND = 0
        private const val RECV = 1
    }

    class SendViewHolder(private val binding: LayoutSendMessageBinding): ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.body
            if (message.timeFlag) {
                binding.tvTime.visibility = VISIBLE
                binding.tvTime.text = DateUtil.toFormattedString(message.createdAt!!, "aa hh:mm")
            } else {
                binding.tvTime.visibility = INVISIBLE
            }
        }
    }

    class RecvViewHolder(private val binding: LayoutRecvMessageBinding): ViewHolder(binding.root) {
        fun bind(message: Message) {
            binding.tvMessage.text = message.body
            if (message.timeFlag) {
                binding.tvTime.visibility = VISIBLE
                binding.tvTime.text = DateUtil.toFormattedString(message.createdAt!!, "aa hh:mm")
            } else {
                binding.tvTime.visibility = INVISIBLE
            }
        }
    }
}