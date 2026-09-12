package com.rfragancias.app.network

import android.content.Context
import com.rfragancias.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var cookieJar: PersistentCookieJar? = null

    fun init(context: Context) {
        if (retrofit != null) return
        synchronized(this) {
            if (retrofit != null) return

            val jar = PersistentCookieJar(context)
            cookieJar = jar

            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val client = OkHttpClient.Builder()
                .cookieJar(jar)
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
    }

    val api: ApiService
        get() = retrofit?.create(ApiService::class.java)
            ?: throw IllegalStateException("ApiClient.init(context) debe llamarse antes de usar la API")

    fun limpiarCookies() {
        cookieJar?.limpiar()
    }
}
