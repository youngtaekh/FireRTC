package kr.young.examplewebrtc.repo

import android.content.Context
import android.content.SharedPreferences
import kr.young.common.ApplicationUtil

class AppSP private constructor() {

    fun setSignIn(sign: Boolean) { setBoolean(SIGN_IN, sign) }

    fun isSignIn() = getBoolean(SIGN_IN)

    fun setUserId(userId: String) { setString(USER_ID, userId) }
    fun setUserPwd(userPwd: String) { setString(USER_PWD, userPwd) }
    fun setFCMToken(token: String) { setString(FCM_TOKEN, token) }

    fun getUserId() = getString(USER_ID)
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

        private const val SIGN_IN = "signIn"

        private const val USER_ID = "userId"
        private const val USER_PWD = "userPassword"
        private const val FCM_TOKEN = "fcmToken"
    }
}