package com.example.locmabar.modelo

import java.util.UUID

data class SolicitudRestaurante(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val direccion: String,
    val provincia: String,
    val municipio: String,
    val latitud: Double,
    val longitud: Double,
    val telefono: String? = null,
    val horario: String? = null,
    val valoracion: String? = null,
    val estado: String = "PENDIENTE" // PENDIENTE, APROBADO, RECHAZADO
)