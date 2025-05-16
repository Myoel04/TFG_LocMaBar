package com.example.locmabar.vista

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Comentario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDatosComentarios(
    navController: NavHostController,
    comentarioId: String
) {
    var comentario by remember { mutableStateOf<Comentario?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Cargar datos del comentario desde Firebase
    LaunchedEffect(comentarioId) {
        scope.launch {
            try {
                val documentSnapshot = FirebaseFirestore.getInstance()
                    .collection("ComentariosPendientes")
                    .document(comentarioId)
                    .get()
                    .await()
                comentario = documentSnapshot.toObject(Comentario::class.java)
                estado = comentario?.estado ?: "PENDIENTE"
            } catch (e: Exception) {
                errorMensaje = "Error al cargar comentario: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Detalles del Comentario",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando detalles...", fontSize = 14.sp)
        } else {
            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else if (mensajeExito) {
                Text(
                    text = "Comentario actualizado con éxito.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else if (comentario != null) {
                OutlinedTextField(
                    value = comentario!!.texto,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Texto del Comentario") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comentario!!.usuarioId,
                    onValueChange = { /* Read-only */ },
                    label = { Text("ID del Usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comentario!!.lugarId,
                    onValueChange = { /* Read-only */ },
                    label = { Text("ID del Lugar") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comentario!!.fechaCreacion?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                    } ?: "",
                    onValueChange = { /* Read-only */ },
                    label = { Text("Fecha de Creación") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = comentario!!.valoracion ?: "",
                    onValueChange = { /* Read-only */ },
                    label = { Text("Valoración (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown para el estado
                var expandirEstado by remember { mutableStateOf(false) }
                val estados = listOf("PENDIENTE", "APROBADO", "RECHAZADO")
                ExposedDropdownMenuBox(
                    expanded = expandirEstado,
                    onExpandedChange = { expandirEstado = !expandirEstado }
                ) {
                    TextField(
                        value = estado,
                        onValueChange = {},
                        label = { Text("Estado") },
                        readOnly = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandirEstado,
                        onDismissRequest = { expandirEstado = false }
                    ) {
                        estados.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    estado = opcion
                                    expandirEstado = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    FirebaseFirestore.getInstance()
                                        .collection("ComentariosPendientes")
                                        .document(comentarioId)
                                        .update("estado", "APROBADO")
                                        .await()
                                    // Mover a colección pública
                                    val comentarioActualizado = comentario!!.copy(estado = "APROBADO")
                                    FirebaseFirestore.getInstance()
                                        .collection("Comentarios")
                                        .document(comentarioId)
                                        .set(comentarioActualizado)
                                        .await()
                                    // Eliminar de pendientes
                                    FirebaseFirestore.getInstance()
                                        .collection("ComentariosPendientes")
                                        .document(comentarioId)
                                        .delete()
                                        .await()
                                    mensajeExito = true
                                } catch (e: Exception) {
                                    errorMensaje = "Error al aprobar comentario: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Aprobar")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    FirebaseFirestore.getInstance()
                                        .collection("ComentariosPendientes")
                                        .document(comentarioId)
                                        .update("estado", "RECHAZADO")
                                        .await()
                                    // Eliminar de pendientes
                                    FirebaseFirestore.getInstance()
                                        .collection("ComentariosPendientes")
                                        .document(comentarioId)
                                        .delete()
                                        .await()
                                    mensajeExito = true
                                } catch (e: Exception) {
                                    errorMensaje = "Error al rechazar comentario: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Rechazar")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver")
                }
            } else {
                Text(
                    text = "Comentario no encontrado.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}