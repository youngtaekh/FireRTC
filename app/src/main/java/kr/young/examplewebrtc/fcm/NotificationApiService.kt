package kr.young.examplewebrtc.fcm

import com.google.gson.JsonObject
import kr.young.examplewebrtc.BuildConfig
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface NotificationApiService {
    @POST("fcm/send")
    fun sendNotification(
        @Header("Authorization") authorization: String = "key=${BuildConfig.FCM_SERVER_KEY}",
        @Header("Content-Type") contentType: String = "application/json",
        @Body payload: JsonObject?
    ): Call<JsonObject?>?
}