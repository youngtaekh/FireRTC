package kr.young.firertc.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.young.common.DateUtil
import kr.young.firertc.databinding.LayoutHistoryBinding
import kr.young.firertc.databinding.LayoutHistoryDateBinding
import kr.young.firertc.model.Call
import java.util.*

class StickyHeaderAdapter(
    private val items: List<Call>
) : RecyclerView.Adapter<StickyHeaderAdapter.StickyHeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = StickyHeaderViewHolder(
        LayoutHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: StickyHeaderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class StickyHeaderViewHolder(
        private val binding: LayoutHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(call: Call) {
            binding.tvName.text = call.counterpartName
            binding.tvTime.text = DateUtil.toFormattedString(call.createdAt!!, "aa hh:mm", TimeZone.getDefault())
        }
    }

    fun isHeader(position: Int) = items[position].connected

    fun getHeaderView(list: RecyclerView, position: Int): View? {
        val item = items[position]

        val binding = LayoutHistoryDateBinding.inflate(LayoutInflater.from(list.context), list, false)
        binding.tvDate.text = DateUtil.toFormattedString(item.createdAt!!, "yy.MM.dd", TimeZone.getDefault())
        return binding.root
    }
}