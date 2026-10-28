package com.rfragancias.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rfragancias.app.BuildConfig
import com.rfragancias.app.R
import com.rfragancias.app.model.LoginRequest
import com.rfragancias.app.model.ROL_ADMIN
import com.rfragancias.app.model.ROL_CLIENTE
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.ui.MainActivity
import kotlinx.coroutines.launch
import com.rfragancias.app.utils.mensajeDeError

/**
 * Pantalla de inicio de la app. Un solo formulario sirve tanto para
 * clientes como para el administrador: el backend decide el rol y esta
 * pantalla navega a la sección correspondiente.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        // Con Firebase, la sesión local solo vale si Firebase Auth también
        // tiene un usuario activo (por ejemplo, borra la sesión del antiguo
        // acceso de prueba "admin/admin").
        if (BuildConfig.USAR_FIREBASE && sessionManager.estaLogueado && !hayUsuarioFirebase()) {
            sessionManager.limpiar()
        }

        // Si ya hay sesión guardada localmente, saltamos directo a la app.
        if (sessionManager.estaLogueado) {
            irAInicio()
            finish()
            return
        }

        val editUsuario = findViewById<TextInputEditText>(R.id.edit_usuario)
        val editClave = findViewById<TextInputEditText>(R.id.edit_clave)
        val textoError = findViewById<android.widget.TextView>(R.id.texto_error)
        val progress = findViewById<View>(R.id.progress_login)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)
        val linkCrearCuenta = findViewById<android.widget.TextView>(R.id.link_crear_cuenta)

        linkCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val usuario = editUsuario.text?.toString()?.trim().orEmpty()
            val clave = editClave.text?.toString()?.trim().orEmpty()

            if (usuario.isEmpty() || clave.isEmpty()) {
                textoError.text = getString(R.string.error_campos_obligatorios)
                textoError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // --- BYPASS LOCAL PARA DESARROLLO ---
            // Solo funciona en modo servidor local. Con Firebase activo se usa
            // el login real, porque Firestore exige una sesión válida para
            // guardar datos.
            if (!BuildConfig.USAR_FIREBASE && usuario == "admin" && clave == "admin") {
                sessionManager.guardarSesion(ROL_ADMIN, "Admin Local", -1)
                irAInicio()
                finish()
                return@setOnClickListener
            }
            if (!BuildConfig.USAR_FIREBASE && usuario == "user" && clave == "user") {
                sessionManager.guardarSesion(ROL_CLIENTE, "Usuario Local", 1)
                irAInicio()
                finish()
                return@setOnClickListener
            }
            // ------------------------------------

            textoError.visibility = View.GONE
            progress.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            lifecycleScope.launch {
                try {
                    val respuesta = ApiClient.api.login(LoginRequest(usuario, clave))
                    val cuerpo = respuesta.body()
                    if (respuesta.isSuccessful && cuerpo?.ok == true && cuerpo.rol != null) {
                        sessionManager.guardarSesion(cuerpo.rol, cuerpo.nombre ?: usuario, cuerpo.clienteId)
                        irAInicio()
                        finish()
                    } else {
                        textoError.text = getString(R.string.error_credenciales)
                        textoError.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    textoError.text = mensajeDeError(e)
                    textoError.visibility = View.VISIBLE
                } finally {
                    progress.visibility = View.GONE
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun hayUsuarioFirebase(): Boolean = try {
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    } catch (_: Exception) {
        false // Firebase todavía sin configurar (falta google-services.json)
    }

    private fun irAInicio() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}
