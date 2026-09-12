package com.rfragancias.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rfragancias.app.R
import com.rfragancias.app.model.Cliente
import com.rfragancias.app.utils.comoMoneda

class ClienteAdapter(
    private val alHacerClick: (Cliente) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.VH>() {

    private val items = mutableListOf<Cliente>()

    fun actualizar(nuevos: List<Cliente>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
        return VH(vista)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val cliente = items[position]
        holder.nombre.text = cliente.nombre
        holder.subtitulo.text = cliente.telefono ?: cliente.email ?: "Sin datos de contacto"
        holder.deuda.text = if (cliente.deuda > 0) cliente.deuda.comoMoneda() else "Al día"
        holder.deuda.setTextColor(
            holder.itemView.context.getColor(if (cliente.deuda > 0) R.color.danger else R.color.success)
        )
        holder.itemView.setOnClickListener { alHacerClick(cliente) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.texto_nombre)
        val subtitulo: TextView = itemView.findViewById(R.id.texto_subtitulo)
        val deuda: TextView = itemView.findViewById(R.id.texto_deuda)
    }
}
