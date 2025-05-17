package com.example.locmabar.vista

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Lugar
import com.example.locmabar.modelo.LugarRepository
import com.example.locmabar.modelo.SolicitudRestaurante
import com.example.locmabar.modelo.Usuario
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminSolicitudes(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var selectedTab by remember { mutableStateOf(0) }
    var isAdmin by remember { mutableStateOf(false) }
    var cargandoAdmin by remember { mutableStateOf(true) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }

    // Estado para permisos de ubicación
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) println("Permiso de ubicación concedido") }

    // Estados para datos
    var usuarios by remember { mutableStateOf(listOf<Usuario>()) }
    var locales by remember { mutableStateOf(listOf<Lugar>()) }
    var solicitudes by remember { mutableStateOf(listOf<SolicitudRestaurante>()) }
    var filtroLocal by remember { mutableStateOf("") }

    // Configuración del mapa
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 10f) // Centro en Madrid por defecto
    }

    // Coroutine scope para manejar la carga de datos
    val coroutineScope = rememberCoroutineScope()

    // Verificar si el usuario es administrador y cargar datos
    LaunchedEffect(user) {
        if (user == null) {
            errorMensaje = "Debes iniciar sesión como administrador."
            cargandoAdmin = false
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
                // Verificar si el usuario es administrador
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .document(user.uid)
                    .get()
                    .await()
                isAdmin = userDoc.getString("rol") == "admin"
                if (!isAdmin) {
                    errorMensaje = "Acceso denegado. Solo para administradores."
                    cargandoAdmin = false
                    return@launch
                }

                // Cargar usuarios
                val usuariosSnapshot = FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .get()
                    .await()
                usuarios = usuariosSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)?.copy(uid = doc.id)
                }

                // Cargar locales
                val lugarRepository = LugarRepository()
                lugarRepository.obtenerTodosLugares { lugares, error ->
                    if (error != null) {
                        errorMensaje = error
                    } else {
                        locales = lugares.filter { it.isValid() }
                        if (lugares.isNotEmpty()) {
                            val primerLugar = lugares.firstOrNull { it.isValid() }
                            if (primerLugar != null && primerLugar.latitud != null && primerLugar.longitud != null) {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(primerLugar.latitudDouble!!, primerLugar.longitudDouble!!), 12f
                                )
                            }
                        }
                    }
                }

                // Cargar solicitudes
                val solicitudesSnapshot = FirebaseFirestore.getInstance()
                    .collection("Solicitudes")
                    .whereEqualTo("estado", "PENDIENTE")
                    .get()
                    .await()
                solicitudes = solicitudesSnapshot.documents.mapNotNull { document ->
                    try {
                        val solicitud = document.toObject(SolicitudRestaurante::class.java)
                        if (solicitud != null && solicitud.isValid()) {
                            println("Solicitud cargada: ID=${solicitud.id}, Nombre=${solicitud.nombre}")
                            solicitud
                        } else {
                            println("No se pudo convertir el documento ${document.id} a SolicitudRestaurante: objeto no válido")
                            null
                        }
                    } catch (e: Exception) {
                        println("Error al convertir el documento ${document.id} a SolicitudRestaurante: ${e.message}")
                        null
                    }
                }

                cargando = false
                cargandoAdmin = false
            } catch (e: Exception) {
                errorMensaje = "Error al cargar datos: ${e.message}"
                cargando = false
                cargandoAdmin = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cargandoAdmin) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Verificando permisos...", fontSize = 14.sp)
            return@Column
        }

        if (user == null || !isAdmin) {
            Text(
                text = "Acceso denegado. Solo para administradores.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Iniciar Sesión")
            }
            return@Column
        }

        Text(
            text = "Panel de Administración",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando datos...", fontSize = 14.sp)
        } else {
            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Usuarios") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Locales") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Comentarios") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Solicitudes") }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Usuarios
                    Button(
                        onClick = { navController.navigate("adminUsuarios") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gestionar Usuarios")
                    }
                }
                1 -> {
                    // Locales
                    if (!locationPermissionState.status.isGranted) {
                        Button(
                            onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Solicitar permiso de ubicación")
                        }
                    }
                    OutlinedTextField(
                        value = filtroLocal,
                        onValueChange = { filtroLocal = it },
                        label = { Text("Filtrar por nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Mapa de Google
                    GoogleMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(isMyLocationEnabled = locationPermissionState.status.isGranted),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = true,
                            scrollGesturesEnabled = true,
                            zoomGesturesEnabled = true,
                            rotationGesturesEnabled = true
                        )
                    ) {
                        locales.filter { it.nombre?.contains(filtroLocal, ignoreCase = true) == true }.forEach { lugar ->
                            if (lugar.latitud != null && lugar.longitud != null) {
                                Marker(
                                    state = MarkerState(position = LatLng(lugar.latitudDouble!!, lugar.longitudDouble!!)),
                                    title = lugar.nombre,
                                    snippet = lugar.direccion,
                                    onClick = {
                                        val route = "detallesBar/${lugar.id}?latitudUsuario=0.0&longitudUsuario=0.0"
                                        navController.navigate(route)
                                        true
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(locales.filter { it.nombre?.contains(filtroLocal, ignoreCase = true) == true }) { lugar ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        val route = "detallesBar/${lugar.id}?latitudUsuario=0.0&longitudUsuario=0.0"
                                        navController.navigate(route)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = lugar.nombre ?: "Sin nombre",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Dirección: ${lugar.direccion ?: "Sin dirección"}",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Comentarios
                    Button(
                        onClick = { navController.navigate("adminComentarios") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gestionar Comentarios")
                    }
                }
                3 -> {
                    // Solicitudes
                    if (solicitudes.isEmpty()) {
                        Text(
                            text = "No hay solicitudes pendientes.",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(solicitudes) { solicitud ->
                                Card(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("adminDatosSolicitudes/${solicitud.id}")
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = solicitud.nombre ?: "Sin nombre",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Dirección: ${solicitud.direccion ?: "Sin dirección"}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${solicitud.municipio ?: "Sin municipio"}, ${solicitud.provincia ?: "Sin provincia"}",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}