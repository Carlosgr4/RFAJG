package com.rfragancias.app.model

data class DashboardResumen(
    val totalPerfumes: Int = 0,
    val totalClientes: Int = 0,
    val ventasMes: Double = 0.0,
    val deudaTotal: Double = 0.0,
    val pedidosPendientes: Int = 0,
    val stockBajo: Int = 0
)

data class OkResponse(val ok: Boolean = false)
data class ErrorResponse(val error: String? = null)
