package com.example.farmapp.data.repository

import com.example.farmapp.data.model.SoilResponse
import com.example.farmapp.data.model.WeatherResponse
import com.example.farmapp.data.model.UnifiedSoilData
import com.example.farmapp.data.network.RetrofitClient

class MainRepository {

    private val weatherApi = RetrofitClient.weatherApi
    private val soilGridsApi = RetrofitClient.soilGridsApi
    private val openMeteoApi = RetrofitClient.openMeteoApi

    suspend fun getWeather(apiKey: String, lat: Double, lon: Double): WeatherResponse {
        val location = "$lat,$lon"
        return weatherApi.getWeather(apiKey, location)
    }

    suspend fun getUnifiedSoilData(lat: Double, lon: Double): UnifiedSoilData {
        return try {
            val gridsResponse = soilGridsApi.getSoilProperties(lon, lat)
            val meteoResponse = openMeteoApi.getAgroData(lat, lon)

            // Extract Nitrogen and pH from SoilGrids
            var nitrogen: Double? = null
            var ph: Double? = null
            
            gridsResponse.properties?.layers?.forEach { layer ->
                when (layer.name) {
                    "nitrogen" -> nitrogen = layer.depths?.firstOrNull()?.values?.get("mean")?.let { it / 100.0 }
                    "phh2o" -> ph = layer.depths?.firstOrNull()?.values?.get("mean")?.let { it / 10.0 }
                }
            }

            // Extract Moisture and Temperature from Open-Meteo
            val moisture = meteoResponse.hourly?.soil_moisture_0_to_7cm?.firstOrNull()?.let { it * 100.0 } // Convert to %
            val temperature = meteoResponse.hourly?.soil_temperature_0_to_7cm?.firstOrNull()

            UnifiedSoilData(
                nitrogen = nitrogen,
                ph = ph,
                moisture = moisture,
                temperature = temperature,
                source = "SoilGrids & Open-Meteo"
            )
        } catch (e: Exception) {
            throw e
        }
    }

    // Legacy Soil fetch (Optional)
    suspend fun getSoilData(lat: Double, lng: Double, apiKey: String): SoilResponse {
        return RetrofitClient.soilApi.getSoilData(lat, lng, apiKey)
    }
}
