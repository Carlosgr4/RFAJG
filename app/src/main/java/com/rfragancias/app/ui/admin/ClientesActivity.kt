package com.rfragancias.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
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

class ClientesActivity : AppCompatActivity() {

    private lateinit var adapter: ClienteAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var estadoVacio: View
    private lateinit var progress: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_generica)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.titulo_clientes)
        toolbar.setNavigationOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        estadoVacio = findViewById(R.id.estado_vacio)
        findViewById<View>(R.id.btn_reintentar).setOnClickListener { cargarClientes() }
        // Alternativa con botón al gesto de deslizar para actualizar
        toolbar.inflateMenu(R.menu.menu_lista)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.accion_actualizar) { swipeRefresh.isRefreshing = true; cargarClientes(); true } else false
        }
        progress = findViewById(R.id.progress_lista)

        val recycler = findViewById<RecyclerView>(R.id.recycler_lista)
        adapter = ClienteAdapter { cliente ->
            startActivity(Intent(this, ClienteDetalleActivity::class.java).apply {
                putExtra(ClienteDetalleActivity.EXTRA_CLIENTE_ID, cliente.id)
            })
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { cargarClientes() }

        findViewById<FloatingActionButton>(R.id.fab_agregar).setOnClickListener { mostrarDialogoNuevoCliente() }
    }

    override fun onResume() {
        super.onResume()
        cargarClientes()
    }

    private fun cargarClientes() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getClientes()
                val lista = if (respuesta.isSuccessful) respuesta.body().orEmpty() else emptyList()
                adapter.actualizar(lista)
                estadoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            } finally {
                progress.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun mostrarDialogoNuevoCliente() {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_cliente_form, null)
        val dialog = AlertDialog.Builder(this).setView(vista).create()

        vista.findViewById<android.widget.TextView>(R.id.dialog_titulo).text = getString(R.string.titulo_clientes)
        val editNombre = vista.findViewById<TextInputEditText>(R.id.edit_nombre)
        val editTelefono = vista.findViewById<TextInputEditText>(R.id.edit_telefono)
        val editEmail = vista.findViewById<TextInputEditText>(R.id.edit_email)
        val editNotas = vista.findViewById<TextInputEditText>(R.id.edit_notas)
        listOf(editNombre, editEmail).forEach { it.limpiarErrorAlEscribir() }

        vista.findViewById<View>(R.id.btn_cancelar).setOnClickListener { dialog.dismiss() }
        vista.findViewById<View>(R.id.btn_guardar).setOnClickListener {
            val nombre = editNombre.text?.toString()?.trim().orEmpty()
            if (!validarObligatorios(editNombre)) return@setOnClickListener
            if (editEmail.text?.toString()?.trim()?.let { it.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches() } == true) {
                editEmail.mostrarErrorCampo(getString(R.string.error_correo_invalido))
                editEmail.requestFocus()
                return@setOnClickListener
            }
            val datos = mapOf(
                "nombre" to nombre,
                "telefono" to editTelefono.text?.toString()?.trim(),
                "email" to editEmail.text?.toString()?.trim(),
                "notas" to editNotas.text?.toString()?.trim()
            )
            lifecycleScope.launch {
                try {
                    val respuesta = ApiClient.api.crearCliente(datos)
                    if (respuesta.isSuccessful) {
                        dialog.dismiss()
                        cargarClientes()
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
}
