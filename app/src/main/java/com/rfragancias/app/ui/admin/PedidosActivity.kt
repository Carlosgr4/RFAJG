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
import com.rfragancias.app.model.Pedido
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

class PedidosActivity : AppCompatActivity() {

    private lateinit var adapter: PedidoAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var estadoVacio: View
    private lateinit var progress: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_generica)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.titulo_pedidos)
        toolbar.setNavigationOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        estadoVacio = findViewById(R.id.estado_vacio)
        findViewById<View>(R.id.btn_reintentar).setOnClickListener { cargarPedidos() }
        // Alternativa con botón al gesto de deslizar para actualizar
        toolbar.inflateMenu(R.menu.menu_lista)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.accion_actualizar) { swipeRefresh.isRefreshing = true; cargarPedidos(); true } else false
        }
        progress = findViewById(R.id.progress_lista)

        val recycler = findViewById<RecyclerView>(R.id.recycler_lista)
        adapter = PedidoAdapter { pedido -> confirmarEliminar(pedido) }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { cargarPedidos() }

        findViewById<FloatingActionButton>(R.id.fab_agregar).setOnClickListener {
            startActivity(Intent(this, PedidoFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarPedidos()
    }

    private fun cargarPedidos() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getPedidos()
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

    private fun confirmarEliminar(pedido: Pedido) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirmar_eliminar_pedido))
            .setNegativeButton(getString(R.string.cancelar), null)
            .setPositiveButton(getString(R.string.aceptar)) { _, _ -> eliminarPedido(pedido) }
            .show()
    }

    private fun eliminarPedido(pedido: Pedido) {
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.eliminarPedido(pedido.id)
                if (respuesta.isSuccessful) {
                    cargarPedidos()
                } else {
                    mostrarToast(getString(R.string.error_generico))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            }
        }
    }
}
