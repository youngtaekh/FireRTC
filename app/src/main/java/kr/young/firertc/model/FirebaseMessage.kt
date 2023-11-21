package kr.young.firertc.model

import kr.young.firertc.util.Config.Companion.CALL_ID
import kr.young.firertc.util.Config.Companion.CALL_TYPE
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.util.Config.Companion.MESSAGE
import kr.young.firertc.util.Config.Companion.MESSAGE_ID
import kr.young.firertc.util.Config.Companion.NAME
import kr.young.firertc.util.Config.Companion.SDP
import kr.young.firertc.util.Config.Companion.SPACE_ID
import kr.young.firertc.util.Config.Companion.TARGET_OS
import kr.young.firertc.util.Config.Companion.TYPE
import kr.young.firertc.util.Config.Companion.USER_ID

class FirebaseMessage constructor(data: Map<String, String>) {
    var userId: String? = null
    var spaceId: String? = null
    var callId: String? = null
    var chatId: String? = null
    var messageId: String? = null
    var type: String? = null
    var callType: String? = null
    var name: String? = null
    var targetOS: String? = null
    var sdp: String? = null
    var fcmToken: String? = null
    var message: String? = null

    init {
        this.userId = data[USER_ID]
        this.spaceId = data[SPACE_ID]
        this.callId = data[CALL_ID]
        this.chatId = data[CHAT_ID]
        this.messageId = data[MESSAGE_ID]
        this.type = data[TYPE]
        this.callType = data[CALL_TYPE]
        this.name = data[NAME]
        this.targetOS = data[TARGET_OS]
        this.sdp = data[SDP]
        this.fcmToken = data[FCM_TOKEN]
        this.message = data[MESSAGE]
    }
}