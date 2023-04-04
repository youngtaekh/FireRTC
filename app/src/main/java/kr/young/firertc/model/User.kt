package kr.young.firertc.model

import com.google.firebase.firestore.FieldValue
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.util.Config.Companion.NAME
import java.util.*

data class User (
    var id: String = "",
    var password: String = "",
    var name: String = id,
    var fcmToken: String? = null,
    val createdAt: Date? = null,
) {
    fun toMap(): Map<String, Any> {
        val token = fcmToken ?: ""
        val at = createdAt ?: FieldValue.serverTimestamp()
        return mapOf(
            "id" to id,
            "password" to password,
            NAME to name,
            FCM_TOKEN to token,
            "createdAt" to at
        )
    }

    override fun toString(): String {
        return "id $id, pwd $password, $NAME $name, createdAt $createdAt\nfcmToken $fcmToken"
    }

    companion object {
        private const val TAG = "User"
    }
}
