//package com.example.farmapp.data.network
//
//import com.example.farmapp.data.model.WeatherResponse
//import com.example.farmapp.data.model.SoilResponse
//import retrofit2.http.GET
//import retrofit2.http.Query
//
//interface ApiService {
//    // Weather API (WeatherAPI.com)
//    @GET("forecast.json")
//    suspend fun getWeather(
//        @Query("key") apiKey: String,
//        @Query("q") location: String,
//        @Query("days") days: Int = 2
//    ): WeatherResponse
//
//    @GET("latest")
//    suspend fun getSoilData(
//        @Query("lat") lat: Double,
//        @Query("lng") lng: Double,
//        @Query("apikey") apiKey: String
//    ): SoilResponse
//
//
//}

// WeatherApiService.kt
package com.example.farmapp.data.network

import com.example.farmapp.data.model.SoilResponse
import com.example.farmapp.data.model.WeatherResponse
import com.example.farmapp.data.model.SoilGridsResponse
import com.example.farmapp.data.model.OpenMeteoResponse

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface WeatherApiService {
    @GET("forecast.json")
    suspend fun getWeather(
        @Query("key") apiKey: String,
        @Query("q") location: String,
        @Query("days") days: Int = 2
    ): WeatherResponse
}

interface SoilApiService {
    @GET("soil/latest/by-lat-lng")
    suspend fun getSoilData(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Header("x-api-key") apiKey: String
    ): SoilResponse
}

interface SoilGridsApiService {
    @GET("soilgrids/v2.0/properties/query")
    suspend fun getSoilProperties(
        @Query("lon") lon: Double,
        @Query("lat") lat: Double,
        @Query("property") properties: List<String> = listOf("phh2o", "nitrogen"),
        @Query("depth") depths: List<String> = listOf("0-5cm"),
        @Query("value") value: String = "mean"
    ): SoilGridsResponse
}

interface OpenMeteoApiService {
    @GET("v1/forecast")
    suspend fun getAgroData(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("hourly") variables: String = "soil_moisture_0_to_7cm,soil_temperature_0_to_7cm"
    ): OpenMeteoResponse
}
