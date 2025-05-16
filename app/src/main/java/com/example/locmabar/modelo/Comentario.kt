package com.example.locmabar.modelo

import com.google.firebase.Timestamp

data class Comentario(
    val id: String = "",
    val texto: String = "",
    val usuarioId: String = "",
    val lugarId: String = "",
    val estado: String = "PENDIENTE",
    val fechaCreacion: Timestamp? = null,
    val valoracion: String? = null
)