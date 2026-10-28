package com.rfragancias.app.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rfragancias.app.R
import com.rfragancias.app.model.Perfume
import com.rfragancias.app.utils.comoMoneda
import com.rfragancias.app.utils.urlImagenCompleta

class PerfumeAdminAdapter(
    private val alEditar: (Perfume) -> Unit,
    private val alEliminar: (Perfume) -> Unit
) : RecyclerView.Adapter<PerfumeAdminAdapter.VH>() {

    private val items = mutableListOf<Perfume>()

    fun actualizar(nuevos: List<Perfume>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_perfume_admin, parent, false)
        return VH(vista)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val perfume = items[position]
        holder.nombre.text = perfume.nombre
        holder.subtitulo.text = "${perfume.disenador ?: "Sin diseñador"} · ${perfume.precio.comoMoneda()}"
        holder.badge.text = if (perfume.activo) "Visible" else "Oculto"

        Glide.with(holder.img.context)
            .load(urlImagenCompleta(perfume.imagenUrl))
            .placeholder(R.drawable.ic_perfume_placeholder)
            .error(R.drawable.ic_perfume_placeholder)
            .fitCenter()
            .into(holder.img)

        holder.itemView.setOnClickListener { alEditar(perfume) }
        holder.btnEditar.setOnClickListener { alEditar(perfume) }
        holder.btnEliminar.setOnClickListener { alEliminar(perfume) }
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.img_item)
        val nombre: TextView = itemView.findViewById(R.id.texto_nombre)
        val subtitulo: TextView = itemView.findViewById(R.id.texto_subtitulo)
        val badge: TextView = itemView.findViewById(R.id.texto_badge)
        val btnEditar: View = itemView.findViewById(R.id.btn_editar)
        val btnEliminar: View = itemView.findViewById(R.id.btn_eliminar)
    }
}
