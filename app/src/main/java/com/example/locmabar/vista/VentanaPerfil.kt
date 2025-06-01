package com.example.locmabar.vista

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import com.example.locmabar.modelo.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentanaPerfil(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val contexto = LocalContext.current

    // Estados para los datos del usuario
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var modoEdicion by remember { mutableStateOf(false) }
    var nombreEditado by remember { mutableStateOf("") }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    // Estado para la barra de navegación inferior
    var selectedItem by remember { mutableStateOf("perfil") }

    // Cargar datos del usuario desde Firestore
    LaunchedEffect(user) {
        if (user == null) {
            errorMensaje = "Debes iniciar sesión para ver tu perfil."
            cargando = false
            return@LaunchedEffect
        }

        scope.launch {
            try {
                val userDoc = firestore.collection("Usuarios")
                    .document(user.uid)
                    .get()
                    .await()
                usuario = userDoc.toObject(Usuario::class.java)?.copy(uid = userDoc.id)
                usuario?.let {
                    nombreEditado = it.nombre
                }
                cargando = false
            } catch (e: Exception) {
                errorMensaje = "Error al cargar tu perfil: ${e.message}"
                cargando = false
            }
        }
    }

    // Usamos Scaffold para añadir la barra de navegación inferior
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
                        navController.navigate("ventana2") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil") },
                    label = { Text("Perfil") },
                    selected = selectedItem == "perfil",
                    onClick = {
                        selectedItem = "perfil"
                        // No navegamos porque ya estamos en VentanaPerfil
                    }
                )
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
            Text(
                text = "Perfil de Usuario",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (cargando) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cargando perfil...", fontSize = 14.sp)
            } else if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = { navController.navigate("login") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Iniciar Sesión")
                }
            } else if (usuario != null) {
                // Mostrar mensaje de éxito si se actualiza el perfil
                if (mensajeExito.isNotEmpty()) {
                    Text(
                        text = mensajeExito,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                // Campo Nombre (Editable)
                OutlinedTextField(
                    value = nombreEditado,
                    onValueChange = { nombreEditado = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = modoEdicion,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Campo Correo (Solo Lectura)
                OutlinedTextField(
                    value = usuario!!.email,
                    onValueChange = { /* Solo lectura */ },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Botones de Edición
                if (modoEdicion) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                if (nombreEditado.isBlank()) {
                                    errorMensaje = "El nombre no puede estar vacío."
                                    return@Button
                                }

                                scope.launch {
                                    try {
                                        firestore.collection("Usuarios")
                                            .document(user!!.uid)
                                            .update("nombre", nombreEditado)
                                            .await()
                                        usuario = usuario!!.copy(nombre = nombreEditado)
                                        mensajeExito = "Nombre actualizado con éxito."
                                        errorMensaje = ""
                                        modoEdicion = false
                                    } catch (e: Exception) {
                                        errorMensaje = "Error al actualizar el nombre: ${e.message}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Guardar")
                        }

                        Button(
                            onClick = {
                                nombreEditado = usuario!!.nombre
                                modoEdicion = false
                                errorMensaje = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancelar")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            modoEdicion = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Editar Perfil")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Botón para Cerrar Sesión
                Button(
                    onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Cerrar Sesión")
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Botón para Donar a LocMaBar (sin funcionalidad)
                Button(
                    onClick = { /* Sin funcionalidad */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Donar a LocMaBar")
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Botón para Eliminar Cuenta (Opcional, solo para usuarios no administradores)
                if (usuario!!.rol != "admin") {
                    Button(
                        onClick = {
                            mostrarDialogoEliminar = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar Cuenta")
                    }
                }
            }
        }
    }

    // Diálogo de Confirmación para Eliminar Cuenta
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar Cuenta") },
            text = { Text("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                // Eliminar el documento del usuario en Firestore
                                firestore.collection("Usuarios")
                                    .document(user!!.uid)
                                    .delete()
                                    .await()

                                // Eliminar la cuenta del usuario en Firebase Authentication
                                user.delete().await()

                                Toast.makeText(
                                    contexto,
                                    "Cuenta eliminada con éxito.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Redirigir al login
                                navController.navigate("login") {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                errorMensaje = "Error al eliminar la cuenta: ${e.message}"
                            }
                        }
                        mostrarDialogoEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sí")
                }
            },
            dismissButton = {
                Button(
                    onClick = { mostrarDialogoEliminar = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("No")
                }
            }
        )
    }

    // Mostrar Toast para errores
    LaunchedEffect(errorMensaje) {
        if (errorMensaje.isNotEmpty()) {
            Toast.makeText(contexto, errorMensaje, Toast.LENGTH_SHORT).show()
        }
    }
}