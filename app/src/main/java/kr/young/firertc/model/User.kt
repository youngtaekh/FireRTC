package kr.young.firertc.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.FieldValue
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.util.Config.Companion.NAME
import kr.young.firertc.util.Config.Companion.OS
import java.util.*
import kotlin.math.min

@Entity(tableName = "users")
data class User (
    @PrimaryKey
    var id: String = "",
    var password: String = "",
    var name: String = id,
    var os: String? = null,
    var fcmToken: String? = null,
    val createdAt: Date? = null,
) {
    fun toMap(): Map<String, Any> {
        val token = fcmToken ?: ""
        val at = createdAt ?: FieldValue.serverTimestamp()
        val os = this.os ?: "Android"
        return mapOf(
            "id" to id,
            "password" to password,
            NAME to name,
            OS to os,
            FCM_TOKEN to token,
            "createdAt" to at
        )
    }

    override fun toString(): String {
        return "id-$id, $NAME-$name, createdAt-$createdAt, " +
                "pwd-${password.substring(0, min(5, password.length))}, " +
                "fcmToken-${fcmToken?.substring(0, min(5, fcmToken?.length ?: 0))}"
    }

    companion object {
        private const val TAG = "User"
    }
}
