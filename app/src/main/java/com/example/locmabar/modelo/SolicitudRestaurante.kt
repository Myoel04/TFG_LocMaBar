package com.example.locmabar.modelo

data class SolicitudRestaurante(
    val id: String? = "",
    val nombre: String? = null,
    val direccion: String? = null,
    val provincia: String? = null,
    val municipio: String? = null,
    val latitud: String? = null,
    val longitud: String? = null,
    val telefono: String? = null,
    val horario: String? = null,
    val valoracion: String? = null,
    val estado: String? = "PENDIENTE"
) {
    // Propiedades computadas para obtener latitud y longitud como Double
    val latitudDouble: Double
        get() = latitud?.toDoubleOrNull() ?: 0.0

    val longitudDouble: Double
        get() = longitud?.toDoubleOrNull() ?: 0.0

    // Método para validar si la solicitud tiene todos los campos necesarios
    fun isValid(): Boolean {
        return !id.isNullOrBlank() &&
                !nombre.isNullOrBlank() &&
                !direccion.isNullOrBlank() &&
                !provincia.isNullOrBlank() &&
                !municipio.isNullOrBlank() &&
                !latitud.isNullOrBlank() &&
                !longitud.isNullOrBlank() &&
                !estado.isNullOrBlank()
    }
}