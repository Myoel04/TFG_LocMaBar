package com.example.locmabar.vista

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Barra superior
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(90.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )

            // Texto "LOCMABAR" en la barra superior
            Text(
                text = "LOCMABAR",
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 1.43.em,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 33.dp)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 90.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                    // Desplegables reorganizados: Comunidad y Provincia en una fila, Municipio y botón de búsqueda debajo
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        // Fila para Comunidad Autónoma y Provincia
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selector de comunidad autónoma
                            ExposedDropdownMenuBox(
                                expanded = expandirComunidad,
                                onExpandedChange = { expandirComunidad = !expandirComunidad },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                TextField(
                                    value = comunidadSeleccionada,
                                    onValueChange = {},
                                    label = { Text("Comunidad Autónoma", color = MaterialTheme.colorScheme.onSurface) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
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

                            // Selector de provincia
                            ExposedDropdownMenuBox(
                                expanded = expandirProvincia,
                                onExpandedChange = { expandirProvincia = !expandirProvincia },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                TextField(
                                    value = provinciaSeleccionada,
                                    onValueChange = {},
                                    label = { Text("Provincia", color = MaterialTheme.colorScheme.onSurface) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface),
                                    enabled = provincias.isNotEmpty(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
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
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Fila para Municipio y botón de búsqueda
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Selector de municipio
                            ExposedDropdownMenuBox(
                                expanded = expandirMunicipio,
                                onExpandedChange = { expandirMunicipio = !expandirMunicipio },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                TextField(
                                    value = municipioSeleccionado,
                                    onValueChange = {},
                                    label = { Text("Municipio", color = MaterialTheme.colorScheme.onSurface) },
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface),
                                    enabled = municipios.isNotEmpty(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface
                                    )
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

                            // Botón de búsqueda con icono de lupa
                            IconButton(
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
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = comunidadSeleccionada.isNotEmpty() && provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty(),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = if (comunidadSeleccionada.isNotEmpty() && provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    }
                                )
                            }
                        }
                    }

                    if (lugares.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f) // Para que ocupe el espacio disponible
                        ) {
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
                    } else if (!cargando) {
                        Text(errorMensaje.ifEmpty { "No se encontraron lugares." }, fontSize = 14.sp)
                    }
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