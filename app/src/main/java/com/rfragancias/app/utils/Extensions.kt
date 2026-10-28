package com.rfragancias.app.utils

import android.content.Context
import android.view.View
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
