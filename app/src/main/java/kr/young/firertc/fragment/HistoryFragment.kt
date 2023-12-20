package kr.young.firertc.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.CallDetailActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.HistoryAdapter
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*
import kr.young.firertc.vm.CallVM
import kr.young.firertc.vm.HistoryVM

class HistoryFragment : Fragment() {
    private val callViewModel = CallVM.instance
    private val historyVM = HistoryVM.getInstance()!!
    private lateinit var historyAdapter: HistoryAdapter

    private var isBottom = true
    private var lastVisiblePosition = -1
    private var isLoading = false

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
            historyVM.getHistory()
            swipe.isRefreshing = false
        }

        historyAdapter = HistoryAdapter(historyVM.list)
        historyAdapter.setOnItemClickListener(clickListener, longClickListener)
        recyclerView.adapter = historyAdapter

        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = VERTICAL
        recyclerView.layoutManager = layoutManager

        recyclerView.addOnScrollListener(object: OnScrollListener() {
            var dragging = false
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dragging) {
                    if (layoutManager.findLastCompletelyVisibleItemPosition() != lastVisiblePosition) {
                        lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition()
                        isBottom = layoutManager.findLastCompletelyVisibleItemPosition() == historyVM.list.size - 1

                        if (layoutManager.findLastCompletelyVisibleItemPosition() < 20 &&
                            !isLoading &&
                            !historyVM.isEndReload
                        ) {
                            isLoading = true
                            d(TAG, "scroll Additional history ${layoutManager.findLastCompletelyVisibleItemPosition()}")
                            historyVM.getHistory(true)
                        }
                    }
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                dragging = true
            }
        })

        historyVM.notifier.observe(viewLifecycleOwner) {
            tvEmpty.visibility = if (historyVM.list.isEmpty()) VISIBLE else INVISIBLE
            it?.let {
                d(TAG, "historyList ${it.position} ${it.count} ${it.modifierCategory}")
                when (it.modifierCategory) {
                    Insert -> historyAdapter.notifyItemRangeInserted(it.position, it.count)
                    Removed -> historyAdapter.notifyItemRangeRemoved(it.position, it.count)
                    Changed -> historyAdapter.notifyItemRangeChanged(it.position, it.count)
                }
                if (it.isBottom) {
                    recyclerView.scrollToPosition(0)
                }
            }
        }

        return layout
    }

    override fun onResume() {
        super.onResume()
        d(TAG, "onResume")
    }

    private fun removeHistory(pos: Int) {
        d(TAG, "removeHistory($pos)")
        historyVM.list.removeAt(pos)
        historyAdapter.notifyItemRemoved(pos)
    }

    private val clickListener = object: HistoryAdapter.ClickListener {
        override fun onClick(pos: Int, v: View) {
            d(TAG, "onClick($pos, v)")
            callViewModel.selectedCall = historyVM.list[pos]
            startActivity(Intent(context, CallDetailActivity::class.java))
        }
    }

    private val longClickListener = object: HistoryAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v)")
            val call = historyVM.list[pos]
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