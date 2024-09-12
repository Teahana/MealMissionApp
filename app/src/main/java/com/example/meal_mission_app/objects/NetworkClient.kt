package com.example.meal_mission_app.objects

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.meal_mission_app.services.ApiService
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalTime
import com.google.gson.JsonDeserializationContext
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
object NetworkClient {  // This is a singleton object

    private const val BASE_URL = "https://mealsmission.com"
    // private const val BASE_URL = "https://192.168.100.35"

    // Lazy initialization of Retrofit
    private val retrofit: Retrofit by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        // Register custom deserializers for LocalDate and LocalTime
        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
            .registerTypeAdapter(LocalTime::class.java, LocalTimeDeserializer())
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))  // Use custom Gson
            .build()
    }

    // Lazy initialization of ApiService
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}

// Custom deserializer for LocalDate
class LocalDateDeserializer : JsonDeserializer<LocalDate> {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalDate {
        return LocalDate.parse(json?.asString, DateTimeFormatter.ISO_DATE)
    }
}

// Custom deserializer for LocalTime
class LocalTimeDeserializer : JsonDeserializer<LocalTime> {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalTime {
        return LocalTime.parse(json?.asString, DateTimeFormatter.ISO_TIME)
    }
}
