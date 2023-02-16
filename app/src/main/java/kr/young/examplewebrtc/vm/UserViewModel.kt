package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kr.young.examplewebrtc.model.User

class UserViewModel: ViewModel() {
    val users = MutableLiveData<MutableList<User>>()

    fun add(user: User) {
        users.value!!.add(user)
    }

    private object Holder {
        val INSTANCE = UserViewModel()
    }

    companion object {
        val instance: UserViewModel by lazy { Holder.INSTANCE }
    }
}