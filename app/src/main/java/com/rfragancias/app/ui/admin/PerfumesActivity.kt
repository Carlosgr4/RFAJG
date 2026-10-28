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
import com.rfragancias.app.model.Perfume
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

class PerfumesActivity : AppCompatActivity() {

    private lateinit var adapter: PerfumeAdminAdapter
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var estadoVacio: View
    private lateinit var progress: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista_generica)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = getString(R.string.titulo_perfumes)
        toolbar.setNavigationOnClickListener { finish() }

        swipeRefresh = findViewById(R.id.swipe_refresh)
        estadoVacio = findViewById(R.id.estado_vacio)
        progress = findViewById(R.id.progress_lista)

        val recycler = findViewById<RecyclerView>(R.id.recycler_lista)
        adapter = PerfumeAdminAdapter(
            alEditar = { perfume ->
                startActivity(Intent(this, PerfumeFormActivity::class.java).apply {
                    putExtra(PerfumeFormActivity.EXTRA_PERFUME_ID, perfume.id)
                })
            },
            alEliminar = { perfume -> confirmarEliminar(perfume) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { cargarPerfumes() }

        findViewById<FloatingActionButton>(R.id.fab_agregar).setOnClickListener {
            startActivity(Intent(this, PerfumeFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        cargarPerfumes()
    }

    private fun cargarPerfumes() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getPerfumesAdmin()
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

    private fun confirmarEliminar(perfume: Perfume) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirmar_eliminar_perfume))
            .setNegativeButton(getString(R.string.cancelar), null)
            .setPositiveButton(getString(R.string.aceptar)) { _, _ -> eliminarPerfume(perfume) }
            .show()
    }

    private fun eliminarPerfume(perfume: Perfume) {
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.eliminarPerfume(perfume.id)
                if (respuesta.isSuccessful) {
                    cargarPerfumes()
                } else {
                    mostrarToast(getString(R.string.error_generico))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            }
        }
    }
}
