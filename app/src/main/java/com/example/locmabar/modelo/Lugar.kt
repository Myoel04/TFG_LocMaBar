package com.example.locmabar.modelo

data class Lugar(
    val id: String? = null,
    val nombre: String? = null,
    val direccion: String? = null,
    val provincia: String? = null,
    val municipio: String? = null,
    val latitud: String? = null, // Cambiado a String? para coincidir con Firestore
    val longitud: String? = null, // Cambiado a String? para coincidir con Firestore
    val telefono: String? = null,
    val horario: String? = null,
    val valoracion: String? = null
) {
    // Propiedades computadas para obtener latitud y longitud como Double
    val latitudDouble: Double?
        get() = latitud?.toDoubleOrNull()

    val longitudDouble: Double?
        get() = longitud?.toDoubleOrNull()

    // Método para validar si el objeto Lugar tiene todos los campos necesarios
    fun isValid(): Boolean {
        return !id.isNullOrBlank() &&
                !nombre.isNullOrBlank() &&
                !direccion.isNullOrBlank() &&
                !provincia.isNullOrBlank() &&
                !municipio.isNullOrBlank() &&
                latitudDouble != null &&
                longitudDouble != null
    }
}