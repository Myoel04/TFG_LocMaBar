package com.example.locmabar.vista

import android.widget.Toast
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
import com.example.locmabar.modelo.Comentario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminComentariosAprobados(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var comentarios by remember { mutableStateOf<List<Comentario>>(emptyList()) }
    var usuariosMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var comentarioSeleccionado by remember { mutableStateOf<Comentario?>(null) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    val contexto = LocalContext.current

    // Cargar lista de comentarios aprobados desde Firebase
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val querySnapshot = FirebaseFirestore.getInstance()
                    .collection("Comentarios")
                    .whereEqualTo("estado", "APROBADO")
                    .get()
                    .await()
                comentarios = querySnapshot.toObjects(Comentario::class.java)

                // Cargar nombres de usuarios para los comentarios
                val usuarioIds = comentarios.map { it.usuarioId }.distinct()
                val usuarios = mutableMapOf<String, String>()
                for (usuarioId in usuarioIds) {
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("Usuarios")
                            .document(usuarioId)
                            .get()
                            .await()
                        val nombre = userDoc.get("nombre") as String?
                        usuarios[usuarioId] = nombre ?: "Usuario Desconocido"
                    } catch (e: Exception) {
                        usuarios[usuarioId] = "Usuario Desconocido"
                    }
                }
                usuariosMap = usuarios
                cargando = false
            } catch (e: Exception) {
                errorMensaje = "Error al cargar comentarios aprobados: ${e.message}"
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Comentarios Aprobados",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando comentarios aprobados...", fontSize = 14.sp)
        } else if (errorMensaje.isNotEmpty()) {
            Text(
                text = errorMensaje,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else if (comentarios.isEmpty()) {
            Text(
                text = "No hay comentarios aprobados.",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            LazyColumn {
                items(comentarios) { comentario ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = comentario.texto,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Usuario: ${usuariosMap[comentario.usuarioId] ?: "Usuario Desconocido"}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Fecha: ${
                                    comentario.fechaCreacion?.toDate()?.let {
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                                    } ?: "Desconocida"
                                }",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            comentario.valoracion?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Valoración: $it",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    comentarioSeleccionado = comentario
                                    mostrarDialogoEliminar = true
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

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }

    // Diálogo para confirmar eliminación
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar Comentario") },
            text = { Text("¿Estás seguro de que deseas eliminar este comentario? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("Comentarios")
                                    .document(comentarioSeleccionado!!.id)
                                    .delete()
                                    .await()
                                comentarios = comentarios.filter { it.id != comentarioSeleccionado!!.id }
                                Toast.makeText(
                                    contexto,
                                    "Comentario eliminado con éxito.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                comentarioSeleccionado = null
                            } catch (e: Exception) {
                                errorMensaje = "Error al eliminar el comentario: ${e.message}"
                            }
                        }
                        mostrarDialogoEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { mostrarDialogoEliminar = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}