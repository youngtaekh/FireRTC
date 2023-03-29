package kr.young.examplewebrtc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.examplewebrtc.util.Config.Companion.CANDIDATES
import kr.young.examplewebrtc.util.Config.Companion.DIRECTION
import kr.young.examplewebrtc.util.Config.Companion.FCM_TOKEN
import kr.young.examplewebrtc.util.Config.Companion.SDP
import kr.young.examplewebrtc.util.Config.Companion.SPACE_ID
import kr.young.examplewebrtc.util.Config.Companion.TERMINATED
import kr.young.examplewebrtc.util.Config.Companion.USER_ID
import kr.young.examplewebrtc.vm.MyDataViewModel
import java.lang.System.currentTimeMillis
import java.util.*

data class Call(
    val userId: String = MyDataViewModel.instance.getMyId(),
    val fcmToken: String? = MyDataViewModel.instance.myData!!.fcmToken,
    val spaceId: String? = null,
    val id: String = Crypto().getHash("$userId$spaceId${currentTimeMillis()}"),
    var type: Type = Type.AUDIO,
    var direction: Direction? = null,
    var connected: Boolean = false,
    var terminated: Boolean = false,
    var sdp: String? = null,
    var candidates: MutableList<String> = mutableListOf(),
    val createdAt: Date? = null,
    var terminatedAt: Date? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id
        map["connected"] = connected
        map[TERMINATED] = terminated
        map[USER_ID] = userId
        if (spaceId != null) map[SPACE_ID] = spaceId
        if (fcmToken != null) map[FCM_TOKEN] = fcmToken
        if (direction != null) map[DIRECTION] = direction!!
        if (sdp != null) map[SDP] = sdp!!
        map[CANDIDATES] = candidates
        map["createdAt"] = createdAt ?: FieldValue.serverTimestamp()
        return map
    }

    override fun toString(): String {
        val s = if (sdp == null) 0
        else 1
        return "($USER_ID=${userId?.substring(0,5)}\n" +
                "$SPACE_ID=${spaceId?.substring(0,5)}\n" +
                "state=$direction\nterminated=${terminated}\n" +
                "createAt=$createdAt\nid=${id.substring(0,5)}\n" +
                "sdp=$s\ncandidates=${candidates.size})"
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
        var result = userId?.hashCode() ?: 0
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
        MESSAGE,
        CONFERENCE
    }
}
