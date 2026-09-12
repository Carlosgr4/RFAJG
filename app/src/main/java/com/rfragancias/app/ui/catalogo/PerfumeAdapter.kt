package com.rfragancias.app.ui.catalogo

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.rfragancias.app.R
import com.rfragancias.app.model.Perfume
import com.rfragancias.app.utils.urlImagenCompleta

class PerfumeAdapter(
    private val alHacerClick: (Perfume) -> Unit,
    private val alAgregarCarrito: ((Perfume) -> Unit)? = null
) : RecyclerView.Adapter<PerfumeAdapter.PerfumeViewHolder>() {

    private val items = mutableListOf<Perfume>()

    fun actualizar(nuevos: List<Perfume>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerfumeViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_perfume, parent, false)
        return PerfumeViewHolder(vista)
    }

    override fun onBindViewHolder(holder: PerfumeViewHolder, position: Int) {
        holder.bind(items[position], alHacerClick, alAgregarCarrito)
    }

    override fun getItemCount(): Int = items.size

    class PerfumeViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val img: ImageView = itemView.findViewById(R.id.img_perfume)
        private val badgeGenero: TextView = itemView.findViewById(R.id.badge_genero)
        private val textoDisenador: TextView = itemView.findViewById(R.id.texto_disenador)
        private val textoNombre: TextView = itemView.findViewById(R.id.texto_nombre)
        private val textoNotas: TextView = itemView.findViewById(R.id.texto_notas)
        private val btnAgregarCarrito: MaterialButton = itemView.findViewById(R.id.btn_agregar_carrito)

        fun bind(perfume: Perfume, alHacerClick: (Perfume) -> Unit, alAgregarCarrito: ((Perfume) -> Unit)?) {
            textoNombre.text = perfume.nombre
            textoDisenador.text = perfume.disenador ?: ""
            textoNotas.text = perfume.notas ?: ""
            badgeGenero.text = perfume.genero

            Glide.with(img.context)
                .load(urlImagenCompleta(perfume.imagenUrl))
                .placeholder(R.drawable.ic_perfume_placeholder)
                .error(R.drawable.ic_perfume_placeholder)
                .centerCrop()
                .into(img)

            itemView.setOnClickListener { alHacerClick(perfume) }

            if (alAgregarCarrito != null) {
                btnAgregarCarrito.visibility = android.view.View.VISIBLE
                btnAgregarCarrito.setOnClickListener { alAgregarCarrito(perfume) }
            } else {
                btnAgregarCarrito.visibility = android.view.View.GONE
            }
        }
    }
}
