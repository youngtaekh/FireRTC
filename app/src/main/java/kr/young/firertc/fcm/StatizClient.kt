package kr.young.firertc.fcm

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StatizClient {
    companion object {
        private val gson = GsonBuilder().setLenient().create()

        fun getApiService() = Retrofit.Builder()
            .baseUrl("http://www.statiz.co.kr/")
            .client(provideClient())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(NotificationApiService::class.java)

        private fun provideClient(): OkHttpClient {
            val interceptor = HttpLoggingInterceptor()
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            return OkHttpClient.Builder()
//                .addInterceptor(interceptor)
                .addInterceptor { chain -> chain.proceed(chain.request()) }
                .build()
        }
    }
}