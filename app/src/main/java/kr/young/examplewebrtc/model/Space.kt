package kr.young.examplewebrtc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.common.DateUtil
import kr.young.examplewebrtc.util.Config.Companion.NAME
import kr.young.examplewebrtc.util.Config.Companion.STATUS
import java.util.*

data class Space(
    val name: String = "",
    val id: String = Crypto().getHash("$name${System.currentTimeMillis()}"),
    val maximum: Int = 2,
    var status: SpaceStatus = SpaceStatus.INACTIVE,
    val calls: MutableList<String> = mutableListOf(),
    var connected: Boolean = false,
    var terminated: Boolean = false,
    val type: Type = Type.AUDIO,
    val createdBy: String = "",
    val createdAt: Date? = null,
    var terminatedAt: Date? = null
) {
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["id"] = id
        map[NAME] = name
        map["maximum"] = maximum
        map["connected"] = connected
        map["terminated"] = terminated
        map["calls"] = calls
        map["createdBy"] = createdBy
        map["createdAt"] = createdAt ?: FieldValue.serverTimestamp()
        return map
    }

    override fun toString(): String {
        return "$NAME=$name max=$maximum connected=$connected terminated=$terminated creator=$createdBy id=${id.substring(0,5)} calls=$calls)"
    }

    enum class SpaceStatus {
        ACTIVE,
        INACTIVE,
        TERMINATED
    }

    enum class Type {
        AUDIO,
        VIDEO,
        MESSAGE,
        CONFERENCE
    }

    companion object {
        private const val TAG = "Space"

        fun notTerminated(): List<String> {
            return listOf(
                SpaceStatus.ACTIVE.toString(),
                SpaceStatus.INACTIVE.toString()
            )
        }
    }
}
