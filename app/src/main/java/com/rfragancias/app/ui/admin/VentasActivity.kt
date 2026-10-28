package com.rfragancias.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rfragancias.app.R
import com.rfragancias.app.model.Venta
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

class VentasActivity : AppCompatActivity() {

    private lateinit var adapter: VentaAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var estadoVacio: View
    private lateinit var progress: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_generica)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.titulo_ventas)
        toolbar.setNavigationOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        estadoVacio = findViewById(R.id.estado_vacio)
        progress = findViewById(R.id.progress_lista)

        val recycler = findViewById<RecyclerView>(R.id.recycler_lista)
        adapter = VentaAdapter { venta -> confirmarEliminar(venta) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { cargarVentas() }

        findViewById<FloatingActionButton>(R.id.fab_agregar).setOnClickListener {
            startActivity(Intent(this, VentaFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarVentas()
    }

    private fun cargarVentas() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getVentas()
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

    private fun confirmarEliminar(venta: Venta) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirmar_eliminar_venta))
            .setNegativeButton(getString(R.string.cancelar), null)
            .setPositiveButton(getString(R.string.aceptar)) { _, _ -> eliminarVenta(venta) }
            .show()
    }

    private fun eliminarVenta(venta: Venta) {
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.eliminarVenta(venta.id)
                if (respuesta.isSuccessful) {
                    cargarVentas()
                } else {
                    mostrarToast(getString(R.string.error_generico))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            }
        }
    }
}
