package com.example.locmabar.vista

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
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
                println("Error al cargar comunidades desde assets: ${e.message}")
            }
        }
    }

    // Cargar provincias cuando se selecciona una comunidad
    LaunchedEffect(comunidadSeleccionada) {
        if (comunidadSeleccionada.isNotEmpty()) {
            val comunidadData = comunidades.find { it.label == comunidadSeleccionada }
            provincias = comunidadData?.provinces?.map { it.label }?.sorted() ?: emptyList()
            provinciaSeleccionada = "" // Reiniciar la provincia seleccionada
            municipios = emptyList() // Reiniciar los municipios
            municipioSeleccionado = "" // Reiniciar el municipio seleccionado
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
            municipioSeleccionado = "" // Reiniciar el municipio seleccionado
        } else {
            municipios = emptyList()
            municipioSeleccionado = ""
        }
    }

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

                ubicacionService.solicitarUbicacion(
                    onSuccess = { ubicacion ->
                        latitudUsuario = ubicacion.latitude
                        longitudUsuario = ubicacion.longitude
                        println("Ubicación obtenida: lat=$latitudUsuario, lon=$longitudUsuario")

                        lugarRepository.obtenerTodosLugares { todosLugares ->
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
                            lugarRepository.obtenerLugaresPorMunicipio(provinciaSeleccionada, municipioSeleccionado) { resultado ->
                                lugares = resultado
                                cargando = false
                                falloUbicacion = false
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
            } else if (!cargando && (latitudUsuario != null || (comunidadSeleccionada.isNotEmpty() && provinciaSeleccionada.isNotEmpty() && municipioSeleccionado.isNotEmpty()))) {
                Text("No se encontraron lugares.", fontSize = 14.sp)
            }
        }
    }
}