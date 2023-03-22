package kr.young.examplewebrtc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.Crypto
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.examplewebrtc.model.User
import kr.young.examplewebrtc.repo.AppSP
import kr.young.examplewebrtc.repo.UserRepository
import kr.young.examplewebrtc.repo.UserRepository.Companion.USER_READ_FAILURE
import kr.young.examplewebrtc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.examplewebrtc.util.ResponseCode.Companion.FIRESTORE_FAILURE
import kr.young.examplewebrtc.util.ResponseCode.Companion.WRONG_PASSWORD

class MyDataViewModel {

    val isSigned = MutableLiveData<Boolean>()
    val responseCode = MutableLiveData<Int>()
    var myData: User? = null

    private fun setSigned(value: Boolean) {
        Handler(Looper.getMainLooper()).post { isSigned.value = value }
    }

    fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    fun createUser(user: User) {
        UserRepository.post(user = user, success = {
            d(TAG, "createUser success")
            AppSP.instance.setUserId(user.id)
            AppSP.instance.setUserName(user.name)
            AppSP.instance.setSignIn(true)
            myData = user
            setSigned(true)
        })
    }

    fun checkMyData(id: String, pwd: String) {
        val lowerId = id.lowercase()
        val cryptoPassword = Crypto().getHash(pwd)
        d(TAG, "lower $lowerId")
        d(TAG, "crypto $cryptoPassword")
        UserRepository.getUser(lowerId, { document ->
            d(TAG, "get user success")
            setResponseCode(USER_READ_SUCCESS)
            if (document.data == null) {
                createUser(User(id = lowerId, name = id, password = cryptoPassword, fcmToken = AppSP.instance.getFCMToken()))
            } else {
                val user = document.toObject<User>()
                if (cryptoPassword == user?.password) {
                    myData = user
                    AppSP.instance.setUserId(user.id)
                    AppSP.instance.setUserName(user.name)
                    AppSP.instance.setSignIn(true)
                    setSigned(true)
                } else {
                    setResponseCode(WRONG_PASSWORD)
                }
            }
        }, { e ->
            e(TAG, "checkMyData fail", e)
            setResponseCode(USER_READ_FAILURE)
        })
    }

    fun signOut() {
        myData = null
        AppSP.instance.setSignIn(false)
        setSigned(false)
    }

    fun getMyId(): String {
        return myData!!.id
    }

    init {
        responseCode.value = 0
        if (AppSP.instance.isSigned()) {
            myData = User(
                id = AppSP.instance.getUserId()!!,
                name = AppSP.instance.getUserName()!!,
                fcmToken = AppSP.instance.getFCMToken()
            )
            isSigned.value = true
        } else {
            isSigned.value = false
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