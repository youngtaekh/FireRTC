package kr.young.firertc.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.CANDIDATES
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.DIRECTION
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.util.Config.Companion.SDP
import kr.young.firertc.util.Config.Companion.SPACE_ID
import kr.young.firertc.util.Config.Companion.TERMINATED
import kr.young.firertc.util.Config.Companion.USER_ID
import kr.young.firertc.vm.MyDataViewModel
import java.lang.System.currentTimeMillis
import java.util.*

@Entity(tableName = "calls")
data class Call(
    val userId: String = MyDataViewModel.instance.getMyId(),
    val fcmToken: String? = MyDataViewModel.instance.myData!!.fcmToken,
    val spaceId: String? = null,
    @PrimaryKey
    val id: String = Crypto().getHash("$userId$spaceId${currentTimeMillis()}"),
    var type: Type = Type.AUDIO,
    var direction: Direction? = null,
    var counterpartName: String? = null,
    var connected: Boolean = false,
    var terminated: Boolean = false,
    var sdp: String? = null,
    var candidates: MutableList<String> = mutableListOf(),
    val createdAt: Date? = null,
    var connectedAt: Date? = null,
    var terminatedAt: Date? = null,
    var isHeader: Boolean = false,
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id
        map["connected"] = connected
        map[TERMINATED] = terminated
        map[USER_ID] = userId
        map["type"] = type
        if (spaceId != null) map[SPACE_ID] = spaceId
        if (counterpartName != null) map["counterpartName"] = counterpartName!!
        if (fcmToken != null) map[FCM_TOKEN] = fcmToken
        if (direction != null) map[DIRECTION] = direction!!
        if (sdp != null) map[SDP] = sdp!!
        map[CANDIDATES] = candidates
        map[CREATED_AT] = createdAt ?: FieldValue.serverTimestamp()
        return map
    }

    override fun toString(): String {
        val s = if (sdp == null) 0
        else 1
        return "($USER_ID=${userId}\n" +
                "$SPACE_ID=${spaceId?.substring(0,5)}\n" +
                "direction=$direction\n" +
                "connected=$connected\nterminated=${terminated}\n" +
                "$CREATED_AT=$createdAt\nid=${id.substring(0,5)}\n" +
                "$SDP=$s\n$CANDIDATES=${candidates.size})"
    }

    override fun equals(other: Any?): Boolean {
        return if (other == null) {
            false
        } else {
            val o = other as Call
            this.userId == o.userId &&
            this.spaceId == o.spaceId &&
            this.id == o.id &&
            this.direction == o.direction &&
            this.sdp == o.sdp &&
            this.createdAt == o.createdAt
        }
    }

    override fun hashCode(): Int {
        var result = userId.hashCode() ?: 0
        result = 31 * result + (spaceId?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        result = 31 * result + (direction?.hashCode() ?: 0)
        result = 31 * result + (sdp?.hashCode() ?: 0)
        result = 31 * result + createdAt.hashCode()
        return result
    }

    companion object {
        private const val TAG = "Call"
    }

    enum class Direction {
        Offer,
        Answer,
    }

    enum class Type {
        AUDIO,
        VIDEO,
        SCREEN,
        MESSAGE,
        CONFERENCE
    }
}
