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
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.CallDetailActivity
import kr.young.firertc.R
import kr.young.firertc.adapter.HistoryAdapter
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.Call
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

        historyAdapter = HistoryAdapter()
        historyAdapter.setOnItemClickListener(clickListener, longClickListener)
        recyclerView.adapter = historyAdapter
        historyAdapter.registerAdapterDataObserver(adapterObserver)

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

                        if (layoutManager.findLastCompletelyVisibleItemPosition() > historyAdapter.currentList.size - 20 &&
                            !isLoading &&
                            !historyVM.isEndReload
                        ) {
                            isLoading = true
                            d(TAG, "scroll Additional history ${historyAdapter.currentList.size} ${layoutManager.findLastCompletelyVisibleItemPosition()}")
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

        historyVM.historyList.observe(viewLifecycleOwner) {
            d(TAG, "list observe size ${it.size}")
            historyAdapter.submitList(it)
            tvEmpty.visibility = if (it.isEmpty()) VISIBLE else INVISIBLE
            recyclerView.visibility = if (it.isEmpty()) INVISIBLE else VISIBLE
        }

        return layout
    }

    override fun onResume() {
        super.onResume()
        d(TAG, "onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        d(TAG, "onDestroy")
        historyAdapter.unregisterAdapterDataObserver(adapterObserver)
    }

    private fun removeHistory(pos: Int) {
        d(TAG, "removeHistory($pos)")
        val call = historyAdapter.currentList[pos]
        Observable.just(call)
            .observeOn(Schedulers.io())
            .map { AppRoomDatabase.getInstance()!!.callDao().deleteCall(it) }
            .subscribe()
        val list = mutableListOf<Call>()
        list.removeAt(pos)
        historyVM.setHistoryListLiveData(list)
    }

    private val clickListener = object: HistoryAdapter.ClickListener {
        override fun onClick(pos: Int, v: View) {
            d(TAG, "onClick($pos, v)")
            callViewModel.selectedCall = historyAdapter.currentList[pos]
            startActivity(Intent(context, CallDetailActivity::class.java))
        }
    }

    private val longClickListener = object: HistoryAdapter.LongClickListener {
        override fun onLongClick(pos: Int, v: View) {
            d(TAG, "onLongClick($pos, v)")
            val call = historyAdapter.currentList[pos]
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

    private val adapterObserver = object: AdapterDataObserver() {
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            d(TAG, "onItemRangeChanged($positionStart, $itemCount)")
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            d(TAG, "onItemRangeChanged($positionStart, $itemCount, payload)")
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            d(TAG, "onItemRangeInserted($positionStart, $itemCount)")
            isLoading = false
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            d(TAG, "onItemRangeRemoved($positionStart, $itemCount)")
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            d(TAG, "onItemRangeMoved($fromPosition, $toPosition, $itemCount)")
        }
    }

    companion object {
        private const val TAG = "HistoryFragment"
    }
}