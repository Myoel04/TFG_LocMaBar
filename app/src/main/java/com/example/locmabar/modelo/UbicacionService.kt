package com.example.locmabar.modelo

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class UbicacionService(private val contexto: Context) {
    private val clienteUbicacionFusionada: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(contexto)

    fun solicitarUbicacion(onSuccess: (Location) -> Unit, onFailure: () -> Unit) {
        try {
            val solicitudUbicacion = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 10000
                fastestInterval = 5000
                numUpdates = 1
            }

            clienteUbicacionFusionada.lastLocation.addOnSuccessListener { ubicacion ->
                if (ubicacion != null) {
                    onSuccess(ubicacion)
                } else {
                    clienteUbicacionFusionada.requestLocationUpdates(
                        solicitudUbicacion,
                        object : LocationCallback() {
                            override fun onLocationResult(resultadoUbicacion: LocationResult) {
                                val nuevaUbicacion = resultadoUbicacion.lastLocation
                                if (nuevaUbicacion != null) {
                                    onSuccess(nuevaUbicacion)
                                    clienteUbicacionFusionada.removeLocationUpdates(this)
                                } else {
                                    onFailure()
                                }
                            }
                        },
                        Looper.getMainLooper()
                    )
                }
            }.addOnFailureListener {
                onFailure()
            }
        } catch (e: SecurityException) {
            onFailure()
        }
    }
}