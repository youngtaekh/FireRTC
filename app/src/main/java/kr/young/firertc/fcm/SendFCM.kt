package kr.young.firertc.fcm

import com.google.gson.JsonObject
import kr.young.common.UtilLog
import kr.young.common.UtilLog.Companion.d
import kr.young.common.UtilLog.Companion.e
import kr.young.common.UtilLog.Companion.w
import kr.young.firertc.model.Call
import kr.young.firertc.repo.AppSP
import kr.young.firertc.util.Config.Companion.CALL_ID
import kr.young.firertc.util.Config.Companion.DATA
import kr.young.firertc.util.Config.Companion.SDP
import kr.young.firertc.util.Config.Companion.SPACE_ID
import kr.young.firertc.util.Config.Companion.CALL_TYPE
import kr.young.firertc.util.Config.Companion.CHAT_ID
import kr.young.firertc.util.Config.Companion.FCM_TOKEN
import kr.young.firertc.util.Config.Companion.MESSAGE
import kr.young.firertc.util.Config.Companion.MESSAGE_ID
import kr.young.firertc.util.Config.Companion.NAME
import kr.young.firertc.util.Config.Companion.TO
import kr.young.firertc.util.Config.Companion.TYPE
import kr.young.firertc.util.Config.Companion.USER_ID
import kr.young.firertc.vm.MyDataViewModel
import retrofit2.Callback
import retrofit2.Response

class SendFCM {
    companion object {
        fun sendMessage(
            toToken: String,
            type: FCMType,
            callType: Call.Type = Call.Type.AUDIO,
            spaceId: String? = null,
            callId: String? = null,
            chatId: String? = null,
            messageId: String? = null,
            targetOS: String? = null,
            sdp: String? = null,
            message: String? = null,
            myId: String = "",
            name: String = "",
        ) {
            d(TAG, "toToken $toToken")
            d(TAG, "FCMType $type")
//            d(TAG, "callType $callType")
//            d(TAG, "spaceId $spaceId")
//            d(TAG, "callId $callId")
//            d(TAG, "chatId $chatId")
//            d(TAG, "messageId $messageId")
//            d(TAG, "sdp $sdp")
//            d(TAG, "message $message")
//            d(TAG, "myId $myId")
//            d(TAG, "name $name")
            ApiClient.getApiService().sendNotification(payload = fcmPayload(name, toToken, type, callType, spaceId, callId, chatId, messageId, targetOS, sdp, message))?.enqueue(object:
                Callback<JsonObject?> {
                override fun onResponse(
                    call: retrofit2.Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    if (response.isSuccessful) {
                        d(TAG, "$type send Success")
                    } else {
                        w(TAG, "$type send failure")
                    }
                }

                override fun onFailure(call: retrofit2.Call<JsonObject?>, t: Throwable) {
                    e(TAG, "send failure")
                }
            })
        }

        private fun fcmPayload(
            name: String,
            toToken: String,
            type: FCMType,
            callType: Call.Type,
            spaceId: String?,
            callId: String?,
            chatId: String?,
            messageId: String?,
            targetOS: String?,
            sdp: String?,
            message: String?
        ): JsonObject {
            val payload = JsonObject()
            payload.addProperty(TO, toToken)
            val data = JsonObject()
            val notification = JsonObject()
            notification.addProperty("title", name)
            notification.addProperty("body", type.toString())
            data.addProperty("content_available", true)
            data.addProperty(NAME, name)
            data.addProperty(TYPE, type.toString())
            data.addProperty(CALL_TYPE, callType.toString())
            data.addProperty(USER_ID, MyDataViewModel.instance.getMyId())
            data.addProperty(FCM_TOKEN, AppSP.instance.getFCMToken())
            if (callId != null) {
                data.addProperty(CALL_ID, callId)
            }
            if (spaceId != null) {
                data.addProperty(SPACE_ID, spaceId)
            }
            if (chatId != null) {
                data.addProperty(CHAT_ID, chatId)
            }
            if (messageId != null) {
                data.addProperty(MESSAGE_ID, messageId)
            }
            if (sdp != null) {
                data.addProperty(SDP, sdp)
            }
            if (message != null) {
                data.addProperty(MESSAGE, message)
            }
            payload.add(DATA, data)
            if (targetOS == null || targetOS == "iOS") {
                payload.add("notification", notification)
            }
            return payload
        }

        private const val TAG = "SendFCM"
    }

    enum class FCMType {
        Offer,
        Answer,
        Cancel,
        Decline,
        Bye,
        Busy,
        New,
        Leave,
        Sdp,
        Ice,
        Message,
        Else
    }
}