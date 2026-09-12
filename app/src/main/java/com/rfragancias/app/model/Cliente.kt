package com.rfragancias.app.model

import com.google.gson.annotations.SerializedName

data class Cliente(
    val id: Int = 0,
    val nombre: String = "",
    val telefono: String? = null,
    val email: String? = null,
    val notas: String? = null,
    @SerializedName("creado_en") val creadoEn: String? = null,
    @SerializedName("totalVentas") val totalVentas: Double = 0.0,
    @SerializedName("totalAbonos") val totalAbonos: Double = 0.0,
    val deuda: Double = 0.0,
    @SerializedName("ultimoAbono") val ultimoAbono: String? = null,
    @SerializedName("ultimoPerfume") val ultimoPerfume: String? = null,
    @SerializedName("fechaUltimaVenta") val fechaUltimaVenta: String? = null,
    @SerializedName("cantidadCompras") val cantidadCompras: Int = 0
)

data class Abono(
    val id: Int = 0,
    @SerializedName("cliente_id") val clienteId: Int = 0,
    val monto: Double = 0.0,
    val fecha: String? = null,
    val nota: String? = null
)

data class HistorialCliente(
    val cliente: Cliente,
    val ventas: List<Venta> = emptyList(),
    val abonos: List<Abono> = emptyList()
)
