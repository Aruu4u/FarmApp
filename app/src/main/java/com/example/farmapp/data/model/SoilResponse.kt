package com.example.farmapp.data.model


data class SoilResponse(
    val message: String? = null,
    val data: List<AmbeeSoilData>? = null
)

data class AmbeeSoilData(
    val soil_moisture: Double? = null,
    val soil_ph: Double? = null,
    val soil_temperature: Double? = null,
    val fertility: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null
)

