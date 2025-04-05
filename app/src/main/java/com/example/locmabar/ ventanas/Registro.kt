package com.example.locmabar.ventanas

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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Registro(navController: NavController) {
    // Instancia de Firebase Authentication
    val auth = FirebaseAuth.getInstance()

    // Estados para los campos de entrada
    val email = remember { mutableStateOf("") }
    val contrasena = remember { mutableStateOf("") }
    val confContrasena = remember { mutableStateOf("") }
    val errorMensaje = remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
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
                color = Color.Black,
                style = TextStyle(fontSize = 25.sp),
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "CORREO ELECTRÓNICO",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = email.value,
                    onValueChange = { email.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = "CONTRASEÑA",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = contrasena.value,
                    onValueChange = { contrasena.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Text(
                    text = "CONFIRMAR CONTRASEÑA",
                    color = Color.Black,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                TextField(
                    value = confContrasena.value,
                    onValueChange = { confContrasena.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Button(
                onClick = {
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

                    auth.createUserWithEmailAndPassword(email.value, contrasena.value)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                navController.navigate("Login")
                            } else {
                                errorMensaje.value =
                                    "Error de registro: ${task.exception?.localizedMessage ?: "Desconocido"}"
                            }
                        }
                },
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAB9)),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Aceptar",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge
                )
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