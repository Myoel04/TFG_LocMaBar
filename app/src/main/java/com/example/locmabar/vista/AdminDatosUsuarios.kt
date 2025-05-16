package com.example.locmabar.vista

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun AdminDatosUsuarios(
    navController: NavHostController,
    usuarioId: String
) {
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Cargar datos del usuario desde Firebase
    LaunchedEffect(usuarioId) {
        scope.launch {
            try {
                val documentSnapshot = FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .document(usuarioId)
                    .get()
                    .await()
                usuario = documentSnapshot.toObject(Usuario::class.java)?.copy(id = documentSnapshot.id)
            } catch (e: Exception) {
                errorMensaje = "Error al cargar usuario: ${e.message}"
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
            text = "Detalles del Usuario",
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
                    text = "Usuario eliminado con éxito.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else if (usuario != null) {
                OutlinedTextField(
                    value = usuario!!.nombre,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = usuario!!.email,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = usuario!!.rol,
                    onValueChange = { /* Read-only */ },
                    label = { Text("Rol") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Botones "Eliminar Usuario" y "Cancelar"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                                    if (usuario!!.id == currentUserUid) {
                                        errorMensaje = "No puedes eliminarte a ti mismo."
                                        return@launch
                                    }
                                    FirebaseFirestore.getInstance()
                                        .collection("Usuarios")
                                        .document(usuario!!.id)
                                        .delete()
                                        .await()
                                    mensajeExito = true
                                    navController.popBackStack()
                                } catch (e: Exception) {
                                    errorMensaje = "Error al eliminar usuario: ${e.message}"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Eliminar Usuario")
                    }

                    Button(
                        onClick = {
                            // Regresar a AdminUsuarios sin hacer cambios
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Cancelar")
                    }
                }
            } else {
                Text(
                    text = "Usuario no encontrado.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}