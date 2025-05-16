package com.example.locmabar.vista

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.locmabar.modelo.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Registro(navController: NavController) {
    // Instancia de Firebase Authentication y Firestore
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    // Estados para los campos de entrada
    val nombre = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val contrasena = remember { mutableStateOf("") }
    val confContrasena = remember { mutableStateOf("") }
    val errorMensaje = remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "REGISTRARSE",
                color = MaterialTheme.colorScheme.onBackground,
                style = TextStyle(fontSize = 25.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "NOMBRE",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = nombre.value,
                    onValueChange = { nombre.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "CORREO ELECTRÓNICO",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = email.value,
                    onValueChange = { email.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "CONTRASEÑA",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = contrasena.value,
                    onValueChange = { contrasena.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Text(
                    text = "CONFIRMAR CONTRASEÑA",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = confContrasena.value,
                    onValueChange = { confContrasena.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            if (cargando) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (nombre.value.isBlank()) {
                            errorMensaje.value = "El nombre no puede estar vacío"
                            return@Button
                        }

                        if (contrasena.value != confContrasena.value) {
                            errorMensaje.value = "Las contraseñas no coinciden"
                            return@Button
                        }

                        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$")
                        if (!emailRegex.matches(email.value)) {
                            errorMensaje.value = "El correo no es válido"
                            return@Button
                        }
                        if (contrasena.value.length < 6) {
                            errorMensaje.value = "La contraseña debe tener al menos 6 caracteres"
                            return@Button
                        }

                        cargando = true
                        scope.launch {
                            try {
                                // Registrar usuario en Firebase Authentication
                                val result = auth.createUserWithEmailAndPassword(email.value, contrasena.value)
                                    .await()
                                val user = result.user
                                if (user != null) {
                                    // Crear documento en la colección "Usuarios"
                                    val usuario = Usuario(
                                        id = user.uid,
                                        nombre = nombre.value,
                                        email = email.value,
                                        rol = "usuario" // Rol por defecto para usuarios normales
                                    )
                                    firestore.collection("Usuarios")
                                        .document(user.uid)
                                        .set(usuario)
                                        .await()
                                    navController.navigate("login")
                                } else {
                                    errorMensaje.value = "Error al registrar usuario: usuario nulo"
                                }
                            } catch (e: Exception) {
                                errorMensaje.value = "Error de registro: ${e.localizedMessage ?: "Desconocido"}"
                            } finally {
                                cargando = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(50.dp)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Aceptar",
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        val contexto = LocalContext.current
        LaunchedEffect(errorMensaje.value) {
            errorMensaje.value?.let { message ->
                Toast.makeText(
                    contexto,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
                errorMensaje.value = null
            }
        }
    }
}