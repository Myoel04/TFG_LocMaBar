package com.example.locmabar.vista

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Lugar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLugares(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var isAdmin by remember { mutableStateOf(false) }
    var cargandoAdmin by remember { mutableStateOf(true) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var lugares by remember { mutableStateOf(listOf<Lugar>()) }
    var lugarSeleccionado by remember { mutableStateOf<Lugar?>(null) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarDialogoEditar by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val contexto = LocalContext.current

    // Verificar si el usuario es administrador
    LaunchedEffect(user) {
        if (user == null) {
            errorMensaje = "Debes iniciar sesión como administrador."
            cargandoAdmin = false
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
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

                // Cargar lugares aprobados
                val lugaresSnapshot = FirebaseFirestore.getInstance()
                    .collection("Locales")
                    .get()
                    .await()
                lugares = lugaresSnapshot.documents.mapNotNull { doc ->
                    val lugar = doc.toObject(Lugar::class.java)
                    lugar?.copy(id = doc.id)
                }.filter { it.isValid() }

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
            text = "Gestión de Lugares Aprobados",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando lugares...", fontSize = 14.sp)
        } else {
            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (lugares.isEmpty()) {
                Text(
                    text = "No hay lugares aprobados.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(lugares) { lugar ->
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = lugar.nombre ?: "Sin nombre",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dirección: ${lugar.direccion ?: "Sin dirección"}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${lugar.municipio ?: "Sin municipio"}, ${lugar.provincia ?: "Sin provincia"}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            lugarSeleccionado = lugar
                                            mostrarDialogoEditar = true
                                        },
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Text("Editar")
                                    }
                                    Button(
                                        onClick = {
                                            lugarSeleccionado = lugar
                                            mostrarDialogoEliminar = true
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Eliminar")
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

    // Diálogo para confirmar eliminación
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar Lugar") },
            text = { Text("¿Estás seguro de que deseas eliminar este lugar? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("Locales")
                                    .document(lugarSeleccionado!!.id!!)
                                    .delete()
                                    .await()
                                lugares = lugares.filter { it.id != lugarSeleccionado!!.id }
                                Toast.makeText(
                                    contexto,
                                    "Lugar eliminado con éxito.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                lugarSeleccionado = null
                            } catch (e: Exception) {
                                errorMensaje = "Error al eliminar el lugar: ${e.message}"
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

    // Diálogo para editar un lugar
    if (mostrarDialogoEditar) {
        var nombre by remember { mutableStateOf(lugarSeleccionado!!.nombre ?: "") }
        var direccion by remember { mutableStateOf(lugarSeleccionado!!.direccion ?: "") }
        var provincia by remember { mutableStateOf(lugarSeleccionado!!.provincia ?: "") }
        var municipio by remember { mutableStateOf(lugarSeleccionado!!.municipio ?: "") }
        var latitud by remember { mutableStateOf(lugarSeleccionado!!.latitud ?: "") }
        var longitud by remember { mutableStateOf(lugarSeleccionado!!.longitud ?: "") }
        var telefono by remember { mutableStateOf(lugarSeleccionado!!.telefono ?: "") }
        var horario by remember { mutableStateOf(lugarSeleccionado!!.horario ?: "") }
        var valoracion by remember { mutableStateOf(lugarSeleccionado!!.valoracion ?: "") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoEditar = false },
            title = { Text("Editar Lugar") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = direccion,
                        onValueChange = { direccion = it },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = provincia,
                        onValueChange = { provincia = it },
                        label = { Text("Provincia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = municipio,
                        onValueChange = { municipio = it },
                        label = { Text("Municipio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = latitud,
                        onValueChange = { latitud = it },
                        label = { Text("Latitud") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = longitud,
                        onValueChange = { longitud = it },
                        label = { Text("Longitud") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it },
                        label = { Text("Teléfono (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = horario,
                        onValueChange = { horario = it },
                        label = { Text("Horario (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = valoracion,
                        onValueChange = { valoracion = it },
                        label = { Text("Valoración (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val lugarActualizado = Lugar(
                                    id = lugarSeleccionado!!.id,
                                    nombre = nombre,
                                    direccion = direccion,
                                    provincia = provincia,
                                    municipio = municipio,
                                    latitud = latitud,
                                    longitud = longitud,
                                    telefono = if (telefono.isBlank()) null else telefono,
                                    horario = if (horario.isBlank()) null else horario,
                                    valoracion = if (valoracion.isBlank()) null else valoracion
                                )
                                if (!lugarActualizado.isValid()) {
                                    Toast.makeText(
                                        contexto,
                                        "Por favor, completa todos los campos requeridos.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }
                                FirebaseFirestore.getInstance()
                                    .collection("Locales")
                                    .document(lugarSeleccionado!!.id!!)
                                    .set(lugarActualizado)
                                    .await()
                                lugares = lugares.map { if (it.id == lugarSeleccionado!!.id) lugarActualizado else it }
                                Toast.makeText(
                                    contexto,
                                    "Lugar actualizado con éxito.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                lugarSeleccionado = null
                                mostrarDialogoEditar = false
                            } catch (e: Exception) {
                                errorMensaje = "Error al actualizar el lugar: ${e.message}"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(
                    onClick = { mostrarDialogoEditar = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}