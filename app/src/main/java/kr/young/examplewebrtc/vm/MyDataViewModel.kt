package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import kr.young.common.Crypto
import kr.young.common.UtilLog.Companion.d
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.AppSP
import kr.young.examplewebrtc.repo.UserRepository

class MyDataViewModel {

    val myData = MutableLiveData<User>()

    fun setMyData(user: User) {
        myData.value = user
    }

    fun createUser(user: User) {
        UserRepository.post(user) {
            d(TAG, "createUser success")
            AppSP.instance.setUserId(user.id)
            AppSP.instance.setSignIn(true)
            setMyData(user)
        }
    }

    fun getMyId(): String {
        return myData.value!!.id
    }

    init {
        if (AppSP.instance.isSignIn()) {
            myData.value = User(id = AppSP.instance.getUserId()!!, fcmToken = AppSP.instance.getFCMToken())
        }
    }

    private object Holder {
        val INSTANCE = MyDataViewModel()
    }

    companion object {
        private const val TAG = "MyDataViewModel"
        val instance: MyDataViewModel by lazy { Holder.INSTANCE }
    }
}