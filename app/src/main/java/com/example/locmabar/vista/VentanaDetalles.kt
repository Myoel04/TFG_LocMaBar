package com.example.locmabar.vista

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Lugar
import com.example.locmabar.utils.calcularDistancia

@Composable
fun VentanaDetalles(
    navController: NavHostController,
    lugar: Lugar,
    latitudUsuario: Double?,
    longitudUsuario: Double?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título: Nombre del bar
        Text(
            text = lugar.nombre,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Dirección
        Text(
            text = "Dirección: ${lugar.direccion}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Distancia (si se tienen las coordenadas del usuario)
        if (latitudUsuario != null && longitudUsuario != null) {
            val distancia = calcularDistancia(latitudUsuario, longitudUsuario, lugar.latitud, lugar.longitud)
            Text(
                text = "Distancia: %.2f km".format(distancia),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = "Distancia: No disponible",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Teléfono (si está disponible)
        Text(
            text = "Teléfono: ${lugar.telefono ?: "No disponible"}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Horario (si está disponible)
        Text(
            text = "Horario: ${lugar.horario ?: "No disponible"}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Valoración (si está disponible)
        Text(
            text = "Valoración: ${lugar.valoracion ?: "No disponible"}",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Botón para abrir en Google Maps
        Button(
            onClick = {
                val uri = Uri.parse("geo:${lugar.latitud},${lugar.longitud}?q=${lugar.latitud},${lugar.longitud}(${lugar.nombre})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                navController.context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Abrir en Google Maps")
        }

        // Botón para volver atrás
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Volver")
        }
    }
}