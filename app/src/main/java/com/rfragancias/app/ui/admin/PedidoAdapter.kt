package com.rfragancias.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rfragancias.app.R
import com.rfragancias.app.model.Pedido
import com.rfragancias.app.utils.comoMoneda

class PedidoAdapter(
    private val alEliminar: (Pedido) -> Unit
) : RecyclerView.Adapter<PedidoAdapter.VH>() {

    private val items = mutableListOf<Pedido>()

    fun actualizar(nuevos: List<Pedido>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false)
        return VH(vista)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pedido = items[position]
        holder.insumo.text = pedido.insumo
        holder.proveedor.text = pedido.proveedor ?: "Sin proveedor registrado"
        holder.costo.text = pedido.costo.comoMoneda()
        holder.estado.text = pedido.estado
        holder.fecha.text = pedido.fecha ?: ""
        holder.btnEliminar.setOnClickListener { alEliminar(pedido) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val insumo: TextView = itemView.findViewById(R.id.texto_insumo)
        val proveedor: TextView = itemView.findViewById(R.id.texto_proveedor)
        val costo: TextView = itemView.findViewById(R.id.texto_costo)
        val estado: TextView = itemView.findViewById(R.id.texto_estado)
        val fecha: TextView = itemView.findViewById(R.id.texto_fecha)
        val btnEliminar: View = itemView.findViewById(R.id.btn_eliminar)
    }
}
