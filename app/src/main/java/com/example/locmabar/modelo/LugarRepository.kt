package com.example.locmabar.modelo

import com.google.firebase.firestore.FirebaseFirestore

class LugarRepository {
    private val baseDatos: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun obtenerTodosLugares(callback: (List<Lugar>) -> Unit) {
        baseDatos.collection("12345")
            .get()
            .addOnSuccessListener { resultado ->
                val lugares = resultado.documents.mapNotNull { documento ->
                    try {
                        val latitudStr = documento.getString("lat") ?: "0.0"
                        val longitudStr = documento.getString("lon") ?: "0.0"
                        Lugar(
                            id = documento.id,
                            nombre = documento.getString("name") ?: "",
                            direccion = documento.getString("address") ?: "",
                            provincia = documento.getString("province") ?: "",
                            municipio = documento.getString("municipality") ?: "",
                            latitud = latitudStr.toDouble(),
                            longitud = longitudStr.toDouble()
                        )
                    } catch (e: Exception) {
                        println("Error al parsear documento ${documento.id}: $e")
                        null
                    }
                }
                callback(lugares)
            }
            .addOnFailureListener {
                println("Error al obtener lugares: $it")
                callback(emptyList())
            }
    }

    fun obtenerLugaresPorMunicipio(provincia: String, municipio: String, callback: (List<Lugar>) -> Unit) {
        baseDatos.collection("12345")
            .whereEqualTo("province", provincia.trim())
            .whereEqualTo("municipality", municipio.trim())
            .get()
            .addOnSuccessListener { resultado ->
                val lugares = resultado.documents.mapNotNull { documento ->
                    try {
                        val latitudStr = documento.getString("lat") ?: "0.0"
                        val longitudStr = documento.getString("lon") ?: "0.0"
                        Lugar(
                            id = documento.id,
                            nombre = documento.getString("name") ?: "",
                            direccion = documento.getString("address") ?: "",
                            provincia = documento.getString("province") ?: "",
                            municipio = documento.getString("municipality") ?: "",
                            latitud = latitudStr.toDouble(),
                            longitud = longitudStr.toDouble()
                        )
                    } catch (e: Exception) {
                        println("Error al parsear documento ${documento.id}: $e")
                        null
                    }
                }
                callback(lugares)
            }
            .addOnFailureListener {
                println("Error al filtrar lugares: $it")
                callback(emptyList())
            }
    }
}