package kr.young.firertc.vm

import android.annotation.SuppressLint
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
    val list = mutableListOf<Call>()
    val notifier = MutableLiveData<RecyclerViewNotifier<Call>>()
    var isEndReload = false

    @SuppressLint("CheckResult")
    fun getHistory(isAdditional: Boolean = false) {
        if (isAdditional) {
            d("HistoryVM", "reload additional history")
        }
        MyDataViewModel.instance.myData?.let {
            val size = list.size
            val subList = mutableListOf<Call>()
            val ob = Observable.just(it)
                .observeOn(Schedulers.io())
                .flatMap {
                    if (isAdditional && list.isNotEmpty() && list.last().createdAt != null) {
                        AppRoomDatabase.getInstance()!!.callDao().getAdditionCalls(list.last().createdAt!!).toObservable()
                    } else if (!isAdditional && list.isNotEmpty() && list.first().createdAt != null) {
                        AppRoomDatabase.getInstance()!!.callDao().getCalls(list.first().createdAt!!).toObservable()
                    } else {
                        AppRoomDatabase.getInstance()!!.callDao().getCalls().toObservable()
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
                        if (isAdditional) {
                            if (!checkDay(list.last(), subList.first())) {
                                subList.removeAt(0)
                            }
                            list.addAll(size, subList)
                            notifier.value = RecyclerViewNotifier(size, subList.size, Insert)
                        } else {
                            if (list.isNotEmpty() && !checkDay(list.first(), subList.last())) {
                                list.removeAt(0)
                                notifier.value = RecyclerViewNotifier(0, 1, Removed)
                            }
                            list.addAll(0, subList)
                            notifier.value = RecyclerViewNotifier(0, subList.size, Insert, true)
                        }
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
            if (list.isEmpty()) null else if (isAdditional) { list.last().createdAt } else { list.first().createdAt },
            isAdditional
        ) {
            val size = list.size
            val subList = mutableListOf<Call>()
            val ob = it.toObservable()
                .observeOn(Schedulers.io())
                .map { snapshot ->
                    val call = snapshot.toObject<Call>()
                    AppRoomDatabase.getInstance()!!.callDao().setCall(call)
                    if (subList.isEmpty() || checkDay(subList.last(), call)) {
                        subList.add(Call(isHeader = true, createdAt = call.createdAt))
                    }
                    subList.add(call)
                }.observeOn(AndroidSchedulers.mainThread())
            ob.subscribeBy(
                onComplete = {
                    if (subList.isNotEmpty()) {
                        if (isAdditional) {
                            if (!checkDay(list.last(), subList.first())) {
                                subList.removeAt(0)
                            }
                            list.addAll(size, subList)
                            notifier.value = RecyclerViewNotifier(size, subList.size, Insert)
                        } else {
                            if (list.isNotEmpty() && !checkDay(list.first(), subList.last())) {
                                list.removeAt(0)
                                notifier.value = RecyclerViewNotifier(0, 1, Removed)
                            }
                            list.addAll(0, subList)
                            notifier.value = RecyclerViewNotifier(0, subList.size, Insert, true)
                        }
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