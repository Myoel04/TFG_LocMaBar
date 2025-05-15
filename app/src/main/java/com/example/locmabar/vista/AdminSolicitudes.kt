package com.example.locmabar.vista

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminSolicitudes(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var selectedTab by remember { mutableStateOf(0) }
    var isAdmin by remember { mutableStateOf(false) }
    var cargandoAdmin by remember { mutableStateOf(true) }

    // Estado para permisos de ubicación
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) println("Permiso de ubicación concedido") }

    // Estados para datos
    var usuarios by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var locales by remember { mutableStateOf(listOf<Lugar>()) }
    var comentariosPendientes by remember { mutableStateOf(listOf<Map<String, Any>>()) }
    var solicitudes by remember { mutableStateOf(listOf<SolicitudRestaurante>()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var filtroLocal by remember { mutableStateOf("") }

    // Configuración del mapa
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(40.4168, -3.7038), 10f) // Centro en Madrid por defecto
    }

    // Verificar si el usuario es administrador
    LaunchedEffect(user) {
        if (user == null) {
            errorMensaje = "Debes iniciar sesión como administrador."
            cargandoAdmin = false
            return@LaunchedEffect
        }

        val userDoc = FirebaseFirestore.getInstance()
            .collection("Usuarios")
            .document(user.uid)
            .get()
            .await()
        isAdmin = userDoc.getString("rol") == "admin"
        cargandoAdmin = false

        if (isAdmin) {
            // Usuarios
            FirebaseFirestore.getInstance().collection("Usuarios")
                .get()
                .addOnSuccessListener { result ->
                    usuarios = result.documents.mapNotNull { it.data }
                }
                .addOnFailureListener { e ->
                    errorMensaje = "Error al cargar usuarios: ${e.message}"
                }

            // Locales
            val lugarRepository = LugarRepository()
            lugarRepository.obtenerTodosLugares { lugares, error ->
                locales = lugares
                cargando = false
                // Actualizar la posición del mapa si hay lugares
                if (lugares.isNotEmpty()) {
                    val primerLugar = lugares.first()
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(primerLugar.latitud, primerLugar.longitud), 12f
                    )
                }
            }

            // Comentarios pendientes
            FirebaseFirestore.getInstance().collection("ComentariosPendientes")
                .whereEqualTo("estado", "PENDIENTE")
                .get()
                .addOnSuccessListener { result ->
                    comentariosPendientes = result.documents.mapNotNull { it.data }
                }
                .addOnFailureListener { e ->
                    errorMensaje = "Error al cargar comentarios: ${e.message}"
                }

            // Solicitudes
            FirebaseFirestore.getInstance().collection("Solicitudes")
                .whereEqualTo("estado", "PENDIENTE")
                .get()
                .addOnSuccessListener { result ->
                    solicitudes = result.documents.mapNotNull { it.toObject<SolicitudRestaurante>() }
                }
                .addOnFailureListener { e ->
                    errorMensaje = "Error al cargar solicitudes: ${e.message}"
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
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(usuarios) { usuario ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Nombre: ${usuario["nombre"] ?: "Sin nombre"}",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Email: ${usuario["email"] ?: "Sin email"}",
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Rol: ${usuario["rol"] ?: "Sin rol"}",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val userUid = usuario["uid"] as String
                                            if (userUid != FirebaseAuth.getInstance().currentUser?.uid) {
                                                FirebaseFirestore.getInstance()
                                                    .collection("Usuarios")
                                                    .document(userUid)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        usuarios = usuarios.filter { it["uid"] != userUid }
                                                    }
                                            } else {
                                                errorMensaje = "No puedes eliminarte a ti mismo."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Eliminar")
                                    }
                                }
                            }
                        }
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
                        cameraPositionState = cameraPositionState
                    ) {
                        locales.filter { it.nombre.contains(filtroLocal, ignoreCase = true) }.forEach { lugar ->
                            Marker(
                                state = MarkerState(position = LatLng(lugar.latitud, lugar.longitud)),
                                title = lugar.nombre,
                                snippet = lugar.direccion
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(locales.filter { it.nombre.contains(filtroLocal, ignoreCase = true) }) { lugar ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = lugar.nombre,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Dirección: ${lugar.direccion}",
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Comentarios
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(comentariosPendientes) { comentario ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Comentario: ${comentario["texto"] ?: "Sin texto"}",
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Usuario: ${comentario["usuarioId"] ?: "Desconocido"}",
                                        fontSize = 14.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(
                                            onClick = {
                                                FirebaseFirestore.getInstance()
                                                    .collection("Comentarios")
                                                    .add(comentario)
                                                    .addOnSuccessListener {
                                                        FirebaseFirestore.getInstance()
                                                            .collection("ComentariosPendientes")
                                                            .document(comentario["id"] as String)
                                                            .delete()
                                                            .addOnSuccessListener {
                                                                comentariosPendientes = comentariosPendientes.filter { it["id"] != comentario["id"] }
                                                            }
                                                    }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Aceptar")
                                        }
                                        Button(
                                            onClick = {
                                                FirebaseFirestore.getInstance()
                                                    .collection("ComentariosPendientes")
                                                    .document(comentario["id"] as String)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        comentariosPendientes = comentariosPendientes.filter { it["id"] != comentario["id"] }
                                                    }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Rechazar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // Solicitudes
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(solicitudes) { solicitud ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = solicitud.nombre,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Dirección: ${solicitud.direccion}", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${solicitud.municipio}, ${solicitud.provincia}",
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Latitud: ${solicitud.latitud}, Longitud: ${solicitud.longitud}",
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    solicitud.telefono?.let { Text("Teléfono: $it", fontSize = 12.sp) }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    solicitud.horario?.let { Text("Horario: $it", fontSize = 12.sp) }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    solicitud.valoracion?.let { Text("Valoración: $it", fontSize = 12.sp) }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Button(
                                            onClick = {
                                                val lugar = Lugar(
                                                    id = solicitud.id,
                                                    nombre = solicitud.nombre,
                                                    direccion = solicitud.direccion,
                                                    provincia = solicitud.provincia,
                                                    municipio = solicitud.municipio,
                                                    latitud = solicitud.latitud,
                                                    longitud = solicitud.longitud,
                                                    telefono = solicitud.telefono,
                                                    horario = solicitud.horario,
                                                    valoracion = solicitud.valoracion
                                                )
                                                LugarRepository().agregarLugar(lugar) { success ->
                                                    if (success) {
                                                        FirebaseFirestore.getInstance()
                                                            .collection("Solicitudes")
                                                            .document(solicitud.id)
                                                            .delete()
                                                            .addOnSuccessListener {
                                                                solicitudes = solicitudes.filter { it.id != solicitud.id }
                                                            }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Aprobar")
                                        }
                                        Button(
                                            onClick = {
                                                FirebaseFirestore.getInstance()
                                                    .collection("Solicitudes")
                                                    .document(solicitud.id)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        solicitudes = solicitudes.filter { it.id != solicitud.id }
                                                    }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Rechazar")
                                        }
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