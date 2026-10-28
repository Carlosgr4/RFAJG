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
import com.rfragancias.app.utils.mensajeDeError
import com.rfragancias.app.utils.validarObligatorios
import com.rfragancias.app.utils.validarNumeros
import com.rfragancias.app.utils.limpiarErrorAlEscribir
import com.rfragancias.app.utils.mostrarErrorCampo

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
        listOf(editNombre, editTelefono, editCorreo, editPassword).forEach { it.limpiarErrorAlEscribir() }
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

            if (!validarObligatorios(editNombre, editTelefono, editCorreo, editPassword)) {
                textoError.visibility = View.GONE
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                editCorreo.mostrarErrorCampo(getString(R.string.error_correo_invalido))
                editCorreo.requestFocus()
                return@setOnClickListener
            }
            if (password.length < 6) {
                editPassword.mostrarErrorCampo(getString(R.string.error_clave_corta))
                editPassword.requestFocus()
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
                    mostrarError(textoError, mensajeDeError(e))
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
