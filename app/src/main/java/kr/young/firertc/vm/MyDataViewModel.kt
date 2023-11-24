package kr.young.firertc.vm

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ktx.toObject
import kr.young.common.Crypto
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.firertc.model.User
import kr.young.firertc.repo.AppSP
import kr.young.firertc.repo.UserRepository
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_FAILURE
import kr.young.firertc.repo.UserRepository.Companion.USER_READ_SUCCESS
import kr.young.firertc.util.ResponseCode.Companion.WRONG_PASSWORD

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

    fun updateFCMToken(token: String) {
        if (myData != null) {
            myData!!.fcmToken = token
            UserRepository.updateFCMToken(myData!!)
        }
    }

    fun checkMyData(id: String, pwd: String) {
        val lowerId = id.lowercase()
        val cryptoPassword = Crypto().getHash(pwd)
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
                    myData = user
                    AppSP.instance.setUserId(user.id)
                    AppSP.instance.setUserName(user.name)
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
//        myData = null
//        AppSP.instance.setSignIn(false)
//        setSigned(false)
//        StatizViewModel.getSchedule()
        StatizViewModel.getPlayerInfo()
//        GameRepository.getGames(endDate = DateUtil.getDate(2023, 9, 1), startDate = DateUtil.getDate(2023, 6, 29))
//        GameRepository.getGames(endDate = DateUtil.getDate(2023, 9, 1))
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