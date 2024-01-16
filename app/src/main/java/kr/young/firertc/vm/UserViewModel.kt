package kr.young.firertc.vm

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.R
import kr.young.firertc.db.AppRoomDatabase
import kr.young.firertc.model.Relation
import kr.young.firertc.model.User
import kr.young.firertc.repo.RelationRepository
import kr.young.firertc.repo.UserRepository
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.ResponseCode.Companion.NO_USER

class UserViewModel: ViewModel() {
    val participants = mutableListOf<User>()
    var contacts = MutableLiveData(mutableListOf<User>())
    val foundUser = MutableLiveData<User?>()
    var selectedProfile: User? = null
    var sourcePage = 0
    var destinationPage = 0
    private var userList = mutableListOf<User>()

    internal val responseCode = MutableLiveData<Int> ()

    fun setResponseCode(value: Int) {
        d(TAG, "setResponseCode $value")
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun setContactsLiveData(value: MutableList<User>) {
        Handler(Looper.getMainLooper()).post { contacts.value = value }
    }

    fun setFoundUser(user: User?) {
        if (user != null) {
            Handler(Looper.getMainLooper()).post { foundUser.value = user }
        }
    }

    @SuppressLint("CheckResult")
    fun getContacts() {
        val list = Observable.just(AppRoomDatabase.getInstance()!!)
            .observeOn(Schedulers.io())
            .flatMap { it.userDao().getUsers().toObservable() }
            .toList()
            .blockingGet()
        addContacts(list)
        getRelations()
    }

    private fun addContacts(list: List<User>) {
        d(TAG, "addContacts(${list.size})")
        setContactsLiveData(list.sortedBy { it.name } as MutableList<User>)
    }

    fun readUsers(list: List<String>) {
        d(TAG, "readUsers(${list.size})")
        UserRepository.getUsers(list) {
            d(TAG, "readUsers Success")
            participants.removeAll { true }
            for (document in it) {
                val user = document.toObject<User>()
                participants.add(user)
            }
            setResponseCode(USER_READ_SUCCESS)
        }
    }

    fun initPage(destination: Int) {
        sourcePage = 0
        destinationPage = destination + 1
    }

    private fun checkPage(): Boolean {
        sourcePage += 1
        return sourcePage == destinationPage
    }

    fun readUser(userId: String, success: OnSuccessListener<DocumentSnapshot> = OnSuccessListener {
        if (it.data == null) {
            setResponseCode(NO_USER)
        } else {
            setFoundUser(it.toObject<User>())
        }
    }) {
        d(TAG, "readUser($userId)")
        UserRepository.getUser(id = userId, success = success)
    }

    fun selectImage(id: String?): Int {
        var key = 0
        for (i in id!!.indices) {
            key += id[i].code
        }
        return when (key % 7) {
            0 -> R.drawable.outline_sentiment_very_satisfied_24
            1 -> R.drawable.outline_mood_24
            2 -> R.drawable.outline_sentiment_satisfied_24
            3 -> R.drawable.outline_sentiment_neutral_24
            4 -> R.drawable.outline_sentiment_dissatisfied_24
            5 -> R.drawable.outline_mood_bad_24
            else -> R.drawable.outline_sentiment_very_dissatisfied_24
        }
    }

    fun createRelation(userId: String) {
        d(TAG, "createRelation($userId)")
        val relation = Relation(
            from = MyDataViewModel.instance.getMyId(),
            to = userId
        )
        RelationRepository.post(relation)
    }

    fun deleteRelation(to: String) {
        d(TAG, "deleteRelation($to)")
        RelationRepository.remove(to)
    }

    fun getRelations() {
        d(TAG, "getRelations")
        if (MyDataViewModel.instance.myData == null) return

        RelationRepository.getAll {
            setResponseCode(RelationRepository.RELATION_READ_SUCCESS)
            userList = mutableListOf()
            val list = mutableListOf<String>()
            for (document in it) {
                val relation = document.toObject<Relation>()
                list.add(relation.to!!)
            }
            if (list.isEmpty()) {
                setContactsLiveData(mutableListOf())
            } else {
                UserRepository.getUsers(list = list, success = getUsersListener)
            }
        }
    }

    init {
        responseCode.value = 0
    }

    @SuppressLint("CheckResult")
    val getUsersListener = OnSuccessListener<QuerySnapshot> {
        Observable.fromIterable(it)
            .observeOn(Schedulers.io())
            .map { doc ->
                val user = doc.toObject<User>()
                AppRoomDatabase.getInstance()!!.userDao().setUsers(user)
                userList.add(user)
            }
            .doOnComplete { if (checkPage()) addContacts(userList) }
            .subscribe()
    }

    private object Holder {
        val INSTANCE = UserViewModel()
    }

    companion object {
        private const val TAG = "UserViewModel"
        val instance: UserViewModel by lazy { Holder.INSTANCE }
    }
}