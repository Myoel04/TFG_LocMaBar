package com.example.locmabar.modelo

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class LugarRepository {
    private val baseDatos: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val lugaresCollection = "Locales"

    suspend fun obtenerTodosLugares(callback: (List<Lugar>, String?) -> Unit) {
        try {
            val querySnapshot = baseDatos.collection(lugaresCollection)
                .get()
                .await()
            if (querySnapshot.isEmpty) {
                callback(emptyList(), "La colección $lugaresCollection está vacía.")
                return
            }

            val lugares = querySnapshot.documents.mapNotNull { documento ->
                try {
                    val lugar = documento.toObject(Lugar::class.java)
                    if (lugar?.isValid() == true) {
                        println("Lugar cargado: ID=${documento.id}, Nombre=${lugar.nombre}, Provincia=${lugar.provincia}, Municipio=${lugar.municipio}, Latitud=${lugar.latitud}, Longitud=${lugar.longitud}")
                        lugar
                    } else {
                        println("Datos inválidos para el documento ${documento.id}: $lugar")
                        null
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
        } catch (e: Exception) {
            println("Error al obtener lugares: $e")
            callback(emptyList(), "Error al obtener lugares: ${e.message}")
        }
    }

    suspend fun obtenerLugaresPorMunicipio(provincia: String, municipio: String, callback: (List<Lugar>, String?) -> Unit) {
        try {
            val provinciaNormalizada = provincia.trim().lowercase()
            val municipioNormalizado = municipio.trim().lowercase()
            println("Buscando lugares en Provincia: $provinciaNormalizada, Municipio: $municipioNormalizado")

            // Cargamos todos los documentos y filtramos manualmente
            val querySnapshot = baseDatos.collection(lugaresCollection)
                .get()
                .await()
            if (querySnapshot.isEmpty) {
                println("No se encontraron documentos en $lugaresCollection.")
                callback(emptyList(), "No se encontraron lugares en $municipio, $provincia.")
                return
            }

            val lugares = querySnapshot.documents.mapNotNull { documento ->
                try {
                    val lugar = documento.toObject(Lugar::class.java)
                    if (lugar?.isValid() == true) {
                        val provinciaDoc = lugar.provincia?.trim()?.lowercase()
                        val municipioDoc = lugar.municipio?.trim()?.lowercase()
                        println("Documento encontrado: ID=${documento.id}, Nombre=${lugar.nombre}, Provincia=$provinciaDoc, Municipio=$municipioDoc")
                        if (provinciaDoc == provinciaNormalizada && municipioDoc == municipioNormalizado) {
                            println("Lugar coincide: ID=${documento.id}, Nombre=${lugar.nombre}")
                            lugar
                        } else {
                            println("Lugar no coincide: Provincia esperada=$provinciaNormalizada, encontrada=$provinciaDoc; Municipio esperado=$municipioNormalizado, encontrado=$municipioDoc")
                            null
                        }
                    } else {
                        println("Datos inválidos para el documento ${documento.id}: $lugar")
                        null
                    }
                } catch (e: Exception) {
                    println("Error al parsear documento ${documento.id}: $e")
                    null
                }
            }

            if (lugares.isEmpty()) {
                println("No se encontraron lugares válidos en $municipio, $provincia.")
                callback(emptyList(), "No se encontraron lugares válidos en $municipio, $provincia.")
            } else {
                callback(lugares, null)
            }
        } catch (e: Exception) {
            println("Error al filtrar lugares: $e")
            callback(emptyList(), "Error al filtrar lugares: ${e.message}")
        }
    }

    suspend fun agregarLugar(lugar: Lugar, callback: (Boolean) -> Unit) {
        val lugarData = mutableMapOf<String, Any>(
            "id" to lugar.id!!,
            "nombre" to lugar.nombre!!,
            "direccion" to lugar.direccion!!,
            "provincia" to lugar.provincia!!,
            "municipio" to lugar.municipio!!,
            "latitud" to lugar.latitudDouble.toString(),
            "longitud" to lugar.longitudDouble.toString()
        )
        lugar.telefono?.let { lugarData["telefono"] = it }
        lugar.horario?.let { lugarData["horario"] = it }
        lugar.valoracion?.let { lugarData["valoracion"] = it }

        try {
            baseDatos.collection(lugaresCollection)
                .document(lugar.id!!)
                .set(lugarData)
                .await()
            callback(true)
        } catch (e: Exception) {
            println("Error al agregar lugar: $e")
            callback(false)
        }
    }
}