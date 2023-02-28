package kr.young.examplewebrtc.model

import kr.young.common.DateUtil
import kr.young.examplewebrtc.util.Config.Companion.NAME

data class User (
    var id: String = "",
    var password: String = "",
    var name: String = id,
    var fcmToken: String? = null,
    val createdAt: String = DateUtil.toFormattedString(System.currentTimeMillis()),
) {
    override fun toString(): String {
        return "id ${id.substring(0,5)}, pwd $password, $NAME $name, createdAt $createdAt\nfcmToken $fcmToken"
    }

    companion object {
        private const val TAG = "User"
    }
}
