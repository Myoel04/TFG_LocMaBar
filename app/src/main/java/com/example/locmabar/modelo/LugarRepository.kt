package com.example.locmabar.modelo

import com.google.firebase.firestore.FirebaseFirestore

class LugarRepository {
    private val baseDatos: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val lugaresCollection = "Locales"

    fun obtenerTodosLugares(callback: (List<Lugar>, String?) -> Unit) {
        baseDatos.collection(lugaresCollection)
            .get()
            .addOnSuccessListener { resultado ->
                if (resultado.isEmpty) {
                    callback(emptyList(), "La colección $lugaresCollection está vacía.")
                    return@addOnSuccessListener
                }

                val lugares = resultado.documents.mapNotNull { documento ->
                    try {
                        val latitudStr = documento.getString("latitud") ?: "0.0"
                        val longitudStr = documento.getString("longitud") ?: "0.0"
                        val latitud = latitudStr.toDoubleOrNull() ?: 0.0
                        val longitud = longitudStr.toDoubleOrNull() ?: 0.0

                        if (latitud == 0.0 && longitud == 0.0) {
                            println("Coordenadas inválidas para el documento ${documento.id}: latitud=$latitudStr, longitud=$longitudStr")
                            null
                        } else {
                            Lugar(
                                id = documento.id,
                                nombre = documento.getString("nombre") ?: "",
                                direccion = documento.getString("direccion") ?: "",
                                provincia = documento.getString("provincia") ?: "",
                                municipio = documento.getString("municipio") ?: "",
                                latitud = latitud,
                                longitud = longitud,
                                telefono = documento.getString("telefono"),
                                horario = documento.getString("horario"),
                                valoracion = documento.getString("valoracion")
                            )
                        }
                    } catch (e: Exception) {
                        println("Error al parsear documento ${documento.id}: $e")
                        null
                    }
                }

                if (lugares.isEmpty()) {
                    callback(emptyList(), "No se encontraron lugares válidos en la colección $lugaresCollection.")
                } else {
                    callback(lugares, null)
                }
            }
            .addOnFailureListener { exception ->
                println("Error al obtener lugares: $exception")
                callback(emptyList(), "Error al obtener lugares: ${exception.message}")
            }
    }

    fun obtenerLugaresPorMunicipio(provincia: String, municipio: String, callback: (List<Lugar>, String?) -> Unit) {
        baseDatos.collection(lugaresCollection)
            .whereEqualTo("provincia", provincia.trim())
            .whereEqualTo("municipio", municipio.trim())
            .get()
            .addOnSuccessListener { resultado ->
                if (resultado.isEmpty) {
                    callback(emptyList(), "No se encontraron lugares en $municipio, $provincia.")
                    return@addOnSuccessListener
                }

                val lugares = resultado.documents.mapNotNull { documento ->
                    try {
                        val latitudStr = documento.getString("latitud") ?: "0.0"
                        val longitudStr = documento.getString("longitud") ?: "0.0"
                        val latitud = latitudStr.toDoubleOrNull() ?: 0.0
                        val longitud = longitudStr.toDoubleOrNull() ?: 0.0

                        if (latitud == 0.0 && longitud == 0.0) {
                            println("Coordenadas inválidas para el documento ${documento.id}: latitud=$latitudStr, longitud=$longitudStr")
                            null
                        } else {
                            Lugar(
                                id = documento.id,
                                nombre = documento.getString("nombre") ?: "",
                                direccion = documento.getString("direccion") ?: "",
                                provincia = documento.getString("provincia") ?: "",
                                municipio = documento.getString("municipio") ?: "",
                                latitud = latitud,
                                longitud = longitud,
                                telefono = documento.getString("telefono"),
                                horario = documento.getString("horario"),
                                valoracion = documento.getString("valoracion")
                            )
                        }
                    } catch (e: Exception) {
                        println("Error al parsear documento ${documento.id}: $e")
                        null
                    }
                }

                if (lugares.isEmpty()) {
                    callback(emptyList(), "No se encontraron lugares válidos en $municipio, $provincia.")
                } else {
                    callback(lugares, null)
                }
            }
            .addOnFailureListener { exception ->
                println("Error al filtrar lugares: $exception")
                callback(emptyList(), "Error al filtrar lugares: ${exception.message}")
            }
    }

    fun agregarLugar(lugar: Lugar, callback: (Boolean) -> Unit) {
        val lugarData = mutableMapOf<String, Any>(
            "nombre" to lugar.nombre,
            "direccion" to lugar.direccion,
            "provincia" to lugar.provincia,
            "municipio" to lugar.municipio,
            "latitud" to lugar.latitud.toString(),
            "longitud" to lugar.longitud.toString()
        )
        lugar.telefono?.let { lugarData["telefono"] = it }
        lugar.horario?.let { lugarData["horario"] = it }
        lugar.valoracion?.let { lugarData["valoracion"] = it }

        baseDatos.collection(lugaresCollection)
            .document(lugar.id)
            .set(lugarData)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                println("Error al agregar lugar: $exception")
                callback(false)
            }
    }
}