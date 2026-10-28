package com.rfragancias.app.ui.catalogo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.rfragancias.app.R
import com.rfragancias.app.model.CarritoManager
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.utils.comoMoneda
import com.rfragancias.app.utils.mostrarToast
import com.rfragancias.app.utils.urlImagenCompleta
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

class PerfumeDetalleActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfume_detalle)

        sessionManager = SessionManager(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val id = intent.getIntExtra(EXTRA_PERFUME_ID, -1)
        if (id == -1) {
            finish()
            return
        }

        cargarPerfume(id)
    }

    private fun cargarPerfume(id: Int) {
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getPerfume(id)
                if (!respuesta.isSuccessful || respuesta.body() == null) {
                    mostrarToast(getString(R.string.error_generico))
                    finish()
                    return@launch
                }
                val perfume = respuesta.body()!!

                findViewById<TextView>(R.id.texto_disenador_detalle).text = perfume.disenador ?: ""
                findViewById<TextView>(R.id.texto_nombre_detalle).text = perfume.nombre
                findViewById<TextView>(R.id.badge_genero_detalle).text = perfume.genero
                findViewById<TextView>(R.id.texto_notas_detalle).text =
                    perfume.notas ?: "Sin información de notas olfativas."

                Glide.with(this@PerfumeDetalleActivity)
                    .load(urlImagenCompleta(perfume.imagenUrl))
                    .placeholder(R.drawable.ic_perfume_placeholder)
                    .error(R.drawable.ic_perfume_placeholder)
                    .fitCenter()
                    .into(findViewById<ImageView>(R.id.img_detalle))

                val btnAgregarCarrito = findViewById<MaterialButton>(R.id.btn_agregar_carrito_detalle)
                if (sessionManager.esCliente) {
                    btnAgregarCarrito.visibility = android.view.View.VISIBLE
                    btnAgregarCarrito.setOnClickListener {
                        CarritoManager.agregar(perfume)
                        mostrarToast(getString(R.string.perfume_agregado_carrito))
                    }
                } else {
                    btnAgregarCarrito.visibility = android.view.View.GONE
                }

                findViewById<MaterialButton>(R.id.btn_whatsapp_detalle).setOnClickListener {
                    val numero = getString(R.string.whatsapp_numero)
                    val mensaje = Uri.encode("Hola, me interesa el perfume ${perfume.nombre} (${perfume.precio.comoMoneda()})")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=$mensaje")))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_PERFUME_ID = "extra_perfume_id"
    }
}
