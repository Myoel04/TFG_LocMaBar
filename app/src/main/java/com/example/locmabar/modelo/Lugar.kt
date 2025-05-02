package com.example.locmabar.modelo

data class Lugar(
    val id: String,
    val nombre: String,
    val direccion: String,
    val provincia: String,
    val municipio: String,
    val latitud: Double,
    val longitud: Double,
    val telefono: String? = null,  // Opcional, puedes llenarlo si tienes el dato
    val horario: String? = null,   // Opcional, puedes llenarlo si tienes el dato
    val valoracion: String? = null // Opcional, puedes llenarlo si tienes el dato
)