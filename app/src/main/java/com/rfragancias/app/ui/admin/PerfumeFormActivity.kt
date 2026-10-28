package com.rfragancias.app.ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.rfragancias.app.R
import com.rfragancias.app.network.ApiClient
import com.rfragancias.app.utils.mostrarToast
import com.rfragancias.app.utils.urlImagenCompleta
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import com.rfragancias.app.utils.mensajeDeError

class PerfumeFormActivity : AppCompatActivity() {

    private var perfumeId: Int = -1
    private var imagenSeleccionada: Uri? = null
    private val generos = listOf("Femenino", "Masculino", "Unisex")

    private lateinit var editNombre: TextInputEditText
    private lateinit var editDisenador: TextInputEditText
    private lateinit var editNotas: TextInputEditText
    private lateinit var editPrecio: TextInputEditText
    private lateinit var editGramos: TextInputEditText
    private lateinit var spinnerGenero: Spinner
    private lateinit var switchActivo: SwitchMaterial
    private lateinit var imgPreview: ImageView
    private lateinit var progress: View
    private lateinit var btnGuardar: MaterialButton

    private val selectorImagen = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imagenSeleccionada = uri
            Glide.with(this).load(uri).into(imgPreview)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfume_form)

        perfumeId = intent.getIntExtra(EXTRA_PERFUME_ID, -1)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = if (perfumeId == -1) "Nuevo perfume" else "Editar perfume"

        editNombre = findViewById(R.id.edit_nombre)
        editDisenador = findViewById(R.id.edit_disenador)
        editNotas = findViewById(R.id.edit_notas)
        editPrecio = findViewById(R.id.edit_precio)
        editGramos = findViewById(R.id.edit_gramos)
        spinnerGenero = findViewById(R.id.spinner_genero)
        switchActivo = findViewById(R.id.switch_activo)
        imgPreview = findViewById(R.id.img_preview)
        progress = findViewById(R.id.progress_form)
        btnGuardar = findViewById(R.id.btn_guardar)

        val adapterGenero = ArrayAdapter(this, R.layout.spinner_item, generos)
        adapterGenero.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerGenero.adapter = adapterGenero

        findViewById<View>(R.id.contenedor_imagen).setOnClickListener {
            selectorImagen.launch("image/*")
        }

        if (perfumeId != -1) {
            cargarPerfumeExistente(perfumeId)
        }

        btnGuardar.setOnClickListener { guardar() }
    }

    private fun cargarPerfumeExistente(id: Int) {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val respuesta = ApiClient.api.getPerfume(id)
                val perfume = respuesta.body()
                if (respuesta.isSuccessful && perfume != null) {
                    editNombre.setText(perfume.nombre)
                    editDisenador.setText(perfume.disenador)
                    editNotas.setText(perfume.notas)
                    editPrecio.setText(perfume.precio.toString())
                    editGramos.setText(perfume.gramosDisponibles.toString())
                    switchActivo.isChecked = perfume.activo
                    val posicionGenero = generos.indexOf(perfume.genero).coerceAtLeast(0)
                    spinnerGenero.setSelection(posicionGenero)

                    Glide.with(this@PerfumeFormActivity)
                        .load(urlImagenCompleta(perfume.imagenUrl))
                        .placeholder(R.drawable.ic_perfume_placeholder)
                        .into(imgPreview)
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun guardar() {
        val nombre = editNombre.text?.toString()?.trim().orEmpty()
        val precio = editPrecio.text?.toString()?.trim().orEmpty()
        val gramos = editGramos.text?.toString()?.trim().orEmpty()

        if (nombre.isEmpty() || precio.isEmpty() || gramos.isEmpty()) {
            mostrarToast(getString(R.string.error_campos_obligatorios))
            return
        }

        progress.visibility = View.VISIBLE
        btnGuardar.isEnabled = false

        val disenador = editDisenador.text?.toString()?.trim().orEmpty()
        val notas = editNotas.text?.toString()?.trim().orEmpty()
        val genero = generos[spinnerGenero.selectedItemPosition]
        val activo = switchActivo.isChecked

        lifecycleScope.launch {
            try {
                val nombreBody = nombre.toRequestBody("text/plain".toMediaTypeOrNull())
                val disenadorBody = disenador.toRequestBody("text/plain".toMediaTypeOrNull())
                val generoBody = genero.toRequestBody("text/plain".toMediaTypeOrNull())
                val notasBody = notas.toRequestBody("text/plain".toMediaTypeOrNull())
                val precioBody = precio.toRequestBody("text/plain".toMediaTypeOrNull())
                val gramosBody = gramos.toRequestBody("text/plain".toMediaTypeOrNull())
                val activoBody = activo.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val imagenPart = imagenSeleccionada?.let { uri -> crearParteImagen(uri) }

                val respuesta = if (perfumeId == -1) {
                    ApiClient.api.crearPerfume(nombreBody, disenadorBody, generoBody, notasBody, precioBody, gramosBody, imagenPart)
                } else {
                    ApiClient.api.actualizarPerfume(perfumeId, nombreBody, disenadorBody, generoBody, notasBody, precioBody, gramosBody, activoBody, imagenPart)
                }

                if (respuesta.isSuccessful) {
                    mostrarToast(getString(R.string.perfume_guardado))
                    finish()
                } else {
                    mostrarToast(getString(R.string.error_generico))
                }
            } catch (e: Exception) {
                mostrarToast(mensajeDeError(e))
            } finally {
                progress.visibility = View.GONE
                btnGuardar.isEnabled = true
            }
        }
    }

    /** Copia la imagen elegida a un archivo temporal para poder enviarla como multipart. */
    private fun crearParteImagen(uri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val archivoTemporal = File.createTempFile("perfume_", ".jpg", cacheDir)
            FileOutputStream(archivoTemporal).use { salida ->
                inputStream.copyTo(salida)
            }
            val tipo = contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = archivoTemporal.asRequestBody(tipo.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("imagen", archivoTemporal.name, requestBody)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val EXTRA_PERFUME_ID = "extra_perfume_id"
    }
}
