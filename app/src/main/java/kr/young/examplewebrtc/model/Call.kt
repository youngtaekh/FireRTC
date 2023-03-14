package kr.young.examplewebrtc.model

import kr.young.common.Crypto
import kr.young.common.DateUtil
import kr.young.examplewebrtc.util.Config.Companion.SPACE_ID
import kr.young.examplewebrtc.util.Config.Companion.USER_ID
import java.lang.System.currentTimeMillis

data class Call(
    val userId: String? = null,
    val token: String? = null,
    val spaceId: String? = null,
    val id: String = Crypto().getHash("$userId${currentTimeMillis()}"),
    var direction: CallDirection? = null,
    var connected: Boolean = false,
    var terminated: Boolean = false,
    var sdp: String? = null,
    var candidates: MutableList<String> = mutableListOf(),
    val createdAt: String = DateUtil.toFormattedString(currentTimeMillis()),
    var terminatedAt: String? = null
) {
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

    enum class CallDirection {
        Offer,
        Answer,
    }
}
