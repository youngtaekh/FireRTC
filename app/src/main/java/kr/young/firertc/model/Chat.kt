package kr.young.firertc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.CREATED_AT
import java.util.*

data class Chat(
    val participants: List<String>,
    val title: String = "",
    val id: String = Crypto().getHash("${participants[0]}${participants[1]}"),
    val modifiedAt: Date? = null,
    val createdAt: Date? = null
) {
    fun toMap(): Map<String, Any> {
        val createdAt = this.createdAt ?: FieldValue.serverTimestamp()
        return mapOf(
            "id" to id,
            "participants" to participants,
            CREATED_AT to createdAt
        )
    }

    companion object {
        private const val TAG = "Chat"
    }
}
