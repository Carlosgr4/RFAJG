package com.rfragancias.app.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rfragancias.app.R
import com.rfragancias.app.model.Abono
import com.rfragancias.app.model.Venta
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.comoMoneda
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError
import com.rfragancias.app.utils.validarObligatorios
import com.rfragancias.app.utils.validarNumeros
import com.rfragancias.app.utils.limpiarErrorAlEscribir
import com.rfragancias.app.utils.mostrarErrorCampo

class ClienteDetalleActivity : AppCompatActivity() {

    private var clienteId: Int = -1
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var contenedorVentas: android.widget.LinearLayout
    private lateinit var contenedorAbonos: android.widget.LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente_detalle)

        clienteId = intent.getIntExtra(EXTRA_CLIENTE_ID, -1)
        if (clienteId == -1) {
            finish()
            return
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = getString(R.string.titulo_cliente_detalle)

        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { cargarHistorial() }
        // Alternativa con botón al gesto de deslizar para actualizar
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            inflateMenu(R.menu.menu_lista)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.accion_actualizar) { cargarHistorial(); true } else false
            }
        }

        contenedorVentas = findViewById(R.id.contenedor_ventas)
        contenedorAbonos = findViewById(R.id.contenedor_abonos)

        findViewById<MaterialButton>(R.id.btn_registrar_abono).setOnClickListener {
            mostrarDialogoAbono()
        }

        cargarHistorial()
    }

    private fun cargarHistorial() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getHistorialCliente(clienteId)
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val historial = respuesta.body()!!
                    val cliente = historial.cliente

                    findViewById<TextView>(R.id.texto_nombre).text = cliente.nombre
                    findViewById<TextView>(R.id.texto_contacto).text =
                        listOfNotNull(cliente.telefono, cliente.email).joinToString(" · ").ifBlank { "Sin datos de contacto" }
                    findViewById<TextView>(R.id.texto_deuda).text = cliente.deuda.comoMoneda()
                    findViewById<TextView>(R.id.texto_compras).text = "${cliente.cantidadCompras}"

                    pintarVentas(historial.ventas)
                    pintarAbonos(historial.abonos)
                } else {
                    mostrarToast(getString(R.string.error_generico))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun pintarVentas(ventas: List<Venta>) {
        contenedorVentas.removeAllViews()
        if (ventas.isEmpty()) {
            contenedorVentas.addView(crearTextoVacio())
            return
        }
        ventas.forEach { venta ->
            val fila = LayoutInflater.from(this).inflate(R.layout.item_historial_fila, contenedorVentas, false)
            fila.findViewById<TextView>(R.id.texto_titulo_fila).text = venta.perfumeNombre ?: "Venta"
            fila.findViewById<TextView>(R.id.texto_subtitulo_fila).text = venta.fecha ?: ""
            fila.findViewById<TextView>(R.id.texto_valor_fila).text = venta.total.comoMoneda()
            contenedorVentas.addView(fila)
        }
    }

    private fun pintarAbonos(abonos: List<Abono>) {
        contenedorAbonos.removeAllViews()
        if (abonos.isEmpty()) {
            contenedorAbonos.addView(crearTextoVacio())
            return
        }
        abonos.forEach { abono ->
            val fila = LayoutInflater.from(this).inflate(R.layout.item_historial_fila, contenedorAbonos, false)
            fila.findViewById<TextView>(R.id.texto_titulo_fila).text = abono.nota?.ifBlank { "Abono" } ?: "Abono"
            fila.findViewById<TextView>(R.id.texto_subtitulo_fila).text = abono.fecha ?: ""
            fila.findViewById<TextView>(R.id.texto_valor_fila).text = abono.monto.comoMoneda()
            contenedorAbonos.addView(fila)
        }
    }

    private fun crearTextoVacio(): TextView {
        return TextView(this).apply {
            text = getString(R.string.sin_datos)
            setTextColor(getColor(R.color.text_muted))
            textSize = 13f
        }
    }

    private fun mostrarDialogoAbono() {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_abono_form, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()

        val editMonto = vista.findViewById<TextInputEditText>(R.id.edit_monto)
        val editNota = vista.findViewById<TextInputEditText>(R.id.edit_nota)
        editMonto.limpiarErrorAlEscribir()

        vista.findViewById<View>(R.id.btn_cancelar).setOnClickListener { dialog.dismiss() }
        vista.findViewById<View>(R.id.btn_guardar).setOnClickListener {
            val monto = editMonto.text?.toString()?.trim().orEmpty()
            if (!validarObligatorios(editMonto)) return@setOnClickListener
            if (!validarNumeros(editMonto)) return@setOnClickListener
            val datos = mapOf(
                "monto" to monto,
                "nota" to (editNota.text?.toString()?.trim().orEmpty())
            )
            lifecycleScope.launch {
                try {
                    val respuesta = ApiClient.api.registrarAbono(clienteId, datos)
                    if (respuesta.isSuccessful) {
                        dialog.dismiss()
                        cargarHistorial()
                    } else {
                        mostrarToast(getString(R.string.error_generico))
                    }
                } catch (e: Exception) {
                    mostrarToast(mensajeDeError(e))
                }
            }
        }
        dialog.show()
    }

    companion object {
        const val EXTRA_CLIENTE_ID = "extra_cliente_id"
    }
}
