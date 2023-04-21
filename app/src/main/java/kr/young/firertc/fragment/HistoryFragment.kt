package kr.young.firertc.fragment

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.CallDetailActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.HistoryAdapter
import kr.young.firertc.adapter.StickyHeaderAdapter
import kr.young.firertc.adapter.StickyHeaderItemDecoration
import kr.young.firertc.model.Call
import kr.young.firertc.repo.CallRepository.Companion.CALL_READ_SUCCESS
import kr.young.firertc.vm.CallVM

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
        val fabAdd: FloatingActionButton = layout.findViewById(R.id.fab_add)
        fabAdd.visibility = INVISIBLE

        swipe.setOnRefreshListener {
            d(TAG, "history swipe refresh")
            callViewModel.getHistory()
            swipe.isRefreshing = false
        }

        adapter = StickyHeaderAdapter(historyList)

        historyAdapter = HistoryAdapter(historyList)
        historyAdapter.setOnItemClickListener(clickListener, longClickListener)
        recyclerView.adapter = historyAdapter

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = VERTICAL
        recyclerView.layoutManager = layoutManager

        callViewModel.responseCode.observe(viewLifecycleOwner) {
            if (it != null && it == CALL_READ_SUCCESS) {
                if (historyList.isEmpty()) {
                    historyList.addAll(callViewModel.historyList)
                    historyAdapter.notifyItemRangeInserted(0, historyList.size)
                } else if (historyList.size < callViewModel.historyList.size) {
                    var idx = 0
                    var flag = true
                    while (flag) {
                        val pCall = historyList[idx]
                        val nCall = callViewModel.historyList[idx]
                        if (pCall.isHeader && nCall.isHeader) {
                            historyList.removeAt(idx)
                            historyList.add(idx, nCall)
                            historyAdapter.notifyItemChanged(idx)
                            idx += 1
                        } else if (pCall == nCall) {
                            flag = false
                        } else {
                            historyList.add(idx, nCall)
                            historyAdapter.notifyItemInserted(idx)
                            idx += 1
                        }
                    }
                } else if (historyList.size > callViewModel.historyList.size) {
                    val size = historyList.size
                    historyList.removeAll { true }
                    historyAdapter.notifyItemRangeRemoved(0, size)
                    historyList.addAll(callViewModel.historyList)
                    historyAdapter.notifyItemRangeInserted(0, historyList.size)
                }
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