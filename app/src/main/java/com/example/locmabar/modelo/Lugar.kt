package com.example.locmabar.modelo

data class Lugar(
    val id: String,
    val nombre: String,
    val direccion: String,
    val provincia: String,
    val municipio: String,
    val latitud: Double,
    val longitud: Double
)