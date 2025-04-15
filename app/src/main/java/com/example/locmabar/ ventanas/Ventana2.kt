package com.example.locmabar.ventanas

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController // Usar solo NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*

// datos para un bar o restaurante
data class Lugar(
    val id: String,
    val nombre: String,
    val direccion: String,
    val provincia: String,
    val municipio: String,
    val latitud: Double,
    val longitud: Double
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Ventana2(navController: NavHostController) { // Cambiado a NavHostController y navController
    val contexto = LocalContext.current
    val clienteUbicacionFusionada = LocationServices.getFusedLocationProviderClient(contexto)
    val baseDatos = FirebaseFirestore.getInstance()

    // ... (el resto del código sigue igual)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bares y Restaurantes Cercanos", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // ... (sin cambios en la lógica de ubicación, Firestore, etc.)
    }
}

// ... (funciones solicitarUbicacion, obtenerMunicipios, calcularDistancia sin cambios)

fun obtenerTodosLugares(baseDatos: FirebaseFirestore, callback: (List<Lugar>) -> Unit) {
    baseDatos.collection("bares") // Cambiado de "12345" a "bares"
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

fun obtenerLugaresPorMunicipio(
    baseDatos: FirebaseFirestore,
    provincia: String,
    municipio: String,
    callback: (List<Lugar>) -> Unit
) {
    baseDatos.collection("bares") // Cambiado de "12345" a "bares"
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