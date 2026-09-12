package com.rfragancias.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rfragancias.app.R
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.ui.admin.AdminDashboardActivity
import com.rfragancias.app.ui.auth.LoginActivity
import com.rfragancias.app.ui.catalogo.CarritoFragment
import com.rfragancias.app.ui.catalogo.CatalogoFragment
import kotlinx.coroutines.launch

/**
 * Contenedor con navegación inferior. Los clientes ven Catálogo + Carrito;
 * el administrador ve Catálogo + Admin (la pestaña Admin abre el panel de
 * gestión en su propia pila de pantallas, como ya lo hacía la app).
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

        // Sin sesión activa: de vuelta al login (por ejemplo, si el proceso
        // del sistema mató la app y la reabrió aquí).
        if (!sessionManager.estaLogueado) {
            irALogin()
            return
        }

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar))
        supportActionBar?.title = if (sessionManager.esAdmin) {
            getString(R.string.app_name)
        } else {
            sessionManager.nombre ?: getString(R.string.app_name)
        }

        bottomNav = findViewById(R.id.bottom_nav)
        // Los clientes no gestionan el catálogo desde aquí; el admin no
        // arma pedidos, así que cada rol solo ve lo que le corresponde.
        bottomNav.menu.findItem(R.id.nav_carrito).isVisible = sessionManager.esCliente
        bottomNav.menu.findItem(R.id.nav_admin).isVisible = sessionManager.esAdmin

        if (savedInstanceState == null) {
            mostrarFragmento(CatalogoFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catalogo -> {
                    mostrarFragmento(CatalogoFragment())
                    true
                }
                R.id.nav_carrito -> {
                    mostrarFragmento(CarritoFragment())
                    true
                }
                R.id.nav_admin -> {
                    irAAdmin()
                    false // no cambiamos la pestaña resaltada, es una acción de navegación
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.accion_cerrar_sesion) {
            confirmarCerrarSesion()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun confirmarCerrarSesion() {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.confirmar_cerrar_sesion))
            .setNegativeButton(getString(R.string.cancelar), null)
            .setPositiveButton(getString(R.string.aceptar)) { _, _ -> cerrarSesion() }
            .show()
    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            try {
                ApiClient.api.logout()
            } catch (_: Exception) {
                // Sin conexión igual cerramos sesión localmente.
            }
            ApiClient.limpiarCookies()
            sessionManager.limpiar()
            irALogin()
        }
    }

    private fun irALogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
        finish()
    }

    private fun mostrarFragmento(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    /**
     * Antes de abrir la zona de administración, confirmamos con el servidor
     * si la sesión sigue activa (por si expiró o se cerró desde otro
     * dispositivo). Mientras responde usamos lo que sabemos localmente.
     */
    private fun irAAdmin() {
        startActivity(Intent(this, AdminDashboardActivity::class.java))

        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.session()
                val autenticado = respuesta.body()?.autenticado ?: false
                if (respuesta.isSuccessful && !autenticado) {
                    sessionManager.limpiar()
                }
            } catch (_: Exception) {
                // Sin conexión: seguimos confiando en la sesión local.
            }
        }
    }
}
