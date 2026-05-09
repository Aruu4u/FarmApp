package com.example.farmapp.data.model

data class UnifiedSoilData(
    val nitrogen: Double? = null,
    val phosphorus: Double? = null,
    val potassium: Double? = null,
    val ph: Double? = null,
    val moisture: Double? = null,
    val temperature: Double? = null,
    val source: String? = null
)

// SoilGrids Models
data class SoilGridsResponse(
    val properties: SoilGridsProperties? = null
)

data class SoilGridsProperties(
    val layers: List<SoilGridsLayer>? = null
)

data class SoilGridsLayer(
    val name: String? = null,
    val depths: List<SoilGridsDepth>? = null
)

data class SoilGridsDepth(
    val label: String? = null,
    val values: Map<String, Double>? = null
)

// Open-Meteo Models
data class OpenMeteoResponse(
    val hourly: OpenMeteoHourly? = null
)

data class OpenMeteoHourly(
    val soil_moisture_0_to_7cm: List<Double>? = null,
    val soil_temperature_0_to_7cm: List<Double>? = null
)
