package kr.young.examplewebrtc.model

import kr.young.common.Crypto
import kr.young.common.DateUtil

data class Space(
    val name: String = "",
    val id: String = Crypto().getHash("$name${System.currentTimeMillis()}"),
    val maximum: Int = 2,
    var status: SpaceStatus = SpaceStatus.INACTIVE,
    val calls: MutableList<String> = mutableListOf(),
    val createdBy: String = "",
    val createdAt: String = DateUtil.toFormattedString(System.currentTimeMillis()),
    var terminatedAt: String? = null
) {
    override fun toString(): String {
        return "Space(name=$name max=$maximum status=$status creator=$createdBy id=$id calls=$calls)"
    }

    enum class SpaceStatus {
        ACTIVE,
        INACTIVE,
        TERMINATED
    }

    companion object {
        private const val TAG = "Space"
        const val COLLECTION = "spaces"
        const val NAME = "name"
        const val STATUS = "status"
        const val CALLS = "calls"

        fun notTerminated(): List<String> {
            return listOf(
                SpaceStatus.ACTIVE.toString(),
                SpaceStatus.INACTIVE.toString()
            )
        }
    }
}
