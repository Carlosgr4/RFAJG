package com.rfragancias.app.ui.catalogo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rfragancias.app.R
import com.rfragancias.app.model.CarritoManager
import com.rfragancias.app.model.Perfume
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.utils.mostrarSnackbar
import kotlinx.coroutines.launch

class CatalogoFragment : Fragment(R.layout.fragment_catalogo) {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: PerfumeAdapter
    private lateinit var textoVacio: View
    private lateinit var contenedorFiltros: ViewGroup
    private lateinit var editBuscar: android.widget.EditText
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var sessionManager: SessionManager

    private val filtros = listOf(
        "" to R.string.filtro_todos,
        "Femenino" to R.string.filtro_femenino,
        "Masculino" to R.string.filtro_masculino,
        "Unisex" to R.string.filtro_unisex
    )
    private var filtroSeleccionado: String = ""
    private var textoBusqueda: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        recycler = view.findViewById(R.id.recycler_perfumes)
        textoVacio = view.findViewById(R.id.texto_vacio)
        contenedorFiltros = view.findViewById(R.id.contenedor_filtros)
        editBuscar = view.findViewById(R.id.edit_buscar)
        swipeRefresh = view.findViewById(R.id.swipe_refresh)

        adapter = PerfumeAdapter(
            alHacerClick = { perfume ->
                val intent = Intent(requireContext(), PerfumeDetalleActivity::class.java)
                intent.putExtra(PerfumeDetalleActivity.EXTRA_PERFUME_ID, perfume.id)
                startActivity(intent)
            },
            alAgregarCarrito = if (sessionManager.esCliente) { perfume ->
                CarritoManager.agregar(perfume)
                view.mostrarSnackbar(getString(R.string.perfume_agregado_carrito))
            } else null
        )
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        construirFiltros()

        editBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textoBusqueda = s?.toString().orEmpty()
                cargarPerfumes()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        swipeRefresh.setOnRefreshListener { cargarPerfumes() }
        swipeRefresh.setColorSchemeResources(R.color.primary)

        view.findViewById<View>(R.id.btn_whatsapp_contacto).setOnClickListener { abrirWhatsApp() }
        view.findViewById<View>(R.id.fab_whatsapp).setOnClickListener { abrirWhatsApp() }

        cargarPerfumes()
    }

    private fun construirFiltros() {
        contenedorFiltros.removeAllViews()
        filtros.forEachIndexed { index, (valor, textoResId) ->
            val chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_chip_filtro, contenedorFiltros, false) as android.widget.TextView
            chip.text = getString(textoResId)
            chip.isSelected = valor == filtroSeleccionado
            chip.setOnClickListener {
                filtroSeleccionado = valor
                construirFiltros()
                cargarPerfumes()
            }
            contenedorFiltros.addView(chip)
        }
    }

    private fun cargarPerfumes() {
        swipeRefresh.isRefreshing = true
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getPerfumes(
                    genero = filtroSeleccionado.ifBlank { null },
                    buscar = textoBusqueda.ifBlank { null }
                )
                val lista = if (respuesta.isSuccessful) respuesta.body().orEmpty() else emptyList()
                adapter.actualizar(lista)
                textoVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                view?.mostrarSnackbar(getString(R.string.error_conexion))
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun abrirWhatsApp() {
        val numero = getString(R.string.whatsapp_numero)
        val mensaje = Uri.encode("Hola, quiero información sobre las fragancias de R FRAGANCIAS")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=$mensaje"))
        startActivity(intent)
    }
}
