package kr.young.firertc.vm

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized
import kr.young.common.DateUtil
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.Call
import kr.young.firertc.repo.CallRepository
import kr.young.firertc.util.RecyclerViewNotifier
import kr.young.firertc.util.RecyclerViewNotifier.ModifierCategory.*

class HistoryVM private constructor() {
    val roomDB = AppRoomDatabase.getInstance()!!
    val list = MutableLiveData(listOf<Call>())
    val notifier = MutableLiveData<RecyclerViewNotifier<Call>>()
    var isEndReload = false

    fun setListLiveData(value: List<Call>) {
        Handler(Looper.getMainLooper()).post { list.value = value }
    }

//    fun addDateHistory(iterator: List<Call>, isAdditional: Boolean) {
//        val list = iterator.toObservable()
//            .buffer(2, 1)
//            .concatMap {  }
//    }
//
//    fun checkDateHistory(up: Call, down: Call): List<Call> {
//        val upDate = DateUtil.toFormattedString(up.createdAt!!, "yy MM dd")
//        val downDate = DateUtil.toFormattedString(down.createdAt!!, "yy MM dd")
//    }

    @SuppressLint("CheckResult")
    fun getHistory(isAdditional: Boolean = false) {
        if (isAdditional) {
            d("HistoryVM", "reload additional history")
        }
        MyDataViewModel.instance.myData?.let {
            val size = list.value!!.size
            val subList = mutableListOf<Call>()
            val ob = Observable.just(it)
                .observeOn(Schedulers.io())
                .flatMap {
                    if (isAdditional && list.value!!.isNotEmpty() && list.value!!.last().createdAt != null) {
                        roomDB.callDao().getAdditionCalls(list.value!!.last().createdAt!!).toObservable()
                    } else if (!isAdditional && list.value!!.isNotEmpty() && list.value!!.first().createdAt != null) {
                        roomDB.callDao().getCalls(list.value!!.first().createdAt!!).toObservable()
                    } else {
                        roomDB.callDao().getCalls().toObservable()
                    }
                }
                .observeOn(Schedulers.computation())
                .map { call ->
                    if (subList.isEmpty() || checkDay(subList.last(), call)) {
                        subList.add(Call(isHeader = true, createdAt = call.createdAt))
                    }
                    subList.add(call)
                }
                .observeOn(AndroidSchedulers.mainThread())
            ob.subscribeBy(
                onComplete = {
                    if (subList.isNotEmpty()) {
                        val existList = mutableListOf<Call>()
                        existList.addAll(list.value!!)
                        if (isAdditional) {
                            if (!checkDay(list.value!!.last(), subList.first())) {
                                subList.removeAt(0)
                            }
                            existList.addAll(size, subList)
                        } else {
                            if (list.value!!.isNotEmpty() && !checkDay(list.value!!.first(), subList.last())) {
                                existList.removeAt(0)
                            }
                            existList.addAll(0, subList)
                        }
                        setListLiveData(existList)
                    }
                    getHistoryFromServer(isAdditional)
                }
            )
        }
    }

    @SuppressLint("CheckResult")
    fun getHistoryFromServer(isAdditional: Boolean = false) {
        CallRepository.getByUserId(
            MyDataViewModel.instance.getMyId(),
            if (list.value!!.isEmpty()) null else if (isAdditional) { list.value!!.last().createdAt } else { list.value!!.first().createdAt },
            isAdditional
        ) {
            val size = list.value!!.size
            val subList = mutableListOf<Call>()
            val ob = it.toObservable()
                .observeOn(Schedulers.io())
                .map { snapshot ->
                    val call = snapshot.toObject<Call>()
                    roomDB.callDao().setCalls(call)
                    if (subList.isEmpty() || checkDay(subList.last(), call)) {
                        subList.add(Call(isHeader = true, createdAt = call.createdAt))
                    }
                    subList.add(call)
                }.observeOn(AndroidSchedulers.mainThread())
            ob.subscribeBy(
                onComplete = {
                    if (subList.isNotEmpty()) {
                        val existList = mutableListOf<Call>()
                        existList.addAll(list.value!!)
                        if (isAdditional) {
                            if (!checkDay(list.value!!.last(), subList.first())) {
                                subList.removeAt(0)
                            }
                            existList.addAll(size, subList)
                        } else {
                            if (list.value!!.isNotEmpty() && !checkDay(list.value!!.first(), subList.last())) {
                                existList.removeAt(0)
                            }
                            existList.addAll(0, subList)
                        }
                        setListLiveData(existList)
                    } else if (isAdditional) {
                        d("HistoryVM", "end reload")
                        isEndReload = true
                    }
                }
            )
        }
    }

    private fun checkDay(last: Call, current: Call): Boolean {
        return last.createdAt != null && current.createdAt != null &&
                DateUtil.toFormattedString(last.createdAt, "yyMMdd") !=
                DateUtil.toFormattedString(current.createdAt, "yyMMdd")
    }

    companion object {
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