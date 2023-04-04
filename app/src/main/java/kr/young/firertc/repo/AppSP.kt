package kr.young.firertc.repo

import android.content.Context
import android.content.SharedPreferences
import kr.young.common.ApplicationUtil

class AppSP private constructor() {

    fun setSignIn(sign: Boolean) { setBoolean(SIGNED, sign) }

    fun isSigned() = getBoolean(SIGNED)

    fun setUserId(userId: String) { setString(USER_ID, userId) }
    fun setUserName(userName: String) { setString(USER_NAME, userName) }
    fun setUserPwd(userPwd: String) { setString(USER_PWD, userPwd) }
    fun setFCMToken(token: String) { setString(FCM_TOKEN, token) }

    fun getUserId() = getString(USER_ID)
    fun getUserName() = getString(USER_NAME)
    fun getUserPwd() = getString(USER_PWD)
    fun getFCMToken() = getString(FCM_TOKEN)

    private fun setBoolean(key: String, value: Boolean) {
        val editor = mPreferences.edit()
        editor.putBoolean(key, value)
        editor.apply()
    }

    private fun getBoolean(key: String): Boolean {
        return mPreferences.getBoolean(key, false)
    }

    private fun setString(key: String, value: String) {
        val editor = mPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun getString(key: String): String? {
        return mPreferences.getString(key, null)
    }

    init {
        mPreferences = ApplicationUtil.getContext()!!.getSharedPreferences(
            NAME,
            Context.MODE_PRIVATE
        )
    }

    private object Holder {
        val INSTANCE = AppSP()
    }
    companion object {
        private const val TAG = "AppSP"
        private const val NAME = "appDB"
        val instance: AppSP by lazy { Holder.INSTANCE }
        private lateinit var mPreferences: SharedPreferences

        private const val SIGNED = "signed"

        private const val USER_ID = "userId"
        private const val USER_NAME = "userName"
        private const val USER_PWD = "userPassword"
        private const val FCM_TOKEN = "fcmToken"
    }
}