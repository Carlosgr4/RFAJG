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
import com.rfragancias.app.model.Cliente
import com.rfragancias.app.model.Perfume
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

class VentaFormActivity : AppCompatActivity() {

    private val estados = listOf("Pendiente", "Pagada", "Anulada")
    private var clientes: List<Cliente> = emptyList()
    private var perfumes: List<Perfume> = emptyList()

    private lateinit var spinnerCliente: Spinner
    private lateinit var spinnerPerfume: Spinner
    private lateinit var spinnerEstado: Spinner
    private lateinit var editCantidadMl: TextInputEditText
    private lateinit var editTotal: TextInputEditText
    private lateinit var editNotas: TextInputEditText
    private lateinit var progress: View
    private lateinit var btnGuardar: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_venta_form)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        spinnerCliente = findViewById(R.id.spinner_cliente)
        spinnerPerfume = findViewById(R.id.spinner_perfume)
        spinnerEstado = findViewById(R.id.spinner_estado)
        editCantidadMl = findViewById(R.id.edit_cantidad_ml)
        editTotal = findViewById(R.id.edit_total)
        editNotas = findViewById(R.id.edit_notas)
        progress = findViewById(R.id.progress_form)
        btnGuardar = findViewById(R.id.btn_guardar)

        val adapterEstado = ArrayAdapter(this, R.layout.spinner_item, estados)
        adapterEstado.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerEstado.adapter = adapterEstado

        cargarListas()

        btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarListas() {
        lifecycleScope.launch {
            try {
                val respClientes = ApiClient.api.getClientes()
                clientes = if (respClientes.isSuccessful) respClientes.body().orEmpty() else emptyList()
                val nombresClientes = listOf("Cliente ocasional") + clientes.map { it.nombre }
                val adapterCliente = ArrayAdapter(this@VentaFormActivity, R.layout.spinner_item, nombresClientes)
                adapterCliente.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerCliente.adapter = adapterCliente

                val respPerfumes = ApiClient.api.getPerfumesAdmin()
                perfumes = if (respPerfumes.isSuccessful) respPerfumes.body().orEmpty() else emptyList()
                val nombresPerfumes = listOf("Sin perfume asociado") + perfumes.map { it.nombre }
                val adapterPerfume = ArrayAdapter(this@VentaFormActivity, R.layout.spinner_item, nombresPerfumes)
                adapterPerfume.setDropDownViewResource(R.layout.spinner_dropdown_item)
                spinnerPerfume.adapter = adapterPerfume
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            }
        }
    }

    private fun guardar() {
        val cantidadMl = editCantidadMl.text?.toString()?.trim().orEmpty()
        val total = editTotal.text?.toString()?.trim().orEmpty()

        if (total.isEmpty()) {
            mostrarToast(getString(R.string.error_campos_obligatorios))
            return
        }

        val clienteSeleccionado = clientes.getOrNull(spinnerCliente.selectedItemPosition - 1)
        val perfumeSeleccionado = perfumes.getOrNull(spinnerPerfume.selectedItemPosition - 1)

        val datos = mapOf(
            "cliente_id" to clienteSeleccionado?.id,
            "perfume_id" to perfumeSeleccionado?.id,
            "cantidad_ml" to (cantidadMl.toDoubleOrNull() ?: 0.0),
            "total" to (total.toDoubleOrNull() ?: 0.0),
            "estado" to estados[spinnerEstado.selectedItemPosition],
            "notas" to editNotas.text?.toString()?.trim()
        )

        progress.visibility = View.VISIBLE
        btnGuardar.isEnabled = false

        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.crearVenta(datos)
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
