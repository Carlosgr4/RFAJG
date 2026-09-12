package com.rfragancias.app.network

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * El backend usa "cookie-session" para saber si el admin inició sesión.
 * Este CookieJar guarda esa cookie en SharedPreferences para que la sesión
 * sobreviva aunque se cierre y se vuelva a abrir la app (igual que pasa
 * en un navegador normal).
 */
class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        cargarDesdeDisco()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val host = url.host
        val actuales = cookieStore.getOrPut(host) { mutableListOf() }
        for (nueva in cookies) {
            actuales.removeAll { it.name == nueva.name }
            actuales.add(nueva)
        }
        guardarEnDisco()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host] ?: return emptyList()
        val ahora = System.currentTimeMillis()
        val vigentes = cookies.filter { it.expiresAt > ahora }
        if (vigentes.size != cookies.size) {
            cookieStore[host] = vigentes.toMutableList()
            guardarEnDisco()
        }
        return vigentes
    }

    fun limpiar() {
        cookieStore.clear()
        prefs.edit().clear().apply()
    }

    private fun guardarEnDisco() {
        val editor = prefs.edit()
        editor.clear()
        for ((host, cookies) in cookieStore) {
            val serializado = cookies.joinToString(SEPARADOR_COOKIE) { it.toString() }
            editor.putString(host, serializado)
        }
        editor.apply()
    }

    private fun cargarDesdeDisco() {
        for ((host, valor) in prefs.all) {
            val texto = valor as? String ?: continue
            val cookies = texto.split(SEPARADOR_COOKIE)
                .mapNotNull { Cookie.parse(HttpUrl.Builder().scheme("https").host(host).build(), it) }
                .toMutableList()
            if (cookies.isNotEmpty()) cookieStore[host] = cookies
        }
    }

    companion object {
        private const val PREFS_NAME = "rfragancias_cookies"
        private const val SEPARADOR_COOKIE = "||"
    }
}
