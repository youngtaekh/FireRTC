package kr.young.firertc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.IS_GROUP
import kr.young.firertc.util.Config.Companion.LAST_MESSAGE
import kr.young.firertc.util.Config.Companion.LAST_SEQUENCE
import kr.young.firertc.util.Config.Companion.MODIFIED_AT
import kr.young.firertc.util.Config.Companion.PARTICIPANTS
import kr.young.firertc.util.Config.Companion.TITLE
import java.util.*

data class Chat(
    val participants: List<String> = listOf(),
    val title: String? = "",
    var id: String? = null,
    var isGroup: Boolean = false,
    var lastMessage: String = "",
    var lastSequence: Long = -1,
    val modifiedAt: Date? = null,
    val createdAt: Date? = null
) {
    init {
        if (participants.size >= 2) {
            id = this.id ?: Crypto().getHash("${participants[0]}${participants[1]}")
        }
    }

    fun toMap(): Map<String, Any> {
        val createdAt = this.createdAt ?: FieldValue.serverTimestamp()
        val modifiedAt = this.modifiedAt ?: FieldValue.serverTimestamp()
        return mapOf(
            "id" to id!!,
            TITLE to title!!,
            PARTICIPANTS to participants,
            IS_GROUP to isGroup,
            LAST_MESSAGE to lastMessage,
            LAST_SEQUENCE to lastSequence,
            MODIFIED_AT to modifiedAt,
            CREATED_AT to createdAt
        )
    }

    companion object {
        private const val TAG = "Chat"
    }
}
