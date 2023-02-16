package kr.young.examplewebrtc.model

import kr.young.common.Crypto
import kr.young.common.DateUtil
import java.lang.System.currentTimeMillis

data class Call(
    val userId: String? = null,
    val spaceId: String? = null,
    val id: String = Crypto().getHash("$userId${currentTimeMillis()}"),
    var direction: CallDirection? = null,
    var sdp: String? = null,
    val createdAt: String = DateUtil.toFormattedString(currentTimeMillis()),
    var terminatedAt: String? = null
) {
    override fun toString(): String {
        return "userId=$userId\nspaceId=$spaceId\nstate=$direction\ncreateDate=$createdAt\nid=$id\nsdp=$sdp"
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
        const val COLLECTION = "calls"
        const val DIRECTION = "direction"
        const val SPACE_ID = "spaceId"
        const val TERMINATED_AT = "terminatedAt"
    }

    enum class CallDirection {
        Offer,
        Answer,
    }
}
