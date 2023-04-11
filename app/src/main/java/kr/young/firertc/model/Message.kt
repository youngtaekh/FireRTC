package kr.young.firertc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.FROM
import java.util.*
import kotlin.collections.Map

data class Message(
    val from: String,
    val chatId: String,
    val id: String = Crypto().getHash("$from$chatId${System.currentTimeMillis()}"),
    val body: String,
    var timeFlag: Boolean = true,
    val createdAt: Date?
) {
    fun toMap(): Map<String, Any> {
        val createdAt = this.createdAt ?: FieldValue.serverTimestamp()
        return mapOf(
            "id" to id,
            FROM to from,
            "chatId" to chatId,
            "body" to body,
            CREATED_AT to createdAt
        )
    }

    companion object {
        private const val TAG = "Message"
    }
}