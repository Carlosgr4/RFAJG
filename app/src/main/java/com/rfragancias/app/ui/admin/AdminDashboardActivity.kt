package com.rfragancias.app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.rfragancias.app.R
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.utils.comoMoneda
import com.rfragancias.app.utils.mostrarToast
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var swipeRefresh: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        sessionManager = SessionManager(this)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        swipeRefresh.setColorSchemeResources(R.color.primary)
        swipeRefresh.setOnRefreshListener { cargarResumen() }

        findViewById<TextView>(R.id.texto_bienvenida).text =
            "${getString(R.string.admin_bienvenida)}, ${sessionManager.nombre ?: "admin"}"

        configurarTarjetaMenu(R.id.card_perfumes, R.drawable.ic_catalog, getString(R.string.card_perfumes), "Precios, stock y fotos") {
            startActivity(Intent(this, PerfumesActivity::class.java))
        }
        configurarTarjetaMenu(R.id.card_clientes, R.drawable.ic_person, getString(R.string.card_clientes), "Deudas y abonos") {
            startActivity(Intent(this, ClientesActivity::class.java))
        }
        configurarTarjetaMenu(R.id.card_ventas, R.drawable.ic_sale, getString(R.string.card_ventas), "Historial de ventas") {
            startActivity(Intent(this, VentasActivity::class.java))
        }
        configurarTarjetaMenu(R.id.card_pedidos, R.drawable.ic_box, getString(R.string.card_pedidos), "Insumos y proveedores") {
            startActivity(Intent(this, PedidosActivity::class.java))
        }

        configurarTarjetaEstadistica(R.id.stat_perfumes, getString(R.string.resumen_total_perfumes))
        configurarTarjetaEstadistica(R.id.stat_clientes, getString(R.string.resumen_total_clientes))
        configurarTarjetaEstadistica(R.id.stat_ventas_mes, getString(R.string.resumen_ventas_mes))
        configurarTarjetaEstadistica(R.id.stat_deuda, getString(R.string.resumen_deuda_total))
        configurarTarjetaEstadistica(R.id.stat_pedidos, getString(R.string.resumen_pedidos_pendientes))
        configurarTarjetaEstadistica(R.id.stat_stock_bajo, getString(R.string.resumen_stock_bajo))

        findViewById<android.view.View>(R.id.btn_cerrar_sesion).setOnClickListener { cerrarSesion() }

        cargarResumen()
    }

    private fun configurarTarjetaMenu(id: Int, icono: Int, titulo: String, subtitulo: String, alClick: () -> Unit) {
        val vista = findViewById<android.view.View>(id)
        vista.findViewById<ImageView>(R.id.icono_menu).setImageResource(icono)
        vista.findViewById<TextView>(R.id.titulo_menu).text = titulo
        vista.findViewById<TextView>(R.id.subtitulo_menu).text = subtitulo
        vista.setOnClickListener { alClick() }
    }

    private fun configurarTarjetaEstadistica(id: Int, etiqueta: String) {
        findViewById<android.view.View>(id).findViewById<TextView>(R.id.stat_etiqueta).text = etiqueta
    }

    private fun cargarResumen() {
        swipeRefresh.isRefreshing = true
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getResumenDashboard()
                if (respuesta.isSuccessful) {
                    val resumen = respuesta.body()
                    findViewById<android.view.View>(R.id.stat_perfumes)
                        .findViewById<TextView>(R.id.stat_valor).text = "${resumen?.totalPerfumes ?: 0}"
                    findViewById<android.view.View>(R.id.stat_clientes)
                        .findViewById<TextView>(R.id.stat_valor).text = "${resumen?.totalClientes ?: 0}"
                    findViewById<android.view.View>(R.id.stat_ventas_mes)
                        .findViewById<TextView>(R.id.stat_valor).text = (resumen?.ventasMes ?: 0.0).comoMoneda()
                    findViewById<android.view.View>(R.id.stat_deuda)
                        .findViewById<TextView>(R.id.stat_valor).text = (resumen?.deudaTotal ?: 0.0).comoMoneda()
                    findViewById<android.view.View>(R.id.stat_pedidos)
                        .findViewById<TextView>(R.id.stat_valor).text = "${resumen?.pedidosPendientes ?: 0}"
                    findViewById<android.view.View>(R.id.stat_stock_bajo)
                        .findViewById<TextView>(R.id.stat_valor).text = "${resumen?.stockBajo ?: 0}"
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            } finally {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            try {
                ApiClient.api.logout()
            } catch (_: Exception) {
                // Si no hay conexión igual cerramos sesión localmente.
            }
            ApiClient.limpiarCookies()
            sessionManager.limpiar()
            startActivity(Intent(this@AdminDashboardActivity, com.rfragancias.app.ui.auth.LoginActivity::class.java)
                .apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK })
            finish()
        }
    }
}
