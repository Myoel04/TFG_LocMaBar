package com.example.locmabar.funciones

import kotlin.math.*

fun obtenerMunicipios(provincia: String): List<String> {
    return when (provincia) {
        "Madrid" -> listOf("Madrid", "Alcalá de Henares", "Getafe")
        "Barcelona" -> listOf("Barcelona", "Badalona", "Hospitalet")
        "Valencia" -> listOf("Valencia", "Torrent", "Gandía")
        "Sevilla" -> listOf("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra")
        "Cuenca" -> listOf("Iniesta", "Cuenca", "Tarancón")
        else -> emptyList()
    }
}

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radioTierra = 6371.0 // Radio de la Tierra en km
    val diferenciaLatitud = Math.toRadians(lat2 - lat1)
    val diferenciaLongitud = Math.toRadians(lon2 - lon1)
    val a = sin(diferenciaLatitud / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(diferenciaLongitud / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return radioTierra * c
}