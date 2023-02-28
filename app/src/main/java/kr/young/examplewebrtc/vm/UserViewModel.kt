package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.UserRepository

class UserViewModel: ViewModel() {
    private val getUsersSuccess = OnSuccessListener<QuerySnapshot> { documents ->
        for (document in documents) {
            val user = document.toObject<User>()
            add(user)
        }
    }
    val users = MutableLiveData<MutableList<User>>()

    private fun add(user: User) {
        if (users.value == null) {
            users.value = mutableListOf()
        }
        users.value!!.add(user)
    }

    fun get(id: String) {
        UserRepository.get(id) { document ->
            val user = document.toObject<User>()

        }
    }

    private object Holder {
        val INSTANCE = UserViewModel()
    }

    companion object {
        private const val TAG = "UserViewModel"
        val instance: UserViewModel by lazy { Holder.INSTANCE }
    }
}