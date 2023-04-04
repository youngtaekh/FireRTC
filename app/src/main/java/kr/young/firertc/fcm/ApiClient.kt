package kr.young.firertc.fcm

import kr.young.firertc.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    companion object {
        fun getApiService() =
            Retrofit.Builder()
                .baseUrl(BuildConfig.FCM_BASE_URL)
                .client(provideClient())
                .addConverterFactory(GsonConverterFactory.create())
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