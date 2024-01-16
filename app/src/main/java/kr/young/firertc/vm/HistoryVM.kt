package kr.young.firertc.vm

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kr.young.common.DateUtil
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.Call
import kr.young.firertc.repo.CallRepository
import kr.young.firertc.util.Config.Companion.HISTORY_PAGE_SIZE
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*
import java.util.*

class HistoryVM private constructor() {
    private val roomDB = AppRoomDatabase.getInstance()!!
    val historyList = MutableLiveData(listOf<Call>())
    var isEndReload = false

    fun setHistoryListLiveData(value: List<Call>) {
        Handler(Looper.getMainLooper()).post { historyList.value = value }
    }

    private fun addDateHistory(iterator: List<Call>, isAdditional: Boolean) {
        val list = iterator.toObservable()
            .buffer(2, 1)
            .concatMap { checkDateHistory(it.first(), it.last()).toObservable() }
            .toList().blockingGet() as MutableList

        list.firstOrNull()?.let {
            val existList = mutableListOf<Call>()
            existList.addAll(historyList.value!!)
            if (isAdditional) {
                if (checkDateHistory(existList.last(), list.first()).size == 2) {
                    list.add(0, Call(isHeader = true, createdAt = it.createdAt))
                }
                list.addAll(0, existList)
            } else {
                list.add(0, Call(isHeader = true, createdAt = it.createdAt))
                if (existList.isNotEmpty() && checkDateHistory(list.last(), existList.first()).size != 2) {
                    existList.removeFirst()
                }
                list.addAll(existList)
            }
            setHistoryListLiveData(list)
        }
    }

    private fun checkDateHistory(up: Call, down: Call): List<Call> {
        val upDate = DateUtil.toFormattedString(up.createdAt!!, "yy MM dd")
        val downDate = DateUtil.toFormattedString(down.createdAt!!, "yy MM dd")
        val list = mutableListOf(up)
        if (upDate != downDate) {
            list.add(Call(isHeader = true, createdAt = down.createdAt))
        }
        return list
    }

    private fun getHistoryFromDB(isAdditional: Boolean): List<Call> {
        return if (!historyList.value.isNullOrEmpty()) {
            if (isAdditional) {
                roomDB.callDao().getAdditionCalls(historyList.value!!.last().createdAt!!)
            } else {
                roomDB.callDao().getCalls(historyList.value!!.first().createdAt!!)
            }
        } else {
            roomDB.callDao().getCalls()
        }
    }

    @SuppressLint("CheckResult")
    fun getHistory(isAdditional: Boolean = false) {
        val list = Observable.just(0)
            .observeOn(Schedulers.io())
            .concatMap { getHistoryFromDB(isAdditional).toObservable() }
            .toList().blockingGet()

        addDateHistory(list, isAdditional)

        if (!isAdditional || list.size < HISTORY_PAGE_SIZE) {
            getHistoryFromServer(
                if (historyList.value!!.isEmpty() && list.isEmpty()) {
                    null
                } else if (isAdditional && list.isEmpty()) {
                    historyList.value!!.last().createdAt
                } else if (isAdditional) {
                    list.last().createdAt
                } else if (list.isNotEmpty()) {
                    list.first().createdAt
                } else {
                    historyList.value!!.first().createdAt
                }, isAdditional
            )
        }
    }

    @SuppressLint("CheckResult")
    fun getHistoryFromServer(date: Date? = null, isAdditional: Boolean = false) {
        CallRepository.getByUserId(
            MyDataViewModel.instance.getMyId(),
            date,
            isAdditional
        ) {
            val list = it.documents.toObservable()
                .map { doc -> doc.toObject<Call>()!! }
                .observeOn(Schedulers.io())
                .doOnNext { call -> roomDB.callDao().setCalls(call) }
                .toList().blockingGet()

            isEndReload = isAdditional && list.isEmpty()
            addDateHistory(list, isAdditional)
        }
    }

    companion object {
        private const val TAG = "HistoryVM"
        private var instance: HistoryVM? = null

        @OptIn(InternalCoroutinesApi::class)
        @Synchronized
        fun getInstance(): HistoryVM? {
            if (instance == null) {
                synchronized(HistoryVM::class) {
                    instance = HistoryVM()
                }
            }
            return instance
        }

        fun destroyInstance() {
            instance = null
        }
    }
}