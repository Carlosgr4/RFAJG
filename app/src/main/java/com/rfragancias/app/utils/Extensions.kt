package com.rfragancias.app.utils

import android.content.Context
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.textfield.TextInputLayout
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.rfragancias.app.BuildConfig
import java.text.NumberFormat
import java.util.Locale

/**
 * El backend guarda las imágenes como rutas relativas (ej: "/uploads/foto.jpg").
 * Esta función arma la URL completa para que Glide pueda descargarlas.
 */
fun urlImagenCompleta(ruta: String?): String? {
    if (ruta.isNullOrBlank()) return null
    // Fotos guardadas dentro de Firestore ("data:image/jpeg;base64,...")
    if (ruta.startsWith("data:")) return ruta
    if (ruta.startsWith("http://") || ruta.startsWith("https://")) return ruta
    val base = BuildConfig.BASE_URL.trimEnd('/')
    val relativa = if (ruta.startsWith("/")) ruta else "/$ruta"
    return base + relativa
}

/** Formatea un número como moneda en pesos colombianos, ej: $ 120.000 */
fun Double.comoMoneda(): String {
    val formato = NumberFormat.getNumberInstance(Locale("es", "CO"))
    formato.maximumFractionDigits = 0
    return "$ ${formato.format(this)}"
}

fun Context.mostrarToast(mensaje: String) {
    Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
}

/**
 * Traduce un error de Firebase (o de red) a un mensaje claro en español,
 * para que el usuario sepa qué pasó en lugar de un genérico "sin conexión".
 */
fun Context.mensajeDeError(e: Throwable): String {
    android.util.Log.e("RFragancias", "Error en la operación", e)
    val firestore = e as? com.google.firebase.firestore.FirebaseFirestoreException
    return when {
        firestore?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "No tienes permiso para hacer esto. Cierra sesión y vuelve a entrar con la cuenta de administrador (usuario: admin)."
        firestore?.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE ||
            e is com.google.firebase.FirebaseNetworkException ||
            e is java.io.IOException ->
            "Sin conexión a internet. Revisa tu conexión e intenta de nuevo."
        e is IllegalStateException && e.message?.contains("FirebaseApp") == true ->
            "Firebase no está configurado en esta app (falta google-services.json)."
        else -> "No se pudo completar la operación: ${e.message ?: e.javaClass.simpleName}"
    }
}

fun View.mostrarSnackbar(mensaje: String) {
    Snackbar.make(this, mensaje, Snackbar.LENGTH_LONG).show()
}

/* ------------------------------------------------------------------
 * Validación de formularios con el error junto al campo (auditoría #10)
 * ------------------------------------------------------------------ */

/** El TextInputLayout que envuelve a este campo, si lo hay. */
private fun EditText.contenedorCampo(): TextInputLayout? {
    var padre = parent
    while (padre != null) {
        if (padre is TextInputLayout) return padre
        padre = padre.parent
    }
    return null
}

/** Muestra (o quita, con null) el mensaje de error debajo del campo. */
fun EditText.mostrarErrorCampo(mensaje: String?) {
    val contenedor = contenedorCampo()
    if (contenedor != null) {
        contenedor.error = mensaje
        contenedor.isErrorEnabled = mensaje != null
    } else {
        error = mensaje
    }
}

/** Borra el error del campo en cuanto el usuario vuelve a escribir. */
fun EditText.limpiarErrorAlEscribir() {
    doAfterTextChanged { mostrarErrorCampo(null) }
}

/**
 * Revisa que los campos obligatorios tengan texto. Marca cada campo vacío
 * con su error y lleva el foco al primero. Devuelve true si todo está bien.
 */
fun validarObligatorios(vararg campos: EditText): Boolean {
    var primeroConError: EditText? = null
    for (campo in campos) {
        val vacio = campo.text?.toString()?.trim().isNullOrEmpty()
        campo.mostrarErrorCampo(if (vacio) campo.context.getString(com.rfragancias.app.R.string.error_campo_obligatorio) else null)
        if (vacio && primeroConError == null) primeroConError = campo
    }
    primeroConError?.requestFocus()
    return primeroConError == null
}

/** Revisa que los campos (si tienen texto) sean números válidos. */
fun validarNumeros(vararg campos: EditText): Boolean {
    var primeroConError: EditText? = null
    for (campo in campos) {
        val texto = campo.text?.toString()?.trim().orEmpty()
        if (texto.isNotEmpty() && texto.replace(',', '.').toDoubleOrNull() == null) {
            campo.mostrarErrorCampo(campo.context.getString(com.rfragancias.app.R.string.error_numero_invalido))
            if (primeroConError == null) primeroConError = campo
        }
    }
    primeroConError?.requestFocus()
    return primeroConError == null
}
