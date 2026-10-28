package com.rfragancias.app.ui.catalogo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rfragancias.app.R
import com.rfragancias.app.model.ItemCarrito
import com.rfragancias.app.utils.comoMoneda
import com.rfragancias.app.utils.urlImagenCompleta

class CarritoAdapter(
    private val alSumar: (ItemCarrito) -> Unit,
    private val alRestar: (ItemCarrito) -> Unit,
    private val alQuitar: (ItemCarrito) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    private val items = mutableListOf<ItemCarrito>()

    fun actualizar(nuevos: List<ItemCarrito>) {
        items.clear()
        items.addAll(nuevos)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_carrito, parent, false)
        return CarritoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        holder.bind(items[position], alSumar, alRestar, alQuitar)
    }

    override fun getItemCount(): Int = items.size

    class CarritoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val img: ImageView = itemView.findViewById(R.id.img_item_carrito)
        private val textoNombre: TextView = itemView.findViewById(R.id.texto_nombre_item)
        private val textoPrecio: TextView = itemView.findViewById(R.id.texto_precio_item)
        private val textoCantidad: TextView = itemView.findViewById(R.id.texto_cantidad)
        private val btnSumar: View = itemView.findViewById(R.id.btn_sumar)
        private val btnRestar: View = itemView.findViewById(R.id.btn_restar)
        private val btnQuitar: View = itemView.findViewById(R.id.btn_quitar_item)

        fun bind(
            item: ItemCarrito,
            alSumar: (ItemCarrito) -> Unit,
            alRestar: (ItemCarrito) -> Unit,
            alQuitar: (ItemCarrito) -> Unit
        ) {
            textoNombre.text = item.perfume.nombre
            textoPrecio.text = item.perfume.precio.comoMoneda()
            textoCantidad.text = "${item.cantidad}"

            Glide.with(img.context)
                .load(urlImagenCompleta(item.perfume.imagenUrl))
                .placeholder(R.drawable.ic_perfume_placeholder)
                .error(R.drawable.ic_perfume_placeholder)
                .fitCenter()
                .into(img)

            btnSumar.setOnClickListener { alSumar(item) }
            btnRestar.setOnClickListener { alRestar(item) }
            btnQuitar.setOnClickListener { alQuitar(item) }
        }
    }
}
