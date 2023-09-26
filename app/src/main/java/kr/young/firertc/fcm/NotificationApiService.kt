package kr.young.firertc.fcm

import com.google.gson.JsonObject
import kr.young.firertc.BuildConfig
import kr.young.firertc.model.Game
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationApiService {
    @POST("fcm/send")
    fun sendNotification(
        @Header("Authorization") authorization: String = "key=${BuildConfig.FCM_SERVER_KEY}",
        @Header("Content-Type") contentType: String = "application/json",
        @Body payload: JsonObject?
    ): Call<JsonObject?>?

    @GET("team.php")
    fun getTeam(
        @Query("opt")opt: Int = 0,
        @Query("sopt")sopt: Int = 1,
        @Query("year")year: Int = 2023,
        @Query("team")team: String
    ): Call<ResponseBody>

    @GET("schedule.php")
    fun getSchedule(
        @Query("opt")month: Int,
        @Query("sy")year: Int
    ): Call<ResponseBody>

    @GET("boxscore.php")
    fun getBoxScore(
        @Query("date")date: String,
        @Query("stadium")stadium: String,
        @Query("hour")hour: Int
    ): Call<ResponseBody>
}