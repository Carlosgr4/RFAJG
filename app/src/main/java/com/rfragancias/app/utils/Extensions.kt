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
    Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
}

fun View.mostrarSnackbar(mensaje: String) {
    Snackbar.make(this, mensaje, Snackbar.LENGTH_LONG).show()
}
