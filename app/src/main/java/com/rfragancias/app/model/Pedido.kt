package com.rfragancias.app.model

import com.google.gson.annotations.SerializedName

data class Pedido(
    val id: Int = 0,
    val insumo: String = "",
    val proveedor: String? = null,
    @SerializedName("cantidad_gramos") val cantidadGramos: Double = 0.0,
    val costo: Double = 0.0,
    val estado: String = "Pendiente",
    val fecha: String? = null
)
