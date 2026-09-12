package com.rfragancias.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rfragancias.app.R
import com.rfragancias.app.model.Venta
import com.rfragancias.app.utils.comoMoneda

class VentaAdapter(
    private val alEliminar: (Venta) -> Unit
) : RecyclerView.Adapter<VentaAdapter.VH>() {

    private val items = mutableListOf<Venta>()

    fun actualizar(nuevos: List<Venta>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_venta, parent, false)
        return VH(vista)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val venta = items[position]
        holder.perfume.text = venta.perfumeNombre ?: "Venta sin perfume asociado"
        holder.cliente.text = venta.clienteNombre ?: "Cliente ocasional"
        holder.total.text = venta.total.comoMoneda()
        holder.estado.text = venta.estado
        holder.fecha.text = venta.fecha ?: ""
        holder.btnEliminar.setOnClickListener { alEliminar(venta) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val perfume: TextView = itemView.findViewById(R.id.texto_perfume)
        val cliente: TextView = itemView.findViewById(R.id.texto_cliente)
        val total: TextView = itemView.findViewById(R.id.texto_total)
        val estado: TextView = itemView.findViewById(R.id.texto_estado)
        val fecha: TextView = itemView.findViewById(R.id.texto_fecha)
        val btnEliminar: View = itemView.findViewById(R.id.btn_eliminar)
    }
}
