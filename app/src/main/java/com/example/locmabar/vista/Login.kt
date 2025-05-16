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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Login(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    // Estados para los campos de entrada
    val email = remember { mutableStateOf("") }
    val contrasena = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }
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
                text = "¡Bienvenido!",
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
            }

            if (cargando) {
                CircularProgressIndicator()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            if (email.value.isEmpty() || contrasena.value.isEmpty()) {
                                errorMessage.value = "Por favor ingrese correo y contraseña"
                                return@Button
                            }
                            cargando = true
                            scope.launch {
                                try {
                                    auth.signInWithEmailAndPassword(email.value, contrasena.value)
                                        .await()
                                    navController.navigate("ventana2")
                                } catch (e: Exception) {
                                    errorMessage.value = "Error: ${e.localizedMessage ?: "Desconocido"}"
                                } finally {
                                    cargando = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Entrar",
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Button(
                        onClick = { navController.navigate("registro") },
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Registrarse",
                            color = MaterialTheme.colorScheme.onSecondary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Mostrar mensaje de error con Toast
        val contexto = LocalContext.current
        LaunchedEffect(errorMessage.value) {
            errorMessage.value?.let { message ->
                Toast.makeText(
                    contexto,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
                errorMessage.value = null
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 802, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    Login(navController = rememberNavController())
}