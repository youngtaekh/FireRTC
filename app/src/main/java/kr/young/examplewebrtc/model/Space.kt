package kr.young.examplewebrtc.model

import kr.young.common.Crypto
import kr.young.common.DateUtil
import kr.young.examplewebrtc.util.Config.Companion.NAME

data class Space(
    val name: String = "",
    val id: String = Crypto().getHash("$name${System.currentTimeMillis()}"),
    val maximum: Int = 2,
    var status: SpaceStatus = SpaceStatus.INACTIVE,
    val callIds: MutableList<String> = mutableListOf(),
//    val participants: MutableList<String> = mutableListOf(),
    val createdBy: String = "",
    val createdAt: String = DateUtil.toFormattedString(System.currentTimeMillis()),
    var terminatedAt: String? = null
) {
    override fun toString(): String {
        return "$NAME=$name max=$maximum status=$status creator=$createdBy id=${id.substring(0,5)} calls=$callIds)"
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
