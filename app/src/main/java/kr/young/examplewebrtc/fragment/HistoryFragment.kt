package kr.young.examplewebrtc.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.CallDetailActivity
import kr.young.examplewebrtc.R
import kr.young.examplewebrtc.adapter.HistoryAdapter
import kr.young.examplewebrtc.adapter.StickyHeaderAdapter
import kr.young.examplewebrtc.adapter.StickyHeaderItemDecoration
import kr.young.examplewebrtc.model.Call
import kr.young.examplewebrtc.repo.CallRepository.Companion.CALL_READ_SUCCESS
import kr.young.examplewebrtc.vm.CallVM
import org.zakariya.stickyheaders.StickyHeaderLayoutManager
import java.util.*

class HistoryFragment : Fragment() {
    private val callViewModel = CallVM.instance
    private var historyList = mutableListOf<Call>()
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var adapter: StickyHeaderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_home, container, false)
        val swipe = layout.findViewById<SwipeRefreshLayout>(R.id.swipe)
        val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
        val tvEmpty = layout.findViewById<TextView>(R.id.tv_empty)

        swipe.setOnRefreshListener {
            d(TAG, "history swipe refresh")
            callViewModel.getHistory()
            swipe.isRefreshing = false
        }

        adapter = StickyHeaderAdapter(historyList)

        historyAdapter = HistoryAdapter(historyList)
        historyAdapter.setOnItemClickListener(clickListener, longClickListener)
        recyclerView.adapter = historyAdapter
//        recyclerView.addItemDecoration(DividerItemDecoration(context, VERTICAL))

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = VERTICAL
        recyclerView.layoutManager = layoutManager

//        recyclerView.addItemDecoration(StickyHeaderItemDecoration(getSectionCallback()))

        callViewModel.responseCode.observe(viewLifecycleOwner) {
            if (it != null && it == CALL_READ_SUCCESS) {
                val size = historyList.size
                historyList.removeAll { true }
                historyAdapter.notifyItemRangeRemoved(0, size)
                adapter.notifyItemRangeRemoved(0, size)
                historyList.addAll(callViewModel.historyList)
                historyAdapter.notifyItemRangeInserted(0, historyList.size)
                adapter.notifyItemRangeInserted(0, historyList.size)
                if (historyList.isEmpty()) {
                    tvEmpty.visibility = VISIBLE
                } else {
                    tvEmpty.visibility = INVISIBLE
                }
            }
        }

        // Inflate the layout for this fragment
        return layout
    }

    override fun onResume() {
        super.onResume()
        d(TAG, "onResume")

        callViewModel.getHistory()
    }

    private fun removeHistory(pos: Int) {
        d(TAG, "removeHistory($pos)")
        historyList.removeAt(pos)
        historyAdapter.notifyItemRemoved(pos)
    }

    private fun getSectionCallback(): StickyHeaderItemDecoration.SectionCallback {
        return object : StickyHeaderItemDecoration.SectionCallback {
            override fun isHeader(position: Int): Boolean {
                return adapter.isHeader(position)
            }

            override fun getHeaderLayoutView(list: RecyclerView, position: Int): View? {
                return adapter.getHeaderView(list, position)
            }
        }
    }

    private val clickListener = object: HistoryAdapter.ClickListener {
        override fun onClick(pos: Int, v: View) {
            d(TAG, "onClick($pos, v)")
            callViewModel.selectedCall = historyList[pos]
            startActivity(Intent(context, CallDetailActivity::class.java))
        }
    }

    private val longClickListener = object: HistoryAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v)")
            val call = historyList[pos]
            val builder = AlertDialog.Builder(context!!)
                .setTitle(call.counterpartName ?: "No name")
                .setMessage(R.string.delete_history)
                .setCancelable(true)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    removeHistory(pos)
                }
            builder.show()
        }
    }

    companion object {
        private const val TAG = "HistoryFragment"
    }
}