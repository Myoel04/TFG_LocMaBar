package com.example.locmabar.vista

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.ComunidadAutonoma
import com.example.locmabar.modelo.SolicitudRestaurante
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudNuevo(
    navController: NavHostController,
    comunidadesJson: String
) {
    val contexto = LocalContext.current
    val comunidades = Gson().fromJson(comunidadesJson, Array<ComunidadAutonoma>::class.java).toList()
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // Estados para los campos del formulario
    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var provinciaSeleccionada by remember { mutableStateOf("") }
    var expandirProvincia by remember { mutableStateOf(false) }
    var municipioSeleccionado by remember { mutableStateOf("") }
    var expandirMunicipio by remember { mutableStateOf(false) }
    var latitud by remember { mutableStateOf("") }
    var longitud by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var valoracion by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    // Listas para los desplegables
    val provincias = comunidades.flatMap { it.provinces }.map { it.label }.distinct().sorted()
    var municipios by remember { mutableStateOf(listOf<String>()) }

    // Cargar municipios cuando se selecciona una provincia
    LaunchedEffect(provinciaSeleccionada) {
        if (provinciaSeleccionada.isNotEmpty()) {
            val provinciaData = comunidades.flatMap { it.provinces }.find { it.label == provinciaSeleccionada }
            municipios = provinciaData?.towns?.map { it.label }?.sorted() ?: emptyList()
            municipioSeleccionado = ""
        } else {
            municipios = emptyList()
            municipioSeleccionado = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user == null) {
            Text(
                text = "Debes iniciar sesión para enviar una solicitud.",
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
            text = "Solicitar Agregar Restaurante",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Subiendo solicitud...", fontSize = 14.sp)
        } else {
            if (mensajeExito) {
                Text(
                    text = "Solicitud enviada con éxito. Será revisada por un administrador.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Campo para el nombre
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Restaurante") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para la dirección
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Selector de provincia
            ExposedDropdownMenuBox(
                expanded = expandirProvincia,
                onExpandedChange = { expandirProvincia = !expandirProvincia }
            ) {
                TextField(
                    value = provinciaSeleccionada,
                    onValueChange = {},
                    label = { Text("Provincia") },
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandirProvincia,
                    onDismissRequest = { expandirProvincia = false }
                ) {
                    provincias.forEach { provincia ->
                        DropdownMenuItem(
                            text = { Text(provincia) },
                            onClick = {
                                provinciaSeleccionada = provincia
                                expandirProvincia = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Selector de municipio
            ExposedDropdownMenuBox(
                expanded = expandirMunicipio,
                onExpandedChange = { expandirMunicipio = !expandirMunicipio }
            ) {
                TextField(
                    value = municipioSeleccionado,
                    onValueChange = {},
                    label = { Text("Municipio") },
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    enabled = municipios.isNotEmpty()
                )
                ExposedDropdownMenu(
                    expanded = expandirMunicipio,
                    onDismissRequest = { expandirMunicipio = false }
                ) {
                    municipios.forEach { municipio ->
                        DropdownMenuItem(
                            text = { Text(municipio) },
                            onClick = {
                                municipioSeleccionado = municipio
                                expandirMunicipio = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para la latitud
            OutlinedTextField(
                value = latitud,
                onValueChange = { latitud = it },
                label = { Text("Latitud") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para la longitud
            OutlinedTextField(
                value = longitud,
                onValueChange = { longitud = it },
                label = { Text("Longitud") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para el teléfono (opcional)
            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para el horario (opcional)
            OutlinedTextField(
                value = horario,
                onValueChange = { horario = it },
                label = { Text("Horario (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Campo para la valoración (opcional)
            OutlinedTextField(
                value = valoracion,
                onValueChange = { valoracion = it },
                label = { Text("Valoración (opcional, ej. 4.5/5)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Botón para enviar la solicitud
            Button(
                onClick = {
                    if (user == null) {
                        errorMensaje = "Debes iniciar sesión para enviar una solicitud."
                        return@Button
                    }

                    // Validar campos obligatorios
                    if (nombre.isNotBlank() && direccion.isNotBlank() && provinciaSeleccionada.isNotBlank() &&
                        municipioSeleccionado.isNotBlank() && latitud.isNotBlank() && longitud.isNotBlank()
                    ) {
                        try {
                            val latitudDouble = latitud.toDouble()
                            val longitudDouble = longitud.toDouble()

                            // Validar rangos de latitud y longitud
                            if (latitudDouble !in -90.0..90.0 || longitudDouble !in -180.0..180.0) {
                                errorMensaje = "Latitud debe estar entre -90 y 90, y longitud entre -180 y 180."
                                return@Button
                            }

                            cargando = true
                            errorMensaje = ""

                            // Crear la solicitud
                            val solicitudId = UUID.randomUUID().toString()
                            val solicitud = SolicitudRestaurante(
                                id = solicitudId,
                                nombre = nombre,
                                direccion = direccion,
                                provincia = provinciaSeleccionada,
                                municipio = municipioSeleccionado,
                                latitud = latitudDouble,
                                longitud = longitudDouble,
                                telefono = telefono.ifBlank { null },
                                horario = horario.ifBlank { null },
                                valoracion = valoracion.ifBlank { null },
                                estado = "PENDIENTE"
                            )

                            // Guardar solicitud en Firestore en la colección "Solicitudes"
                            FirebaseFirestore.getInstance()
                                .collection("Solicitudes")
                                .document(solicitudId)
                                .set(solicitud)
                                .addOnSuccessListener {
                                    mensajeExito = true
                                    cargando = false
                                    // Limpiar el formulario
                                    nombre = ""
                                    direccion = ""
                                    provinciaSeleccionada = ""
                                    municipioSeleccionado = ""
                                    latitud = ""
                                    longitud = ""
                                    telefono = ""
                                    horario = ""
                                    valoracion = ""
                                }
                                .addOnFailureListener { e ->
                                    errorMensaje = "Error al guardar solicitud: ${e.message}"
                                    cargando = false
                                }
                        } catch (e: NumberFormatException) {
                            errorMensaje = "Latitud y longitud deben ser números válidos."
                            cargando = false
                        }
                    }
                },
                enabled = nombre.isNotBlank() && direccion.isNotBlank() && provinciaSeleccionada.isNotBlank() &&
                        municipioSeleccionado.isNotBlank() && latitud.isNotBlank() && longitud.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar Solicitud")
            }

            Spacer(modifier = Modifier.height(8.dp))

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