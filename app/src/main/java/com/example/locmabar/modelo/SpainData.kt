package com.example.locmabar.modelo

data class ComunidadAutonoma(
    val parent_code: String,
    val label: String,
    val code: String,
    val provinces: List<Provincia>
)

data class Provincia(
    val parent_code: String,
    val code: String,
    val label: String,
    val towns: List<Municipio>
)

data class Municipio(
    val parent_code: String,
    val code: String,
    val label: String
)