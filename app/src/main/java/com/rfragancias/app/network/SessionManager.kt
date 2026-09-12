package com.rfragancias.app.network

import android.content.Context
import android.content.SharedPreferences
import com.rfragancias.app.model.ROL_ADMIN
import com.rfragancias.app.model.ROL_CLIENTE

/**
 * Guarda localmente si hay una sesión activa, de qué rol (admin o
 * cliente) y el nombre para mostrar, para no tener que consultar el
 * servidor cada vez que se abre la app. La sesión "real" (la que
 * autoriza las peticiones) vive en la cookie que administra
 * [PersistentCookieJar]; esto es solo una bandera local para decidir qué
 * pantalla mostrar primero.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var rol: String?
        get() = prefs.getString(KEY_ROL, null)
        set(value) = prefs.edit().putString(KEY_ROL, value).apply()

    var nombre: String?
        get() = prefs.getString(KEY_NOMBRE, null)
        set(value) = prefs.edit().putString(KEY_NOMBRE, value).apply()

    var clienteId: Int
        get() = prefs.getInt(KEY_CLIENTE_ID, -1)
        set(value) = prefs.edit().putInt(KEY_CLIENTE_ID, value).apply()

    val estaLogueado: Boolean
        get() = rol == ROL_ADMIN || rol == ROL_CLIENTE

    val esAdmin: Boolean
        get() = rol == ROL_ADMIN

    val esCliente: Boolean
        get() = rol == ROL_CLIENTE

    fun guardarSesion(rol: String, nombre: String?, clienteId: Int?) {
        this.rol = rol
        this.nombre = nombre
        this.clienteId = clienteId ?: -1
    }

    fun limpiar() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "rfragancias_session_prefs"
        private const val KEY_ROL = "rol"
        private const val KEY_NOMBRE = "nombre"
        private const val KEY_CLIENTE_ID = "cliente_id"
    }
}
