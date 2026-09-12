package com.rfragancias.app.ui.auth

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.rfragancias.app.R
import com.rfragancias.app.model.RegistroRequest
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.network.SessionManager
import com.rfragancias.app.ui.MainActivity
import kotlinx.coroutines.launch
import android.content.Intent
import android.util.Patterns

/**
 * Registro de un cliente nuevo. Pide nombre, teléfono, correo y
 * contraseña; al terminar, el cliente queda logueado automáticamente
 * (el backend abre la sesión igual que en el login).
 */
class RegistroActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        sessionManager = SessionManager(this)

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        val editNombre = findViewById<TextInputEditText>(R.id.edit_nombre)
        val editTelefono = findViewById<TextInputEditText>(R.id.edit_telefono)
        val editCorreo = findViewById<TextInputEditText>(R.id.edit_correo)
        val editPassword = findViewById<TextInputEditText>(R.id.edit_password)
        val textoError = findViewById<android.widget.TextView>(R.id.texto_error)
        val progress = findViewById<View>(R.id.progress_registro)
        val btnCrear = findViewById<MaterialButton>(R.id.btn_crear_cuenta)
        val linkYaTengoCuenta = findViewById<android.widget.TextView>(R.id.link_ya_tengo_cuenta)

        linkYaTengoCuenta.setOnClickListener { finish() }

        btnCrear.setOnClickListener {
            val nombre = editNombre.text?.toString()?.trim().orEmpty()
            val telefono = editTelefono.text?.toString()?.trim().orEmpty()
            val correo = editCorreo.text?.toString()?.trim().orEmpty()
            val password = editPassword.text?.toString()?.trim().orEmpty()

            if (nombre.isEmpty() || telefono.isEmpty() || correo.isEmpty() || password.isEmpty()) {
                mostrarError(textoError, getString(R.string.error_campos_obligatorios))
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrarError(textoError, getString(R.string.error_correo_invalido))
                return@setOnClickListener
            }
            if (password.length < 6) {
                mostrarError(textoError, getString(R.string.error_clave_corta))
                return@setOnClickListener
            }

            textoError.visibility = View.GONE
            progress.visibility = View.VISIBLE
            btnCrear.isEnabled = false

            lifecycleScope.launch {
                try {
                    val respuesta = ApiClient.api.registro(
                        RegistroRequest(nombre = nombre, telefono = telefono, email = correo, password = password)
                    )
                    val cuerpo = respuesta.body()
                    if (respuesta.isSuccessful && cuerpo?.ok == true && cuerpo.rol != null) {
                        sessionManager.guardarSesion(cuerpo.rol, cuerpo.nombre ?: nombre, cuerpo.clienteId)
                        startActivity(Intent(this@RegistroActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                        finish()
                    } else if (respuesta.code() == 409) {
                        mostrarError(textoError, getString(R.string.error_correo_registrado))
                    } else {
                        mostrarError(textoError, getString(R.string.error_registro))
                    }
                } catch (e: Exception) {
                    mostrarError(textoError, getString(R.string.error_conexion))
                } finally {
                    progress.visibility = View.GONE
                    btnCrear.isEnabled = true
                }
            }
        }
    }

    private fun mostrarError(vista: android.widget.TextView, mensaje: String) {
        vista.text = mensaje
        vista.visibility = View.VISIBLE
    }
}
