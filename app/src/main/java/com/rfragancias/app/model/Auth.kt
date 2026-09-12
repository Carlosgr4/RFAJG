package com.rfragancias.app.model

import com.google.gson.annotations.SerializedName

/**
 * Un solo formulario de login sirve tanto para clientes como para el
 * administrador. "usuario" acepta el correo del cliente o el usuario del
 * administrador; el backend decide el rol.
 */
data class LoginRequest(val usuario: String, val clave: String)

/**
 * Datos que pide el registro de un cliente nuevo.
 */
data class RegistroRequest(
    val nombre: String,
    val telefono: String,
    val email: String,
    val password: String
)

/**
 * Respuesta del backend al iniciar sesión o registrarse. "rol" es
 * "admin" o "cliente"; el cliente Android usa ese valor para decidir qué
 * pantallas mostrar (catálogo+carrito vs. panel de administración).
 */
data class AuthResponse(
    val ok: Boolean = false,
    val rol: String? = null,
    val nombre: String? = null,
    @SerializedName("clienteId") val clienteId: Int? = null,
    val error: String? = null
)

data class SessionResponse(
    val autenticado: Boolean = false,
    val rol: String? = null,
    val nombre: String? = null,
    @SerializedName("clienteId") val clienteId: Int? = null
)

const val ROL_ADMIN = "admin"
const val ROL_CLIENTE = "cliente"
