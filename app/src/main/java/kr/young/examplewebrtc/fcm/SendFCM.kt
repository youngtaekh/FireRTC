package kr.young.examplewebrtc.fcm

import com.google.gson.JsonObject
import kr.young.common.UtilLog
import kr.young.examplewebrtc.util.Config.Companion.CALL_ID
import kr.young.examplewebrtc.util.Config.Companion.DATA
import kr.young.examplewebrtc.util.Config.Companion.SDP
import kr.young.examplewebrtc.util.Config.Companion.SPACE_ID
import kr.young.examplewebrtc.util.Config.Companion.TO
import kr.young.examplewebrtc.util.Config.Companion.TYPE
import retrofit2.Callback
import retrofit2.Response

class SendFCM {
    companion object {
        fun sendMessage(
            to: String,
            type: FCMType,
            spaceId: String? = null,
            callId: String? = null,
            sdp: String? = null,
        ) {
            ApiClient.getApiService().sendNotification(payload = fcmPayload(to, type, spaceId, callId, sdp))?.enqueue(object:
                Callback<JsonObject?> {
                override fun onResponse(
                    call: retrofit2.Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    if (response.isSuccessful) {
                        UtilLog.d(TAG, "send Success")
                    } else {
                        UtilLog.w(TAG, "send failure")
                    }
                }

                override fun onFailure(call: retrofit2.Call<JsonObject?>, t: Throwable) {
                    UtilLog.e(TAG, "send failure")
                }
            })
        }

        private fun fcmPayload(
            to: String,
            type: FCMType,
            spaceId: String?,
            callId: String?,
            sdp: String?
        ): JsonObject {
            val payload = JsonObject()
            payload.addProperty(TO, to)
            val data = JsonObject()
            data.addProperty(TYPE, type.toString())
            if (callId != null) {
                data.addProperty(CALL_ID, callId)
            }
            if (spaceId != null) {
                data.addProperty(SPACE_ID, spaceId)
            }
            if (sdp != null) {
                data.addProperty(SDP, sdp)
            }
            payload.add(DATA, data)
            return payload
        }

        private const val TAG = "SendFCM"
    }

    enum class FCMType {
        New,
        Leave,
        Sdp,
        Ice,
        Else
    }
}