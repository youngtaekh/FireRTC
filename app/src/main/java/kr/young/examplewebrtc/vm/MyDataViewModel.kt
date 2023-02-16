package kr.young.examplewebrtc.vm

import androidx.lifecycle.MutableLiveData
import kr.young.examplewebrtc.model.User

class MyDataViewModel {
    val myData = MutableLiveData<User>()

    fun setMyData(user: User) {
        myData.value = user
    }

    private object Holder {
        val INSTANCE = MyDataViewModel()
    }

    companion object {
        val instance: MyDataViewModel by lazy { Holder.INSTANCE }
    }
}