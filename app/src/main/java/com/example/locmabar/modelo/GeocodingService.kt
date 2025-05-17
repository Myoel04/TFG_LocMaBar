package com.example.locmabar.modelo

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    @GET("geocode/json")
    fun getLatLngFromAddress(
        @Query("address") address: String,
        @Query("key") apiKey: String,
        @Query("region") region: String = "es", // Añadir región para España
        @Query("language") language: String = "es" // Añadir idioma español
    ): Call<GeocodingResponse>
}

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String,
    val error_message: String? = null // Añadir campo para el mensaje de error
)

data class GeocodingResult(
    val geometry: Geometry
)

data class Geometry(
    val location: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)