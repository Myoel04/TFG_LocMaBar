package com.example.locmabar.utils

import kotlin.math.*

fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radioTierra = 6371.0 // Radio de la Tierra en km
    val diferenciaLatitud = Math.toRadians(lat2 - lat1)
    val diferenciaLongitud = Math.toRadians(lon2 - lon1)
    val a = sin(diferenciaLatitud / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(diferenciaLongitud / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return radioTierra * c
}