package com.example.locmabar.vista

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.ComunidadAutonoma
import com.example.locmabar.modelo.Lugar
import com.example.locmabar.modelo.LugarRepository
import com.example.locmabar.modelo.UbicacionService
import com.example.locmabar.utils.calcularDistancia
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.URLEncoder

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Ventana2(navController: NavHostController) {
    val contexto = LocalContext.current
    val ubicacionService = UbicacionService(contexto)
    val lugarRepository = LugarRepository()

    // Estado para la ubicación del usuario
    var latitudUsuario by remember { mutableStateOf<Double?>(null) }
    var longitudUsuario by remember { mutableStateOf<Double?>(null) }
    var falloUbicacion by remember { mutableStateOf(false) }
    var permisoDenegado by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf("") }
    var mostrarDialogoUbicacion by remember { mutableStateOf(false) }

    // Estado para comunidades, provincias y municipios
    var comunidades by remember { mutableStateOf(listOf<ComunidadAutonoma>()) }
    var comunidadesNombres by remember { mutableStateOf(listOf<String>()) }
    var provincias by remember { mutableStateOf(listOf<String>()) }
    var municipios by remember { mutableStateOf(listOf<String>()) }
    var comunidadSeleccionada by remember { mutableStateOf("") }
    var expandirComunidad by remember { mutableStateOf(false) }
    var provinciaSeleccionada by remember { mutableStateOf("") }
    var expandirProvincia by remember { mutableStateOf(false) }
    var municipioSeleccionado by remember { mutableStateOf("") }
    var expandirMunicipio by remember { mutableStateOf(false) }

    // Lista de lugares
    var lugares by remember { mutableStateOf(listOf<Lugar>()) }

    // Configuración del mapa
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 10f)
    }

    // Permiso de ubicación
    val estadoPermisoUbicacion = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    // Coroutine scope para manejar la carga de datos
    val coroutineScope = rememberCoroutineScope()

    // Cargar las comunidades desde el archivo JSON en assets
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val inputStream = contexto.assets.open("spain.json")
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<List<ComunidadAutonoma>>() {}.type
                comunidades = Gson().fromJson(reader, type)
                comunidadesNombres = comunidades.map { it.label }.sorted()
                reader.close()
            } catch (e: Exception) {
                errorMensaje = "Error al cargar comunidades: ${e.message}"
            }
        }
    }

    // Cargar provincias cuando se selecciona una comunidad
    LaunchedEffect(comunidadSeleccionada) {
        if (comunidadSeleccionada.isNotEmpty()) {
            val comunidadData = comunidades.find { it.label == comunidadSeleccionada }
            provincias = comunidadData?.provinces?.map { it.label }?.sorted() ?: emptyList()
            provinciaSeleccionada = ""
            municipios = emptyList()
            municipioSeleccionado = ""
        } else {
            provincias = emptyList()
            municipios = emptyList()
            provinciaSeleccionada = ""
            municipioSeleccionado = ""
        }
    }

    // Cargar municipios cuando se selecciona una provincia
    LaunchedEffect(provinciaSeleccionada) {
        if (provinciaSeleccionada.isNotEmpty()) {
            val provinciaData = comunidades.flatMap { it.provinces }.find { it.label == provinciaSeleccionada }
            municipios = provinciaData?.towns?.map { it.label }?.sorted() ?: emptyList()
            municipioSeleccionado = ""
        } else {
            municipios = emptyList()
            municipioSeleccionado = ""
        }
    }

    // Verificar si los servicios de ubicación están habilitados
    val gestorUbicacion = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gpsHabilitado = gestorUbicacion.isProviderEnabled(LocationManager.GPS_PROVIDER)
    val redHabilitada = gestorUbicacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    // Mostrar diálogo para solicitar permiso de ubicación al entrar
    LaunchedEffect(Unit) {
        if (!estadoPermisoUbicacion.status.isGranted && !estadoPermisoUbicacion.status.shouldShowRationale) {
            mostrarDialogoUbicacion = true
        }
    }

    // Manejar permisos y búsqueda de lugares después de aceptar el diálogo
    LaunchedEffect(estadoPermisoUbicacion.status) {
        if (estadoPermisoUbicacion.status.isGranted) {
            if (gpsHabilitado || redHabilitada) {
                cargando = true
                permisoDenegado = false
                falloUbicacion = false
                errorMensaje = ""

                ubicacionService.solicitarUbicacion(
                    onSuccess = { ubicacion ->
                        latitudUsuario = ubicacion.latitude
                        longitudUsuario = ubicacion.longitude
                        println("Ubicación obtenida: lat=$latitudUsuario, lon=$longitudUsuario")

                        coroutineScope.launch {
                            lugarRepository.obtenerTodosLugares { todosLugares, error ->
                                if (error != null) {
                                    errorMensaje = error
                                    cargando = false
                                    falloUbicacion = true
                                    return@obtenerTodosLugares
                                }

                                val lugaresFiltrados = todosLugares.filter { lugar ->
                                    lugar.isValid() && latitudUsuario != null && longitudUsuario != null && lugar.latitudDouble != null && lugar.longitudDouble != null && {
                                        val distancia = calcularDistancia(
                                            latitudUsuario!!,
                                            longitudUsuario!!,
                                            lugar.latitudDouble!!,
                                            lugar.longitudDouble!!
                                        )
                                        println("Distancia a ${lugar.nombre}: $distancia km (Provincia=${lugar.provincia}, Municipio=${lugar.municipio})")
                                        distancia < 100.0
                                    }()
                                }
                                lugares = lugaresFiltrados
                                cargando = false
                                if (lugaresFiltrados.isEmpty()) {
                                    errorMensaje = "No se encontraron lugares cercanos (menos de 100 km)."
                                    falloUbicacion = true
                                } else {
                                    val primerLugar = lugaresFiltrados.firstOrNull()
                                    if (primerLugar != null && primerLugar.latitudDouble != null && primerLugar.longitudDouble != null) {
                                        cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                            LatLng(primerLugar.latitudDouble!!, primerLugar.longitudDouble!!), 12f
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onFailure = {
                        errorMensaje = "Error al obtener ubicación."
                        cargando = false
                        falloUbicacion = true
                    }
                )
            } else {
                errorMensaje = "Servicios de ubicación deshabilitados."
                cargando = false
                falloUbicacion = true
            }
        } else if (!estadoPermisoUbicacion.status.isGranted && !estadoPermisoUbicacion.status.shouldShowRationale) {
            permisoDenegado = true
        }
    }

    // Estado para la barra de navegación inferior
    var selectedItem by remember { mutableStateOf("locales") }

    // Usamos Scaffold para añadir la barra de navegación inferior y el FAB
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Locales") },
                    label = { Text("Locales") },
                    selected = selectedItem == "locales",
                    onClick = {
                        selectedItem = "locales"
                        // No navegamos porque ya estamos en Ventana2
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = selectedItem == "perfil",
                    onClick = {
                        selectedItem = "perfil"
                        navController.navigate("ventanaPerfil")
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val comunidadesJson = Gson().toJson(comunidades)
                    val encodedComunidadesJson = URLEncoder.encode(comunidadesJson, "UTF-8")
                    navController.navigate("solicitarRestaurante/$encodedComunidadesJson")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Solicitar Agregar Restaurante")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                        text = "Permiso de ubicación denegado. Selecciona comunidad, provincia y municipio manualmente o revisa los permisos en Configuración.",
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
                        text = errorMensaje.ifEmpty { "No se pudo obtener la ubicación. Asegúrate de que los servicios de ubicación estén habilitados." },
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
                        onClick = { mostrarDialogoUbicacion = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Reintentar permiso")
                    }
                }

                if (latitudUsuario == null || longitudUsuario == null || falloUbicacion || lugares.isEmpty()) {
                    // Selector de comunidad autónoma
                    ExposedDropdownMenuBox(
                        expanded = expandirComunidad,
                        onExpandedChange = { expandirComunidad = !expandirComunidad }
                    ) {
                        TextField(
                            value = comunidadSeleccionada,
                            onValueChange = {},
                            label = { Text("Comunidad Autónoma") },
                            readOnly = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandirComunidad,
                            onDismissRequest = { expandirComunidad = false }
                        ) {
                            comunidadesNombres.forEach { comunidad ->
                                DropdownMenuItem(
                                    text = { Text(comunidad) },
                                    onClick = {
                                        comunidadSeleccionada = comunidad
                                        expandirComunidad = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

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
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = provincias.isNotEmpty()
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
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            enabled = municipios.isNotEmpty()
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
                            if (comunidadSeleccionada.isNotEmpty() && provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()) {
                                cargando = true
                                errorMensaje = ""
                                coroutineScope.launch {
                                    lugarRepository.obtenerLugaresPorMunicipio(provinciaSeleccionada, municipioSeleccionado) { resultado, error ->
                                        lugares = resultado.filter { it.isValid() }
                                        cargando = false
                                        falloUbicacion = false
                                        if (error != null) {
                                            errorMensaje = error
                                            return@obtenerLugaresPorMunicipio
                                        }
                                        if (resultado.isEmpty()) {
                                            errorMensaje = "No se encontraron lugares en $municipioSeleccionado, $provinciaSeleccionada."
                                        } else {
                                            val primerLugar = resultado.firstOrNull { it.isValid() }
                                            if (primerLugar != null && primerLugar.latitudDouble != null && primerLugar.longitudDouble != null) {
                                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                                    LatLng(primerLugar.latitudDouble!!, primerLugar.longitudDouble!!), 12f
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = comunidadSeleccionada.isNotEmpty() && provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()
                    ) {
                        Text("Buscar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (lugares.isNotEmpty()) {
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = latitudUsuario != null && longitudUsuario != null),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = true,
                            scrollGesturesEnabled = true,
                            zoomGesturesEnabled = true,
                            rotationGesturesEnabled = true
                        )
                    ) {
                        lugares.forEach { lugar ->
                            if (lugar.latitudDouble != null && lugar.longitudDouble != null) {
                                Marker(
                                    state = MarkerState(position = LatLng(lugar.latitudDouble!!, lugar.longitudDouble!!)),
                                    title = lugar.nombre,
                                    snippet = lugar.direccion
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(lugares) { lugar ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        val latitud = latitudUsuario?.toString() ?: "0.0"
                                        val longitud = longitudUsuario?.toString() ?: "0.0"
                                        val route = "detallesBar/${lugar.id}?latitudUsuario=$latitud&longitudUsuario=$longitud"
                                        println("Navigating to: $route with lugarId: ${lugar.id}")
                                        try {
                                            navController.navigate(route)
                                        } catch (e: Exception) {
                                            println("Navigation failed: ${e.message}")
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = lugar.nombre ?: "Sin nombre",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = lugar.direccion ?: "Sin dirección",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${lugar.municipio ?: "Sin municipio"}, ${lugar.provincia ?: "Sin provincia"}",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (!cargando && (latitudUsuario != null || (comunidadSeleccionada.isNotEmpty() && provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()))) {
                    Text(errorMensaje.ifEmpty { "No se encontraron lugares." }, fontSize = 14.sp)
                }
            }
        }
    }

    // Diálogo para solicitar permiso de ubicación
    if (mostrarDialogoUbicacion) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoUbicacion = false },
            title = { Text("Permiso de Ubicación") },
            text = { Text("Necesitamos acceder a tu ubicación para mostrarte bares y restaurantes cercanos. ¿Deseas permitirlo?") },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoUbicacion = false
                        estadoPermisoUbicacion.launchPermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        mostrarDialogoUbicacion = false
                        permisoDenegado = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rechazar")
                }
            }
        )
    }
}