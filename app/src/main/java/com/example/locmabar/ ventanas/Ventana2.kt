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
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.*

// Modelo de datos para un lugar
data class Place(
    val id: String,
    val name: String,
    val address: String,
    val province: String,
    val municipality: String,
    val lat: Double,
    val lon: Double
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Ventana2(navController: NavController) {
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    val db = FirebaseFirestore.getInstance()

    // Estado para la ubicación del usuario
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLon by remember { mutableStateOf<Double?>(null) }

    // Estado para indicar si la obtención de la ubicación falló
    var locationFailed by remember { mutableStateOf(false) }

    // Estado para los combobox
    val provinces = listOf("Madrid", "Barcelona", "Valencia", "Sevilla", "Cuenca")
    var selectedProvince by remember { mutableStateOf("") }
    var expandedProvince by remember { mutableStateOf(false) }
    var selectedMunicipality by remember { mutableStateOf("") }
    var expandedMunicipality by remember { mutableStateOf(false) }
    val municipalities = getMunicipalities(selectedProvince)

    // Lista de lugares
    var places by remember { mutableStateOf(listOf<Place>()) }

    // Estado de carga
    var isLoading by remember { mutableStateOf(false) }

    // Estado para manejar si el permiso fue denegado
    var permissionDenied by remember { mutableStateOf(false) }

    // Permiso de ubicación
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Verificar si los servicios de ubicación están habilitados
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    if (!isGpsEnabled && !isNetworkEnabled) {
        locationFailed = true
        println("Los servicios de ubicación están deshabilitados.")
    }

    // Solicitar ubicación si se concede el permiso
    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            if (isGpsEnabled || isNetworkEnabled) {
                isLoading = true
                permissionDenied = false
                locationFailed = false

                requestLocation(fusedLocationClient,
                    onSuccess = { location ->
                        userLat = location.latitude
                        userLon = location.longitude
                        println("Ubicación obtenida: lat=$userLat, lon=$userLon")

                        fetchAllPlaces(db) { allPlaces ->
                            val filteredPlaces = allPlaces.filter { place ->
                                calculateDistance(userLat!!, userLon!!, place.lat, place.lon) < 50.0
                            }
                            places = filteredPlaces
                            isLoading = false
                            if (filteredPlaces.isEmpty()) {
                                locationFailed = true
                                println("No se encontraron lugares cercanos")
                            }
                        }
                    },
                    onFailure = {
                        println("Error al obtener ubicación")
                        isLoading = false
                        locationFailed = true
                    }
                )
            } else {
                println("Servicios de ubicación deshabilitados")
                isLoading = false
                locationFailed = true
            }
        } else {
            if (!locationPermissionState.status.isGranted &&
                !locationPermissionState.status.shouldShowRationale) {
                permissionDenied = true
            }
            locationPermissionState.launchPermissionRequest()
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

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando lugares...", fontSize = 14.sp)
        } else {
            if (permissionDenied) {
                Text(
                    text = "Permiso de ubicación denegado. Selecciona provincia y municipio manualmente.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (locationFailed) {
                Text(
                    text = "No se pudo obtener la ubicación. Asegúrate de que los servicios de ubicación estén habilitados.",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (userLat == null || userLon == null || locationFailed || places.isEmpty()) {
                // Selector de provincia
                ExposedDropdownMenuBox(
                    expanded = expandedProvince,
                    onExpandedChange = { expandedProvince = !expandedProvince }
                ) {
                    TextField(
                        value = selectedProvince,
                        onValueChange = {},
                        label = { Text("Provincia") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedProvince,
                        onDismissRequest = { expandedProvince = false }
                    ) {
                        provinces.forEach { province ->
                            DropdownMenuItem(
                                text = { Text(province) },
                                onClick = {
                                    selectedProvince = province
                                    selectedMunicipality = ""
                                    expandedProvince = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Selector de municipio
                ExposedDropdownMenuBox(
                    expanded = expandedMunicipality,
                    onExpandedChange = { expandedMunicipality = !expandedMunicipality }
                ) {
                    TextField(
                        value = selectedMunicipality,
                        onValueChange = {},
                        label = { Text("Municipio") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMunicipality,
                        onDismissRequest = { expandedMunicipality = false }
                    ) {
                        municipalities.forEach { municipality ->
                            DropdownMenuItem(
                                text = { Text(municipality) },
                                onClick = {
                                    selectedMunicipality = municipality
                                    expandedMunicipality = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (selectedProvince.isNotEmpty() && selectedMunicipality.isNotEmpty()) {
                            isLoading = true
                            fetchPlacesByMunicipality(db, selectedProvince, selectedMunicipality) { result ->
                                places = result
                                isLoading = false
                                locationFailed = false
                            }
                        }
                    },
                    enabled = selectedProvince.isNotEmpty() && selectedMunicipality.isNotEmpty()
                ) {
                    Text("Buscar")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de lugares
            if (places.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(places) { place ->
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(place.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(place.address, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${place.municipality}, ${place.province}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (!isLoading && (userLat != null || (selectedProvince.isNotEmpty() && selectedMunicipality.isNotEmpty()))) {
                Text("No se encontraron lugares.", fontSize = 14.sp)
            }
        }
    }
}

private fun requestLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onSuccess: (Location) -> Unit,
    onFailure: () -> Unit
) {
    try {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
            numUpdates = 1
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onSuccess(location)
            } else {
                // Si lastLocation es null, solicitamos actualizaciones
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            val newLocation = locationResult.lastLocation
                            if (newLocation != null) {
                                onSuccess(newLocation)
                                fusedLocationClient.removeLocationUpdates(this)
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

fun getMunicipalities(province: String): List<String> {
    return when (province) {
        "Madrid" -> listOf("Madrid", "Alcalá de Henares", "Getafe")
        "Barcelona" -> listOf("Barcelona", "Badalona", "Hospitalet")
        "Valencia" -> listOf("Valencia", "Torrent", "Gandía")
        "Sevilla" -> listOf("Sevilla", "Dos Hermanas", "Alcalá de Guadaíra")
        "Cuenca" -> listOf("Iniesta", "Cuenca", "Tarancón")
        else -> emptyList()
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radio de la Tierra en km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun fetchAllPlaces(db: FirebaseFirestore, callback: (List<Place>) -> Unit) {
    db.collection("12345")
        .get()
        .addOnSuccessListener { result ->
            val places = result.documents.mapNotNull { doc ->
                try {
                    val latStr = doc.getString("lat") ?: "0.0"
                    val lonStr = doc.getString("lon") ?: "0.0"
                    Place(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        address = doc.getString("address") ?: "",
                        province = doc.getString("province") ?: "",
                        municipality = doc.getString("municipality") ?: "",
                        lat = latStr.toDouble(),
                        lon = lonStr.toDouble()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            callback(places)
        }
        .addOnFailureListener {
            println("Error al obtener lugares: $it")
            callback(emptyList())
        }
}

fun fetchPlacesByMunicipality(
    db: FirebaseFirestore,
    province: String,
    municipality: String,
    callback: (List<Place>) -> Unit
) {
    db.collection("12345")
        .whereEqualTo("province", province.trim())
        .whereEqualTo("municipality", municipality.trim())
        .get()
        .addOnSuccessListener { result ->
            val places = result.documents.mapNotNull { doc ->
                try {
                    val latStr = doc.getString("lat") ?: "0.0"
                    val lonStr = doc.getString("lon") ?: "0.0"
                    Place(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        address = doc.getString("address") ?: "",
                        province = doc.getString("province") ?: "",
                        municipality = doc.getString("municipality") ?: "",
                        lat = latStr.toDouble(),
                        lon = lonStr.toDouble()
                    )
                } catch (e: Exception) {
                    null
                }
            }
            callback(places)
        }
        .addOnFailureListener {
            println("Error al filtrar lugares: $it")
            callback(emptyList())
        }
}