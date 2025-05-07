package com.example.locmabar.vista

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
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminSolicitudes(navController: NavHostController) {
    val lugarRepository = LugarRepository()
    var solicitudes by remember { mutableStateOf(listOf<SolicitudRestaurante>()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }

    // Cargar solicitudes desde Firestore
    LaunchedEffect(Unit) {
        FirebaseFirestore.getInstance()
            .collection("Solicitudes")
            .whereEqualTo("estado", "PENDIENTE")
            .get()
            .addOnSuccessListener { result ->
                solicitudes = result.documents.mapNotNull { doc ->
                    doc.toObject(SolicitudRestaurante::class.java)
                }
                cargando = false
            }
            .addOnFailureListener { e ->
                errorMensaje = "Error al cargar solicitudes: ${e.message}"
                cargando = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Solicitudes Pendientes",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando solicitudes...", fontSize = 14.sp)
        } else {
            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

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
                                            // Aprobar solicitud: mover a la colección "12345" y eliminar de "Solicitudes"
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
                                            lugarRepository.agregarLugar(lugar) { success ->
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
                                            // Rechazar solicitud: eliminar de "Solicitudes"
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

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para volver atrás
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }
}