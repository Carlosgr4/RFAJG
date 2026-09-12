package com.rfragancias.app.model

import com.google.gson.annotations.SerializedName

data class Venta(
    val id: Int = 0,
    @SerializedName("cliente_id") val clienteId: Int? = null,
    @SerializedName("perfume_id") val perfumeId: Int? = null,
    @SerializedName("cliente_nombre") val clienteNombre: String? = null,
    @SerializedName("perfume_nombre") val perfumeNombre: String? = null,
    @SerializedName("cantidad_ml") val cantidadMl: Double = 0.0,
    val total: Double = 0.0,
    val estado: String = "Pendiente",
    val fecha: String? = null,
    val notas: String? = null
)
