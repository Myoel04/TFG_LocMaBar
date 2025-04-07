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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Login(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val email = remember { mutableStateOf("") }
    val contrasena = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly // Cambiado a SpaceEvenly para mejor distribución
        ) {
            Text(
                text = "¡Bienvenido!",
                color = Color.Black,
                style = TextStyle(fontSize = 25.sp),
                modifier = Modifier.padding(vertical = 9.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.Start
            ) {
                Text("USUARIO", color = Color.Black, style = TextStyle(fontSize = 12.sp))
                TextField(
                    value = email.value,
                    onValueChange = { email.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )

                Text("CONTRASEÑA", color = Color.Black, style = TextStyle(fontSize = 12.sp))
                TextField(
                    value = contrasena.value,
                    onValueChange = { contrasena.value = it },
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color(0xFFF5F5F5)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        if (email.value.isEmpty() || contrasena.value.isEmpty()) {
                            errorMessage.value = "Por favor ingrese usuario y contraseña"
                            return@Button
                        }
                        auth.signInWithEmailAndPassword(email.value, contrasena.value)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    navController.navigate("ventana2")
                                } else {
                                    errorMessage.value = "Error: ${task.exception?.message}"
                                }
                            }
                    },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAB9)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Entrar", color = Color.Black, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = { navController.navigate("registro") },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFDAB9)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Registrarse", color = Color.Black, textAlign = TextAlign.Center, style = MaterialTheme.typography.labelLarge)
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