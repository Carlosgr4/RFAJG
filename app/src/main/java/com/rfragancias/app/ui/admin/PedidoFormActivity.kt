package com.rfragancias.app.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rfragancias.app.R
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError
import com.rfragancias.app.utils.validarObligatorios
import com.rfragancias.app.utils.validarNumeros
import com.rfragancias.app.utils.limpiarErrorAlEscribir
import com.rfragancias.app.utils.mostrarErrorCampo

class PedidoFormActivity : AppCompatActivity() {

    private val estados = listOf("Pendiente", "En camino", "Recibido")

    private lateinit var editInsumo: TextInputEditText
    private lateinit var editProveedor: TextInputEditText
    private lateinit var editCantidad: TextInputEditText
    private lateinit var editCosto: TextInputEditText
    private lateinit var spinnerEstado: Spinner
    private lateinit var progress: View
    private lateinit var btnGuardar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pedido_form)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        editInsumo = findViewById(R.id.edit_insumo)
        editProveedor = findViewById(R.id.edit_proveedor)
        editCantidad = findViewById(R.id.edit_cantidad_gramos)
        editCosto = findViewById(R.id.edit_costo)
        listOf(editInsumo, editCantidad, editCosto).forEach { it.limpiarErrorAlEscribir() }
        spinnerEstado = findViewById(R.id.spinner_estado)
        progress = findViewById(R.id.progress_form)
        btnGuardar = findViewById(R.id.btn_guardar)

        val adapterEstado = ArrayAdapter(this, R.layout.spinner_item, estados)
        adapterEstado.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerEstado.adapter = adapterEstado

        btnGuardar.setOnClickListener { guardar() }
    }

    private fun guardar() {
        val insumo = editInsumo.text?.toString()?.trim().orEmpty()
        val costo = editCosto.text?.toString()?.trim().orEmpty()

        if (!validarObligatorios(editInsumo)) return
        if (!validarNumeros(editCantidad, editCosto)) return

        val datos = mapOf(
            "insumo" to insumo,
            "proveedor" to editProveedor.text?.toString()?.trim(),
            "cantidad_gramos" to (editCantidad.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0),
            "costo" to (costo.toDoubleOrNull() ?: 0.0),
            "estado" to estados[spinnerEstado.selectedItemPosition]
        )

        progress.visibility = View.VISIBLE
        btnGuardar.isEnabled = false

        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.crearPedido(datos)
                if (respuesta.isSuccessful) {
                    finish()
                } else {
                    mostrarToast(getString(R.string.error_generico))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            } finally {
                progress.visibility = View.GONE
                btnGuardar.isEnabled = true
            }
        }
    }
}
