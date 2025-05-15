package com.example.locmabar.vista

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.R
import com.example.locmabar.modelo.ComunidadAutonoma
import com.example.locmabar.modelo.GeocodingResponse
import com.example.locmabar.modelo.RetrofitClient
import com.example.locmabar.modelo.SolicitudRestaurante
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
    var latitud by remember { mutableStateOf<Double?>(null) }
    var longitud by remember { mutableStateOf<Double?>(null) }
    var telefono by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var valoracion by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf(false) }
    var errorMensaje by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var cargandoGeocodificacion by remember { mutableStateOf(false) }

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

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del Restaurante") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

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

            Button(
                onClick = {
                    if (direccion.isBlank() || provinciaSeleccionada.isBlank() || municipioSeleccionado.isBlank()) {
                        errorMensaje = "Por favor, completa la dirección, provincia y municipio."
                        return@Button
                    }

                    cargandoGeocodificacion = true
                    errorMensaje = ""

                    val direccionCompleta = "$direccion, $municipioSeleccionado, $provinciaSeleccionada, Spain"
                    RetrofitClient.geocodingService.getLatLngFromAddress(
                        direccionCompleta,
                        contexto.getString(R.string.google_maps_key)
                    ).enqueue(object : Callback<GeocodingResponse> {
                        override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                            cargandoGeocodificacion = false
                            if (response.isSuccessful) {
                                val geocodingResponse = response.body()
                                if (geocodingResponse?.status == "OK" && geocodingResponse.results.isNotEmpty()) {
                                    val location = geocodingResponse.results[0].geometry.location
                                    latitud = location.lat
                                    longitud = location.lng
                                } else {
                                    errorMensaje = "No se encontraron coordenadas para la dirección: $direccionCompleta. Intenta con una dirección más específica."
                                }
                            } else {
                                errorMensaje = "Error al conectar con la API de Geocodificación: ${response.message()}"
                            }
                        }

                        override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                            cargandoGeocodificacion = false
                            errorMensaje = "Error al obtener coordenadas: ${t.message}"
                        }
                    })
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = direccion.isNotBlank() && provinciaSeleccionada.isNotBlank() && municipioSeleccionado.isNotBlank()
            ) {
                if (cargandoGeocodificacion) {
                    Text("Obteniendo coordenadas...")
                } else {
                    Text("Obtener Coordenadas")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (latitud != null && longitud != null) {
                Text(
                    text = "Coordenadas obtenidas: Latitud: $latitud, Longitud: $longitud",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = horario,
                onValueChange = { horario = it },
                label = { Text("Horario (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = valoracion,
                onValueChange = { valoracion = it },
                label = { Text("Valoración (opcional, ej. 4.5/5)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (user == null) {
                        errorMensaje = "Debes iniciar sesión para enviar una solicitud."
                        return@Button
                    }

                    if (nombre.isNotBlank() && direccion.isNotBlank() && provinciaSeleccionada.isNotBlank() &&
                        municipioSeleccionado.isNotBlank() && latitud != null && longitud != null
                    ) {
                        cargando = true
                        errorMensaje = ""

                        val solicitudId = UUID.randomUUID().toString()
                        val solicitud = SolicitudRestaurante(
                            id = solicitudId,
                            nombre = nombre,
                            direccion = direccion,
                            provincia = provinciaSeleccionada,
                            municipio = municipioSeleccionado,
                            latitud = latitud!!,
                            longitud = longitud!!,
                            telefono = telefono.ifBlank { null },
                            horario = horario.ifBlank { null },
                            valoracion = valoracion.ifBlank { null },
                            estado = "PENDIENTE"
                        )

                        FirebaseFirestore.getInstance()
                            .collection("Solicitudes")
                            .document(solicitudId)
                            .set(solicitud)
                            .addOnSuccessListener {
                                mensajeExito = true
                                cargando = false
                                nombre = ""
                                direccion = ""
                                provinciaSeleccionada = ""
                                municipioSeleccionado = ""
                                latitud = null
                                longitud = null
                                telefono = ""
                                horario = ""
                                valoracion = ""
                            }
                            .addOnFailureListener { e ->
                                errorMensaje = "Error al guardar solicitud: ${e.message}"
                                cargando = false
                            }
                    } else {
                        errorMensaje = "Por favor, completa todos los campos obligatorios y obtén las coordenadas."
                    }
                },
                enabled = nombre.isNotBlank() && direccion.isNotBlank() && provinciaSeleccionada.isNotBlank() &&
                        municipioSeleccionado.isNotBlank() && latitud != null && longitud != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar Solicitud")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver")
            }
        }
    }
}