package com.example.locmabar.ventanas

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.navigation.NavHostController
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
fun Ventana2(navController: NavHostController) {
    val contexto = LocalContext.current
    val clienteUbicacionFusionada = LocationServices.getFusedLocationProviderClient(contexto)
    val baseDatos = FirebaseFirestore.getInstance()

    // Estado para la ubicación del usuario
    var latitudUsuario by remember { mutableStateOf<Double?>(null) }
    var longitudUsuario by remember { mutableStateOf<Double?>(null) }
    var falloUbicacion by remember { mutableStateOf(false) }
    var permisoDenegado by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }

    val provincias = listOf("Madrid", "Barcelona", "Valencia", "Sevilla", "Cuenca")
    var provinciaSeleccionada by remember { mutableStateOf("") }
    var expandirProvincia by remember { mutableStateOf(false) }
    var municipioSeleccionado by remember { mutableStateOf("") }
    var expandirMunicipio by remember { mutableStateOf(false) }
    val municipios = obtenerMunicipios(provinciaSeleccionada)

    // Lista de lugares
    var lugares by remember { mutableStateOf(listOf<Lugar>()) }

    // Permiso de ubicación
    val estadoPermisoUbicacion = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Verifico si los servicios de ubicación están habilitados
    val gestorUbicacion = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gpsHabilitado = gestorUbicacion.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val redHabilitada = gestorUbicacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    // Depuración para entender el estado
    LaunchedEffect(Unit) {
        println("Ventana2 iniciada")
        println("Permiso: isGranted=${estadoPermisoUbicacion.status.isGranted}, shouldShowRationale=${estadoPermisoUbicacion.status.shouldShowRationale}")
        println("GPS habilitado: $gpsHabilitado, Red habilitada: $redHabilitada")
    }

    // Solicitar permiso y ubicación
    LaunchedEffect(estadoPermisoUbicacion) {
        if (!estadoPermisoUbicacion.status.isGranted) {
            println("Solicitando permiso de ubicación...")
            estadoPermisoUbicacion.launchPermissionRequest()
        } else {
            println("Permiso ya concedido, verificando ubicación")
            if (gpsHabilitado || redHabilitada) {
                cargando = true
                permisoDenegado = false
                falloUbicacion = false

                solicitarUbicacion(clienteUbicacionFusionada,
                    onSuccess = { ubicacion ->
                        latitudUsuario = ubicacion.latitude
                        longitudUsuario = ubicacion.longitude
                        println("Ubicación obtenida: lat=$latitudUsuario, lon=$longitudUsuario")

                        obtenerTodosLugares(baseDatos) { todosLugares ->
                            val lugaresFiltrados = todosLugares.filter { lugar ->
                                calcularDistancia(latitudUsuario!!, longitudUsuario!!, lugar.latitud, lugar.longitud) < 50.0
                            }
                            lugares = lugaresFiltrados
                            cargando = false
                            if (lugaresFiltrados.isEmpty()) {
                                falloUbicacion = true
                                println("No se encontraron lugares cercanos")
                            }
                        }
                    },
                    onFailure = {
                        println("Error al obtener ubicación")
                        cargando = false
                        falloUbicacion = true
                    }
                )
            } else {
                println("Servicios de ubicación deshabilitados")
                cargando = false
                falloUbicacion = true
            }
        }
        // Verificar estado después de la solicitud
        if (!estadoPermisoUbicacion.status.isGranted && !estadoPermisoUbicacion.status.shouldShowRationale) {
            permisoDenegado = true
            println("Permiso denegado permanentemente")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Bares y Restaurantes Cercanos", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando lugares...", fontSize = 14.sp)
        } else {
            if (permisoDenegado) {
                Text(
                    text = "Permiso de ubicación denegado. Selecciona provincia y municipio manualmente o revisa los permisos en Configuración.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        contexto.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = android.net.Uri.fromParts("package", contexto.packageName, null)
                        })
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Ir a Configuración")
                }
            }

            if (falloUbicacion) {
                Text(
                    text = "No se pudo obtener la ubicación. Asegúrate de que los servicios de ubicación estén habilitados.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        contexto.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Activar ubicación")
                }
                Button(
                    onClick = { estadoPermisoUbicacion.launchPermissionRequest() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Reintentar permiso")
                }
            }

            if (latitudUsuario == null || longitudUsuario == null || falloUbicacion || lugares.isEmpty()) {
                // Selector de provincia
                ExposedDropdownMenuBox(
                    expanded = expandirProvincia,
                    onExpandedChange = { expandirProvincia = !expandirProvincia }
                ) {
                    TextField(
                        value = provinciaSeleccionada,
                        onValueChange = {},
                        label = { Text("Provincia") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandirProvincia,
                        onDismissRequest = { expandirProvincia = false }
                    ) {
                        provincias.forEach { provincia ->
                            DropdownMenuItem(
                                text = { Text(provincia) },
                                onClick = {
                                    provinciaSeleccionada = provincia
                                    municipioSeleccionado = ""
                                    expandirProvincia = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Selector de municipio
                ExposedDropdownMenuBox(
                    expanded = expandirMunicipio,
                    onExpandedChange = { expandirMunicipio = !expandirMunicipio }
                ) {
                    TextField(
                        value = municipioSeleccionado,
                        onValueChange = {},
                        label = { Text("Municipio") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandirMunicipio,
                        onDismissRequest = { expandirMunicipio = false }
                    ) {
                        municipios.forEach { municipio ->
                            DropdownMenuItem(
                                text = { Text(municipio) },
                                onClick = {
                                    municipioSeleccionado = municipio
                                    expandirMunicipio = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()) {
                            cargando = true
                            obtenerLugaresPorMunicipio(baseDatos, provinciaSeleccionada, municipioSeleccionado) { resultado ->
                                lugares = resultado
                                cargando = false
                                falloUbicacion = false
                            }
                        }
                    },
                    enabled = provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()
                ) {
                    Text("Buscar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (lugares.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(lugares) { lugar ->
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(lugar.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(lugar.direccion, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${lugar.municipio}, ${lugar.provincia}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (!cargando && (latitudUsuario != null || (provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()))) {
                Text("No se encontraron lugares.", fontSize = 14.sp)
            }
        }
    }
}

private fun solicitarUbicacion(
    clienteUbicacionFusionada: FusedLocationProviderClient,
    onSuccess: (Location) -> Unit,
    onFailure: () -> Unit
) {
    try {
        val solicitudUbicacion = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
            numUpdates = 1
        }

        clienteUbicacionFusionada.lastLocation.addOnSuccessListener { ubicacion ->
            if (ubicacion != null) {
                onSuccess(ubicacion)
            } else {
                clienteUbicacionFusionada.requestLocationUpdates(
                    solicitudUbicacion,
                    object : LocationCallback() {
                        override fun onLocationResult(resultadoUbicacion: LocationResult) {
                            val nuevaUbicacion = resultadoUbicacion.lastLocation
                            if (nuevaUbicacion != null) {
                                onSuccess(nuevaUbicacion)
                                clienteUbicacionFusionada.removeLocationUpdates(this)
                            } else {
                                onFailure()
                            }
                        }
                    },
                    Looper.getMainLooper()
                )
            }
        }.addOnFailureListener {
            onFailure()
        }
    } catch (e: SecurityException) {
        onFailure()
    }
}

fun obtenerMunicipios(provincia: String): List<String> {
    return when (provincia) {
        "Madrid" -> listOf("Madrid", "Alcalá de Henares", "Getafe")
        "Barcelona" -> listOf("Barcelona", "Badalona", "Hospitalet")
        "Valencia" -> listOf("Valencia", "Torrent", "Gandía")
        "Sevilla" -> listOf("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra")
        "Cuenca" -> listOf("Iniesta", "Cuenca", "Tarancón")
        else -> emptyList()
    }
}

private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val radioTierra = 6371.0 // Radio de la Tierra en km
    val diferenciaLatitud = Math.toRadians(lat2 - lat1)
    val diferenciaLongitud = Math.toRadians(lon2 - lon1)
    val a = sin(diferenciaLatitud / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(diferenciaLongitud / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return radioTierra * c
}

fun obtenerTodosLugares(baseDatos: FirebaseFirestore, callback: (List<Lugar>) -> Unit) {
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

fun obtenerLugaresPorMunicipio(
    baseDatos: FirebaseFirestore,
    provincia: String,
    municipio: String,
    callback: (List<Lugar>) -> Unit
) {
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