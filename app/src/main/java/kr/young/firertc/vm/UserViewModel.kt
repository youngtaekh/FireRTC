package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.firertc.R
import kr.young.firertc.model.Relation
import kr.young.firertc.model.User
import kr.young.firertc.repo.RelationRepository
import kr.young.firertc.repo.UserRepository
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.ResponseCode.Companion.NO_USER
import kotlin.random.Random

class UserViewModel: ViewModel() {
    val participants = mutableListOf<User>()
    val refreshContacts = MutableLiveData<Boolean>()
    val contacts = mutableListOf<User>()
    val foundUser = MutableLiveData<User?>()
    var selectedProfile: User? = null
    var sourcePage = 0
    var destinationPage = 0

    internal val responseCode = MutableLiveData<Int> ()

    fun setResponseCode(value: Int) {
        d(TAG, "setResponseCode $value")
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun setRefreshContacts(value: Boolean = true) {
        d(TAG, "setRefreshContacts")
        Handler(Looper.getMainLooper()).post { refreshContacts.value = value }
    }

    fun setFoundUser(user: User?) {
        if (user != null) {
            Handler(Looper.getMainLooper()).post { foundUser.value = user }
        }
    }

    fun setContacts(users: List<User>) {
        d(TAG, "setContacts size ${users.size}")
        removeAllContact()
        contacts.addAll(users)
        setRefreshContacts()
    }

    fun addContact(user: User) {
        d(TAG, "addContact id ${user.id}")
        contacts.add(user)
    }

    fun addContacts(list: List<User>) {
        d(TAG, "addContacts size ${list.size}")
        contacts.addAll(list)
    }

    fun removeAllContact() {
        d(TAG, "removeAllContact")
        contacts.removeAll {
            d(TAG, "remove contacts")
            true
        }
    }

    fun readUsers(list: List<String>) {
        d(TAG, "readUsers(${list.size})")
        UserRepository.getUsers(
            list = list,
            success = {
                d(TAG, "readUsers Success")
                participants.removeAll { true }
                for (document in it) {
                    val user = document.toObject<User>()
                    participants.add(user)
                }
                setResponseCode(USER_READ_SUCCESS)
            })
    }

    fun initPage(destination: Int) {
        sourcePage = 0
        destinationPage = destination + 1
    }

    fun checkPage(): Boolean {
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

    fun createRelation(userId: String) {
        d(TAG, "createRelation")
        val relation = Relation(
            from = MyDataViewModel.instance.getMyId(),
            to = userId
        )
        RelationRepository.post(relation)
    }

    fun readAllRelation() {
        d(TAG, "readAllRelation")
        if (MyDataViewModel.instance.myData != null) {
            RelationRepository.getAll()
        }
    }

    fun deleteRelation(to: String) {
        d(TAG, "deleteRelation")
        RelationRepository.remove(to)
    }

    fun selectImage(id: String?): Int {
        return when (Random.nextInt(7)) {
            0 -> R.drawable.outline_sentiment_very_satisfied_24
            1 -> R.drawable.outline_mood_24
            2 -> R.drawable.outline_sentiment_satisfied_24
            3 -> R.drawable.outline_sentiment_neutral_24
            4 -> R.drawable.outline_sentiment_dissatisfied_24
            5 -> R.drawable.outline_mood_bad_24
            else -> R.drawable.outline_sentiment_very_dissatisfied_24
        }
    }

    init {
        setRefreshContacts(false)
        responseCode.value = 0
    }

    private object Holder {
        val INSTANCE = UserViewModel()
    }

    companion object {
        private const val TAG = "UserViewModel"
        val instance: UserViewModel by lazy { Holder.INSTANCE }
    }
}