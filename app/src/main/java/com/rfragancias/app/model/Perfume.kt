package com.rfragancias.app.model

import com.google.gson.annotations.SerializedName

data class Perfume(
    val id: Int = 0,
    val nombre: String = "",
    val disenador: String? = null,
    val genero: String = "Unisex",
    val notas: String? = null,
    val precio: Double = 0.0,
    @SerializedName("gramos_disponibles") val gramosDisponibles: Double = 0.0,
    val activo: Boolean = true,
    @SerializedName("imagenUrl") val imagenUrl: String? = null,
    @SerializedName("porcentajeStock") val porcentajeStock: Int = 0,
    @SerializedName("nivelStock") val nivelStock: String = "alto",
    @SerializedName("creado_en") val creadoEn: String? = null
)
