package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.Crypto
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.HomeActivity
import kr.young.firertc.model.User
import kr.young.firertc.repo.AppSP
import kr.young.firertc.repo.UserRepository
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_FAILURE
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.ResponseCode.Companion.WRONG_PASSWORD

class MyDataViewModel {

    val isSigned = MutableLiveData<Boolean>()
    val responseCode = MutableLiveData<Int>()
    private val _myData = MutableLiveData<User?>(null)
    val myData: LiveData<User?> get() = _myData

    private fun setSigned(value: Boolean) {
        Handler(Looper.getMainLooper()).post { isSigned.value = value }
    }

    fun setResponseCode(value: Int) {
        Handler(Looper.getMainLooper()).post { responseCode.value = value }
    }

    private fun createUser(user: User) {
        UserRepository.post(user = user, success = {
            d(TAG, "createUser success")
            AppSP.instance.setUserId(user.id)
            AppSP.instance.setUserName(user.name)
            AppSP.instance.setSignIn(true)
            _myData.postValue(user)
            setSigned(true)
        })
    }

    fun updateFCMToken(token: String) {
        myData.value?.let {
            it.fcmToken = token
            UserRepository.updateFCMToken(it)
        }
    }

    fun getMyData() {
        UserRepository.getUser(AppSP.instance.getUserId()!!) {
            val user = it.toObject<User>()!!
            _myData.postValue(user)
            AppSP.instance.setUserId(user.id)
            AppSP.instance.setUserName(user.name)
        }
    }

    fun checkMyData(id: String, pwd: String) {
        val lowerId = id.lowercase()
        val cryptoPassword = Crypto.getHash(pwd)
        d(TAG, "lower $lowerId")
        d(TAG, "crypto $cryptoPassword")
        UserRepository.getUser(lowerId, {
            e(TAG, "checkMyData fail", it)
            setResponseCode(USER_READ_FAILURE)
        }, { document ->
            d(TAG, "get user success")
            setResponseCode(USER_READ_SUCCESS)
            if (document.data == null) {
                createUser(User(id = lowerId, name = id, password = cryptoPassword, fcmToken = AppSP.instance.getFCMToken()))
            } else {
                val user = document.toObject<User>()
                if (cryptoPassword == user?.password) {
                    _myData.postValue(user)
                    AppSP.instance.setUserId(user.id)
                    AppSP.instance.setUserName(user.name)
                    AppSP.instance.setUserPwd(pwd)
                    AppSP.instance.setSignIn(true)
                    setSigned(true)
                    updateFCMToken(AppSP.instance.getFCMToken()!!)
                } else {
                    setResponseCode(WRONG_PASSWORD)
                }
            }
        })
    }

    fun signOut() {
        _myData.postValue(null)
        AppSP.instance.setSignIn(false)
        d(TAG, "setFragmentIndex 0")
        AppSP.instance.setFragmentIndex(0)
        setSigned(false)
    }

    fun getMyId(): String {
        d(TAG, "getMyId ${myData.value == null}")
        return if (myData.value == null) {
            AppSP.instance.getUserId()!!
        } else {
            myData.value!!.id
        }
    }

    fun getMyFcmToken(): String? {
        return if (myData.value == null) {
            AppSP.instance.getFCMToken()
        } else {
            myData.value?.id
        }
    }

    init {
        d(TAG, "init")
        responseCode.value = 0
        if (AppSP.instance.isSigned()) {
            _myData.postValue(User(
                id = AppSP.instance.getUserId()!!,
                name = AppSP.instance.getUserName()!!,
                fcmToken = AppSP.instance.getFCMToken()
            ))
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