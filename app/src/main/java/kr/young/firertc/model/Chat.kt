package kr.young.firertc.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.FieldValue
import kr.young.common.ApplicationUtil
import kr.young.common.Crypto
import kr.young.firertc.R
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.IS_GROUP
import kr.young.firertc.util.Config.Companion.LAST_MESSAGE
import kr.young.firertc.util.Config.Companion.LAST_SEQUENCE
import kr.young.firertc.util.Config.Companion.MODIFIED_AT
import kr.young.firertc.util.Config.Companion.PARTICIPANTS
import kr.young.firertc.util.Config.Companion.TITLE
import java.util.*

@Entity (tableName = "chats")
data class Chat(
    val participants: List<String> = listOf(),
    val title: String? = "",
    var localTitle: String = "",
    @PrimaryKey
    var id: String = if (participants.isEmpty()) "" else Crypto().getHash("${participants[0]}${participants[1]}"),
    var isGroup: Boolean = false,
    var lastMessage: String = "",
    var lastSequence: Long = -1,
    var modifiedAt: Date? = null,
    val createdAt: Date? = null
) {
    fun toMap(): Map<String, Any> {
        val createdAt = this.createdAt ?: FieldValue.serverTimestamp()
        val modifiedAt = this.modifiedAt ?: FieldValue.serverTimestamp()
        return mapOf(
            "id" to id,
            TITLE to title!!,
            PARTICIPANTS to participants,
            IS_GROUP to isGroup,
            LAST_MESSAGE to lastMessage,
            LAST_SEQUENCE to lastSequence,
            MODIFIED_AT to modifiedAt,
            CREATED_AT to createdAt
        )
    }

    fun setLocalTitle(title: String?): Chat {
        localTitle = title ?: ApplicationUtil.getContext()!!.getString(R.string.no_one)
        return this
    }

    fun removeParticipants(vararg participants: String): Chat {
        participants.map { (this.participants as MutableList).remove(it) }
        return this
    }

    companion object {
        private const val TAG = "Chat"
    }
}
