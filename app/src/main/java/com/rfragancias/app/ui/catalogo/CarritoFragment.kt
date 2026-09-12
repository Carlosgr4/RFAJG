package com.rfragancias.app.ui.catalogo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rfragancias.app.R
import com.rfragancias.app.model.CarritoManager
import com.rfragancias.app.model.ItemCarrito
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.utils.comoMoneda
import com.rfragancias.app.utils.mostrarSnackbar

/**
 * Muestra el carrito en memoria del cliente. Al confirmar, no se crea
 * ningún pedido en el servidor: solo se arma un mensaje de WhatsApp con
 * lo seleccionado, tal como pidió el negocio.
 */
class CarritoFragment : Fragment(R.layout.fragment_carrito) {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: CarritoAdapter
    private lateinit var textoVacio: View
    private lateinit var contenedorTotal: View
    private lateinit var textoTotal: android.widget.TextView
    private lateinit var btnConfirmar: MaterialButton
    private lateinit var sessionManager: SessionManager

    private val listenerCarrito = { actualizarVista() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        recycler = view.findViewById(R.id.recycler_carrito)
        textoVacio = view.findViewById(R.id.texto_carrito_vacio)
        contenedorTotal = view.findViewById(R.id.contenedor_total)
        textoTotal = view.findViewById(R.id.texto_total_carrito)
        btnConfirmar = view.findViewById(R.id.btn_confirmar_pedido)

        adapter = CarritoAdapter(
            alSumar = { item -> CarritoManager.cambiarCantidad(item.perfume.id, item.cantidad + 1) },
            alRestar = { item -> CarritoManager.cambiarCantidad(item.perfume.id, item.cantidad - 1) },
            alQuitar = { item -> CarritoManager.quitar(item.perfume.id) }
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnConfirmar.setOnClickListener { confirmarPedidoPorWhatsApp() }

        actualizarVista()
    }

    override fun onResume() {
        super.onResume()
        CarritoManager.agregarListener(listenerCarrito)
        actualizarVista()
    }

    override fun onPause() {
        super.onPause()
        CarritoManager.quitarListener(listenerCarrito)
    }

    private fun actualizarVista() {
        if (!isAdded) return
        val items = CarritoManager.obtenerItems()
        adapter.actualizar(items)
        val vacio = items.isEmpty()
        textoVacio.visibility = if (vacio) View.VISIBLE else View.GONE
        recycler.visibility = if (vacio) View.GONE else View.VISIBLE
        contenedorTotal.visibility = if (vacio) View.GONE else View.VISIBLE
        textoTotal.text = CarritoManager.totalPrecio().comoMoneda()
    }

    private fun confirmarPedidoPorWhatsApp() {
        val items = CarritoManager.obtenerItems()
        if (items.isEmpty()) {
            view?.mostrarSnackbar(getString(R.string.carrito_vacio))
            return
        }

        val mensaje = buildString {
            append("Hola, quiero hacer este pedido:\n\n")
            items.forEach { item: ItemCarrito ->
                append("• ${item.perfume.nombre} x${item.cantidad} — ${item.subtotal.comoMoneda()}\n")
            }
            append("\nTotal: ${CarritoManager.totalPrecio().comoMoneda()}")
            sessionManager.nombre?.let { nombre ->
                append("\n\nCliente: $nombre")
            }
        }

        val numero = getString(R.string.whatsapp_numero)
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
        )
        startActivity(intent)
    }
}
