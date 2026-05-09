
package com.example.farmapp.data.network


import com.example.farmapp.data.model.SoilResponse
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

object RetrofitClient {

    // Weather API instance
    private const val WEATHER_BASE_URL = "https://api.weatherapi.com/v1/"
    val weatherApi: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }

    // Soil API instance (Ambee Data - Legacy)
    private const val SOIL_BASE_URL = "https://api.ambeedata.com/"
    val soilApi: SoilApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SOIL_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SoilApiService::class.java)
    }

    // SoilGrids instance
    private const val SOILGRIDS_BASE_URL = "https://rest.isric.org/"
    val soilGridsApi: SoilGridsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(SOILGRIDS_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SoilGridsApiService::class.java)
    }

    // Open-Meteo instance
    private const val OPENMETEO_BASE_URL = "https://api.open-meteo.com/"
    val openMeteoApi: OpenMeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(OPENMETEO_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoApiService::class.java)
    }
}
