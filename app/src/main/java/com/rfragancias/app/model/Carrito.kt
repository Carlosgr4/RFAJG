package com.rfragancias.app.model

data class ItemCarrito(
    val perfume: Perfume,
    var cantidad: Int = 1
) {
    val subtotal: Double
        get() = perfume.precio * cantidad
}

/**
 * El carrito vive solo en memoria (no se guarda en el servidor): al
 * confirmar, la app arma un mensaje de WhatsApp con lo seleccionado y no
 * necesita persistencia propia. Es un singleton para que Catálogo y
 * Carrito compartan el mismo estado mientras la app está abierta.
 */
object CarritoManager {

    private val items = LinkedHashMap<Int, ItemCarrito>()
    private val listeners = mutableListOf<() -> Unit>()

    fun agregar(perfume: Perfume) {
        val actual = items[perfume.id]
        if (actual != null) {
            actual.cantidad += 1
        } else {
            items[perfume.id] = ItemCarrito(perfume, 1)
        }
        notificar()
    }

    fun cambiarCantidad(perfumeId: Int, cantidad: Int) {
        val item = items[perfumeId] ?: return
        if (cantidad <= 0) {
            items.remove(perfumeId)
        } else {
            item.cantidad = cantidad
        }
        notificar()
    }

    fun quitar(perfumeId: Int) {
        items.remove(perfumeId)
        notificar()
    }

    fun limpiar() {
        items.clear()
        notificar()
    }

    fun obtenerItems(): List<ItemCarrito> = items.values.toList()

    fun cantidadTotalItems(): Int = items.values.sumOf { it.cantidad }

    fun totalPrecio(): Double = items.values.sumOf { it.subtotal }

    fun estaVacio(): Boolean = items.isEmpty()

    /** Permite que las pantallas se refresquen (badge del carrito, etc.) cuando cambia el contenido. */
    fun agregarListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun quitarListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notificar() {
        listeners.forEach { it() }
    }
}
