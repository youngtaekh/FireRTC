package kr.young.firertc.model

import com.google.firebase.firestore.FieldValue
import kr.young.common.Crypto
import kr.young.firertc.util.Config.Companion.CREATED_AT
import kr.young.firertc.util.Config.Companion.NAME
import kr.young.firertc.util.Config.Companion.PARTICIPANTS
import kr.young.firertc.vm.MyDataViewModel
import java.util.*

data class Space(
    val name: String = MyDataViewModel.instance.getMyId(),
    val id: String = Crypto().getHash("$name${System.currentTimeMillis()}"),
    val maximum: Int = 2,
    var status: SpaceStatus = SpaceStatus.INACTIVE,
    var connected: Boolean = false,
    var terminated: Boolean = false,
    val calls: MutableList<String> = mutableListOf(),
    val participants: MutableList<String> = mutableListOf(),
    val leaves: MutableList<String> = mutableListOf(),
    val callType: Call.Type = Call.Type.AUDIO,
    val createdBy: String = MyDataViewModel.instance.getMyId(),
    val createdAt: Date? = null,
    val terminatedReason: String? = null,
    val terminatedBy: String? = null,
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
        map[PARTICIPANTS] = participants
        map["leaves"] = leaves
        map["callType"] = callType
        map["createdBy"] = createdBy
        map[CREATED_AT] = createdAt ?: FieldValue.serverTimestamp()
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
