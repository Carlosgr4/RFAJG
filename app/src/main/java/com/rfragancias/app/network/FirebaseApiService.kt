package com.rfragancias.app.network

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.rfragancias.app.BuildConfig
import com.rfragancias.app.model.*
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Implementación de [ApiService] que guarda y lee TODO en Firebase:
 *
 *  - Firebase Authentication → inicio de sesión y registro
 *  - Cloud Firestore         → perfumes, clientes, ventas, abonos, pedidos
 *  - Firebase Storage        → fotos de los perfumes
 *
 * Como implementa la misma interfaz que usaba Retrofit, las pantallas de
 * la app no cambian: siguen llamando a `ApiClient.api.getPerfumes()`,
 * `ApiClient.api.crearVenta(...)`, etc., y reciben un `Response<T>`.
 *
 * Colecciones en Firestore (los IDs numéricos se generan con un contador
 * en la colección "contadores", para que los modelos sigan usando Int):
 *
 *   perfumes/{id}   clientes/{id}   ventas/{id}   abonos/{id}
 *   pedidos/{id}    usuarios/{uid}  contadores/{coleccion}
 */
object FirebaseApiService : ApiService {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage get() = FirebaseStorage.getInstance()

    private const val COL_PERFUMES = "perfumes"
    private const val COL_CLIENTES = "clientes"
    private const val COL_VENTAS = "ventas"
    private const val COL_ABONOS = "abonos"
    private const val COL_PEDIDOS = "pedidos"
    private const val COL_USUARIOS = "usuarios"
    private const val COL_CONTADORES = "contadores"

    /** Gramos que equivalen al 100 % de stock de un perfume (para la barra de stock). */
    private const val GRAMOS_STOCK_COMPLETO = 100.0

    /** Color de fondo de la app (@color/bg, #0D0C0A) para rellenar fotos sin fondo. */
    private const val COLOR_FONDO_FOTO = 0xFF0D0C0A.toInt()

    private val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("es", "CO"))

    // =====================================================================
    // Auth
    // =====================================================================

    override suspend fun login(body: LoginRequest): Response<AuthResponse> {
        // El administrador puede escribir solo "admin"; los clientes usan su correo.
        val email = if (body.usuario.contains("@")) body.usuario.trim()
        else "${body.usuario.trim()}@${BuildConfig.ADMIN_EMAIL.substringAfter("@")}"

        return try {
            val resultado = auth.signInWithEmailAndPassword(email, body.clave).await()
            val user = resultado.user ?: return respuestaError(401)
            Response.success(perfilDeUsuario(user.uid, user.email, user.displayName))
        } catch (e: FirebaseAuthInvalidUserException) {
            respuestaError(401)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            respuestaError(401)
        }
    }

    override suspend fun registro(body: RegistroRequest): Response<AuthResponse> {
        return try {
            val resultado = auth.createUserWithEmailAndPassword(body.email.trim(), body.password).await()
            val user = resultado.user ?: return respuestaError(500)

            val clienteId = siguienteId(COL_CLIENTES)
            db.collection(COL_CLIENTES).document(clienteId.toString()).set(
                mapOf(
                    "id" to clienteId,
                    "nombre" to body.nombre,
                    "telefono" to body.telefono,
                    "email" to body.email.trim(),
                    "notas" to null,
                    "uid" to user.uid,
                    "creado_en" to Timestamp.now()
                )
            ).await()

            db.collection(COL_USUARIOS).document(user.uid).set(
                mapOf("rol" to ROL_CLIENTE, "nombre" to body.nombre, "clienteId" to clienteId, "email" to body.email.trim())
            ).await()

            Response.success(AuthResponse(ok = true, rol = ROL_CLIENTE, nombre = body.nombre, clienteId = clienteId))
        } catch (e: FirebaseAuthUserCollisionException) {
            respuestaError(409) // el correo ya está registrado
        }
    }

    override suspend fun logout(): Response<OkResponse> {
        auth.signOut()
        return Response.success(OkResponse(ok = true))
    }

    override suspend fun session(): Response<SessionResponse> {
        val user = auth.currentUser
            ?: return Response.success(SessionResponse(autenticado = false))
        val perfil = perfilDeUsuario(user.uid, user.email, user.displayName)
        return Response.success(
            SessionResponse(autenticado = true, rol = perfil.rol, nombre = perfil.nombre, clienteId = perfil.clienteId)
        )
    }

    /**
     * Lee (o crea la primera vez) el documento usuarios/{uid} con el rol.
     * El administrador es la cuenta cuyo correo es BuildConfig.ADMIN_EMAIL.
     */
    private suspend fun perfilDeUsuario(uid: String, email: String?, nombreAuth: String?): AuthResponse {
        val esAdmin = email.equals(BuildConfig.ADMIN_EMAIL, ignoreCase = true)
        val ref = db.collection(COL_USUARIOS).document(uid)
        val doc = ref.get().await()

        if (doc.exists()) {
            return AuthResponse(
                ok = true,
                rol = if (esAdmin) ROL_ADMIN else (doc.getString("rol") ?: ROL_CLIENTE),
                nombre = doc.getString("nombre") ?: nombreAuth,
                clienteId = doc.getLong("clienteId")?.toInt()
            )
        }

        if (esAdmin) {
            ref.set(mapOf("rol" to ROL_ADMIN, "nombre" to "Administrador", "email" to email)).await()
            return AuthResponse(ok = true, rol = ROL_ADMIN, nombre = "Administrador")
        }

        // Cuenta creada desde la consola de Firebase sin pasar por el registro:
        // la damos de alta como cliente.
        val nombre = nombreAuth ?: email?.substringBefore("@") ?: "Cliente"
        val clienteId = siguienteId(COL_CLIENTES)
        db.collection(COL_CLIENTES).document(clienteId.toString()).set(
            mapOf("id" to clienteId, "nombre" to nombre, "email" to email, "uid" to uid, "creado_en" to Timestamp.now())
        ).await()
        ref.set(mapOf("rol" to ROL_CLIENTE, "nombre" to nombre, "clienteId" to clienteId, "email" to email)).await()
        return AuthResponse(ok = true, rol = ROL_CLIENTE, nombre = nombre, clienteId = clienteId)
    }

    // =====================================================================
    // Perfumes
    // =====================================================================

    override suspend fun getPerfumes(genero: String?, buscar: String?): Response<List<Perfume>> {
        val texto = buscar?.trim()?.lowercase().orEmpty()
        val lista = todosLosPerfumes()
            .filter { it.activo }
            .filter { genero.isNullOrBlank() || it.genero.equals(genero, ignoreCase = true) }
            .filter {
                texto.isEmpty() ||
                    it.nombre.lowercase().contains(texto) ||
                    it.disenador.orEmpty().lowercase().contains(texto) ||
                    it.notas.orEmpty().lowercase().contains(texto)
            }
        return Response.success(lista)
    }

    override suspend fun getPerfume(id: Int): Response<Perfume> {
        val doc = db.collection(COL_PERFUMES).document(id.toString()).get().await()
        return if (doc.exists()) Response.success(doc.aPerfume()) else respuestaError(404)
    }

    override suspend fun getPerfumesAdmin(): Response<List<Perfume>> =
        Response.success(todosLosPerfumes())

    override suspend fun crearPerfume(
        nombre: RequestBody,
        disenador: RequestBody?,
        genero: RequestBody,
        notas: RequestBody?,
        precio: RequestBody,
        gramos: RequestBody,
        imagen: MultipartBody.Part?
    ): Response<Perfume> {
        val datos = mutableMapOf<String, Any?>(
            "nombre" to nombre.texto(),
            "disenador" to disenador?.texto()?.ifBlank { null },
            "genero" to genero.texto(),
            "notas" to notas?.texto()?.ifBlank { null },
            "precio" to (precio.texto().toDoubleOrNull() ?: 0.0),
            "gramos_disponibles" to (gramos.texto().toDoubleOrNull() ?: 0.0),
            "activo" to true,
            "creado_en" to Timestamp.now()
        )
        // El número y el perfume se guardan juntos: si Firebase rechaza el
        // guardado, no se "gasta" un número de perfume.
        val id = crearConIdNuevo(COL_PERFUMES, datos)
        val ref = db.collection(COL_PERFUMES).document(id.toString())

        // La foto se sube después; si falla, el perfume queda guardado sin foto.
        imagen?.let { parte ->
            subirImagen(id, parte)?.let { url ->
                ref.set(mapOf("imagenUrl" to url), SetOptions.merge()).await()
            }
        }
        return Response.success(ref.get().await().aPerfume())
    }

    override suspend fun actualizarPerfume(
        id: Int,
        nombre: RequestBody,
        disenador: RequestBody?,
        genero: RequestBody,
        notas: RequestBody?,
        precio: RequestBody,
        gramos: RequestBody,
        activo: RequestBody,
        imagen: MultipartBody.Part?
    ): Response<Perfume> {
        val ref = db.collection(COL_PERFUMES).document(id.toString())
        if (!ref.get().await().exists()) return respuestaError(404)

        val datos = mutableMapOf<String, Any?>(
            "nombre" to nombre.texto(),
            "disenador" to disenador?.texto()?.ifBlank { null },
            "genero" to genero.texto(),
            "notas" to notas?.texto()?.ifBlank { null },
            "precio" to (precio.texto().toDoubleOrNull() ?: 0.0),
            "gramos_disponibles" to (gramos.texto().toDoubleOrNull() ?: 0.0),
            "activo" to activo.texto().toBoolean()
        )
        imagen?.let { parte -> subirImagen(id, parte)?.let { datos["imagenUrl"] = it } }

        ref.set(datos, SetOptions.merge()).await()
        return Response.success(ref.get().await().aPerfume())
    }

    override suspend fun eliminarPerfume(id: Int): Response<OkResponse> {
        db.collection(COL_PERFUMES).document(id.toString()).delete().await()
        try {
            storage.reference.child("perfumes/$id.jpg").delete().await()
        } catch (_: Exception) {
            // El perfume no tenía foto: no pasa nada.
        }
        return Response.success(OkResponse(ok = true))
    }

    private suspend fun todosLosPerfumes(): List<Perfume> =
        db.collection(COL_PERFUMES).get().await().documents
            .map { it.aPerfume() }
            .sortedBy { it.nombre.lowercase() }

    /**
     * Guarda la foto del perfume. Como el plan gratuito de Firebase (Spark) no
     * incluye Storage, la foto se reduce (máx. 800 px), se comprime en JPEG y
     * se guarda dentro del propio documento de Firestore como "data URL".
     * Glide sabe mostrar ese formato directamente.
     */
    private suspend fun subirImagen(perfumeId: Int, parte: MultipartBody.Part): String? =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            try {
                imagenComoDataUrl(parte)
            } catch (e: Exception) {
                android.util.Log.w("FirebaseApiService", "No se pudo procesar la foto del perfume $perfumeId", e)
                null
            }
        }

    private fun imagenComoDataUrl(parte: MultipartBody.Part): String? {
        val buffer = Buffer()
        parte.body.writeTo(buffer)
        val bytes = buffer.readByteArray()

        // 1) Leer solo el tamaño para decodificar la imagen ya reducida.
        val limites = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        val ladoMax = 800
        var muestreo = 1
        while (limites.outWidth / (muestreo * 2) >= ladoMax || limites.outHeight / (muestreo * 2) >= ladoMax) {
            muestreo *= 2
        }
        val opciones = android.graphics.BitmapFactory.Options().apply { inSampleSize = muestreo }
        var bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones) ?: return null

        // 2) Ajustar al tamaño máximo conservando la proporción.
        val escala = ladoMax.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (escala < 1f) {
            bitmap = android.graphics.Bitmap.createScaledBitmap(
                bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true
            )
        }

        // 3) JPEG no admite transparencia: las zonas transparentes (fotos PNG
        //    sin fondo) se pintan con el color de fondo de la app en vez de negro.
        val conFondo = android.graphics.Bitmap.createBitmap(
            bitmap.width, bitmap.height, android.graphics.Bitmap.Config.ARGB_8888
        )
        android.graphics.Canvas(conFondo).apply {
            drawColor(COLOR_FONDO_FOTO)
            drawBitmap(bitmap, 0f, 0f, null)
        }
        bitmap = conFondo

        // 4) Comprimir hasta que quepa holgadamente en un documento de Firestore.
        var calidad = 80
        var jpeg: ByteArray
        do {
            val salida = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, calidad, salida)
            jpeg = salida.toByteArray()
            calidad -= 10
        } while (jpeg.size > 400_000 && calidad > 30)

        return "data:image/jpeg;base64," + android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
    }

    private suspend fun subirImagenAStorage(perfumeId: Int, parte: MultipartBody.Part): String {
        val buffer = Buffer()
        parte.body.writeTo(buffer)
        val ref = storage.reference.child("perfumes/$perfumeId.jpg")
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(parte.body.contentType()?.toString() ?: "image/jpeg")
            .build()
        ref.putBytes(buffer.readByteArray(), metadata).await()
        return ref.downloadUrl.await().toString()
    }

    // =====================================================================
    // Clientes y abonos
    // =====================================================================

    override suspend fun getClientes(): Response<List<Cliente>> {
        val ventas = todasLasVentas()
        val abonos = todosLosAbonos()
        val clientes = db.collection(COL_CLIENTES).get().await().documents
            .map { it.aCliente(ventas, abonos) }
            .sortedBy { it.nombre.lowercase() }
        return Response.success(clientes)
    }

    override suspend fun getHistorialCliente(id: Int): Response<HistorialCliente> {
        val doc = db.collection(COL_CLIENTES).document(id.toString()).get().await()
        if (!doc.exists()) return respuestaError(404)
        val ventas = todasLasVentas()
        val abonos = todosLosAbonos()
        return Response.success(
            HistorialCliente(
                cliente = doc.aCliente(ventas, abonos),
                ventas = ventas.filter { it.clienteId == id },
                abonos = abonos.filter { it.clienteId == id }
            )
        )
    }

    override suspend fun crearCliente(body: Map<String, String?>): Response<Cliente> {
        val id = siguienteId(COL_CLIENTES)
        val ref = db.collection(COL_CLIENTES).document(id.toString())
        ref.set(
            mapOf(
                "id" to id,
                "nombre" to body["nombre"].orEmpty(),
                "telefono" to body["telefono"]?.ifBlank { null },
                "email" to body["email"]?.ifBlank { null },
                "notas" to body["notas"]?.ifBlank { null },
                "creado_en" to Timestamp.now()
            )
        ).await()
        return Response.success(ref.get().await().aCliente(emptyList(), emptyList()))
    }

    override suspend fun actualizarCliente(id: Int, body: Map<String, String?>): Response<Cliente> {
        val ref = db.collection(COL_CLIENTES).document(id.toString())
        if (!ref.get().await().exists()) return respuestaError(404)
        val cambios = body.filterKeys { it in setOf("nombre", "telefono", "email", "notas") }
        ref.set(cambios, SetOptions.merge()).await()
        return Response.success(ref.get().await().aCliente(todasLasVentas(), todosLosAbonos()))
    }

    override suspend fun eliminarCliente(id: Int): Response<OkResponse> {
        db.collection(COL_CLIENTES).document(id.toString()).delete().await()
        return Response.success(OkResponse(ok = true))
    }

    override suspend fun registrarAbono(id: Int, body: Map<String, String>): Response<Cliente> {
        val cliente = db.collection(COL_CLIENTES).document(id.toString()).get().await()
        if (!cliente.exists()) return respuestaError(404)
        val monto = body["monto"]?.replace(",", ".")?.toDoubleOrNull() ?: return respuestaError(400)

        val abonoId = siguienteId(COL_ABONOS)
        db.collection(COL_ABONOS).document(abonoId.toString()).set(
            mapOf(
                "id" to abonoId,
                "cliente_id" to id,
                "monto" to monto,
                "nota" to body["nota"]?.ifBlank { null },
                "fecha" to Timestamp.now()
            )
        ).await()
        return Response.success(cliente.aCliente(todasLasVentas(), todosLosAbonos()))
    }

    // =====================================================================
    // Ventas
    // =====================================================================

    override suspend fun getVentas(): Response<List<Venta>> = Response.success(todasLasVentas())

    override suspend fun crearVenta(body: Map<String, Any?>): Response<Venta> {
        val id = siguienteId(COL_VENTAS)
        val clienteId = (body["cliente_id"] as? Number)?.toInt()
        val perfumeId = (body["perfume_id"] as? Number)?.toInt()

        // Guardamos también los nombres para listar ventas sin consultas extra.
        val clienteNombre = clienteId?.let {
            db.collection(COL_CLIENTES).document(it.toString()).get().await().getString("nombre")
        }
        val perfumeNombre = perfumeId?.let {
            db.collection(COL_PERFUMES).document(it.toString()).get().await().getString("nombre")
        }

        val ref = db.collection(COL_VENTAS).document(id.toString())
        ref.set(
            mapOf(
                "id" to id,
                "cliente_id" to clienteId,
                "perfume_id" to perfumeId,
                "cliente_nombre" to clienteNombre,
                "perfume_nombre" to perfumeNombre,
                "cantidad_ml" to ((body["cantidad_ml"] as? Number)?.toDouble() ?: 0.0),
                "total" to ((body["total"] as? Number)?.toDouble() ?: 0.0),
                "estado" to (body["estado"] as? String ?: "Pendiente"),
                "notas" to (body["notas"] as? String)?.ifBlank { null },
                "fecha" to Timestamp.now()
            )
        ).await()
        return Response.success(ref.get().await().aVenta())
    }

    override suspend fun actualizarVenta(id: Int, body: Map<String, Any?>): Response<Venta> {
        val ref = db.collection(COL_VENTAS).document(id.toString())
        if (!ref.get().await().exists()) return respuestaError(404)
        ref.set(body.filterKeys { it != "id" }, SetOptions.merge()).await()
        return Response.success(ref.get().await().aVenta())
    }

    override suspend fun eliminarVenta(id: Int): Response<OkResponse> {
        db.collection(COL_VENTAS).document(id.toString()).delete().await()
        return Response.success(OkResponse(ok = true))
    }

    private suspend fun todasLasVentas(): List<Venta> =
        db.collection(COL_VENTAS).get().await().documents
            .sortedByDescending { it.getTimestamp("fecha") ?: Timestamp(0, 0) }
            .map { it.aVenta() }

    private suspend fun todosLosAbonos(): List<Abono> =
        db.collection(COL_ABONOS).get().await().documents
            .sortedByDescending { it.getTimestamp("fecha") ?: Timestamp(0, 0) }
            .map { it.aAbono() }

    // =====================================================================
    // Pedidos de materia prima
    // =====================================================================

    override suspend fun getPedidos(): Response<List<Pedido>> {
        val pedidos = db.collection(COL_PEDIDOS).get().await().documents
            .sortedByDescending { it.getTimestamp("fecha") ?: Timestamp(0, 0) }
            .map { it.aPedido() }
        return Response.success(pedidos)
    }

    override suspend fun crearPedido(body: Map<String, Any?>): Response<Pedido> {
        val id = siguienteId(COL_PEDIDOS)
        val ref = db.collection(COL_PEDIDOS).document(id.toString())
        ref.set(
            mapOf(
                "id" to id,
                "insumo" to (body["insumo"] as? String).orEmpty(),
                "proveedor" to (body["proveedor"] as? String)?.ifBlank { null },
                "cantidad_gramos" to ((body["cantidad_gramos"] as? Number)?.toDouble() ?: 0.0),
                "costo" to ((body["costo"] as? Number)?.toDouble() ?: 0.0),
                "estado" to (body["estado"] as? String ?: "Pendiente"),
                "fecha" to Timestamp.now()
            )
        ).await()
        return Response.success(ref.get().await().aPedido())
    }

    override suspend fun actualizarPedido(id: Int, body: Map<String, Any?>): Response<Pedido> {
        val ref = db.collection(COL_PEDIDOS).document(id.toString())
        if (!ref.get().await().exists()) return respuestaError(404)
        ref.set(body.filterKeys { it != "id" }, SetOptions.merge()).await()
        return Response.success(ref.get().await().aPedido())
    }

    override suspend fun eliminarPedido(id: Int): Response<OkResponse> {
        db.collection(COL_PEDIDOS).document(id.toString()).delete().await()
        return Response.success(OkResponse(ok = true))
    }

    // =====================================================================
    // Dashboard
    // =====================================================================

    override suspend fun getResumenDashboard(): Response<DashboardResumen> {
        val perfumes = todosLosPerfumes()
        val ventas = todasLasVentas()
        val abonos = todosLosAbonos()
        val clientesDocs = db.collection(COL_CLIENTES).get().await().documents
        val pedidos = db.collection(COL_PEDIDOS).get().await().documents

        val inicioMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        val ventasMesDocs = db.collection(COL_VENTAS)
            .whereGreaterThanOrEqualTo("fecha", Timestamp(inicioMes))
            .get().await().documents

        val deudaTotal = clientesDocs.sumOf { it.aCliente(ventas, abonos).deuda }

        return Response.success(
            DashboardResumen(
                totalPerfumes = perfumes.size,
                totalClientes = clientesDocs.size,
                ventasMes = ventasMesDocs
                    .filter { it.getString("estado") != ESTADO_ANULADA }
                    .sumOf { it.getDouble("total") ?: 0.0 },
                deudaTotal = deudaTotal,
                pedidosPendientes = pedidos.count { it.getString("estado") == "Pendiente" },
                stockBajo = perfumes.count { it.nivelStock == "bajo" }
            )
        )
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private const val ESTADO_ANULADA = "Anulada"
    private const val ESTADO_PAGADA = "Pagada"

    /** Genera un ID numérico consecutivo por colección (1, 2, 3…) de forma atómica. */
    private suspend fun siguienteId(coleccion: String): Int {
        val ref = db.collection(COL_CONTADORES).document(coleccion)
        return db.runTransaction { tx ->
            val actual = tx.get(ref).getLong("ultimo") ?: 0L
            val siguiente = actual + 1
            tx.set(ref, mapOf("ultimo" to siguiente, "actualizado" to FieldValue.serverTimestamp()))
            siguiente
        }.await().toInt()
    }

    /**
     * Crea un documento con el siguiente ID consecutivo de la colección, en
     * una sola transacción (contador + documento). Devuelve el ID asignado.
     */
    private suspend fun crearConIdNuevo(coleccion: String, datos: Map<String, Any?>): Int {
        val contador = db.collection(COL_CONTADORES).document(coleccion)
        return db.runTransaction { tx ->
            val siguiente = (tx.get(contador).getLong("ultimo") ?: 0L) + 1
            tx.set(contador, mapOf("ultimo" to siguiente, "actualizado" to FieldValue.serverTimestamp()))
            tx.set(db.collection(coleccion).document(siguiente.toString()), datos + ("id" to siguiente))
            siguiente
        }.await().toInt()
    }

    private fun RequestBody.texto(): String {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readUtf8().trim()
    }

    private fun <T> respuestaError(codigo: Int): Response<T> =
        Response.error(codigo, "{}".toResponseBody("application/json".toMediaTypeOrNull()))

    private fun DocumentSnapshot.idNumerico(): Int =
        getLong("id")?.toInt() ?: id.toIntOrNull() ?: 0

    private fun DocumentSnapshot.fechaTexto(campo: String): String? =
        getTimestamp(campo)?.toDate()?.let { formatoFecha.format(it) }
            ?: getString(campo)

    private fun DocumentSnapshot.aPerfume(): Perfume {
        val gramos = getDouble("gramos_disponibles") ?: 0.0
        val porcentaje = ((gramos / GRAMOS_STOCK_COMPLETO) * 100).toInt().coerceIn(0, 100)
        val nivel = when {
            porcentaje <= 20 -> "bajo"
            porcentaje <= 50 -> "medio"
            else -> "alto"
        }
        return Perfume(
            id = idNumerico(),
            nombre = getString("nombre").orEmpty(),
            disenador = getString("disenador"),
            genero = getString("genero") ?: "Unisex",
            notas = getString("notas"),
            precio = getDouble("precio") ?: 0.0,
            gramosDisponibles = gramos,
            activo = getBoolean("activo") ?: true,
            imagenUrl = getString("imagenUrl"),
            porcentajeStock = porcentaje,
            nivelStock = nivel,
            creadoEn = fechaTexto("creado_en")
        )
    }

    private fun DocumentSnapshot.aVenta() = Venta(
        id = idNumerico(),
        clienteId = getLong("cliente_id")?.toInt(),
        perfumeId = getLong("perfume_id")?.toInt(),
        clienteNombre = getString("cliente_nombre"),
        perfumeNombre = getString("perfume_nombre"),
        cantidadMl = getDouble("cantidad_ml") ?: 0.0,
        total = getDouble("total") ?: 0.0,
        estado = getString("estado") ?: "Pendiente",
        fecha = fechaTexto("fecha"),
        notas = getString("notas")
    )

    private fun DocumentSnapshot.aAbono() = Abono(
        id = idNumerico(),
        clienteId = getLong("cliente_id")?.toInt() ?: 0,
        monto = getDouble("monto") ?: 0.0,
        fecha = fechaTexto("fecha"),
        nota = getString("nota")
    )

    private fun DocumentSnapshot.aPedido() = Pedido(
        id = idNumerico(),
        insumo = getString("insumo").orEmpty(),
        proveedor = getString("proveedor"),
        cantidadGramos = getDouble("cantidad_gramos") ?: 0.0,
        costo = getDouble("costo") ?: 0.0,
        estado = getString("estado") ?: "Pendiente",
        fecha = fechaTexto("fecha")
    )

    /**
     * Arma el cliente con sus totales:
     * deuda = ventas pendientes (ni pagadas ni anuladas) − abonos.
     * [ventas] y [abonos] vienen ordenados del más reciente al más antiguo.
     */
    private fun DocumentSnapshot.aCliente(ventas: List<Venta>, abonos: List<Abono>): Cliente {
        val id = idNumerico()
        val ventasCliente = ventas.filter { it.clienteId == id && it.estado != ESTADO_ANULADA }
        val abonosCliente = abonos.filter { it.clienteId == id }
        val totalVentas = ventasCliente.sumOf { it.total }
        val totalAbonos = abonosCliente.sumOf { it.monto }
        // Solo las ventas pendientes generan deuda; las "Pagada" ya están saldadas.
        val totalPendiente = ventasCliente.filter { it.estado != ESTADO_PAGADA }.sumOf { it.total }
        return Cliente(
            id = id,
            nombre = getString("nombre").orEmpty(),
            telefono = getString("telefono"),
            email = getString("email"),
            notas = getString("notas"),
            creadoEn = fechaTexto("creado_en"),
            totalVentas = totalVentas,
            totalAbonos = totalAbonos,
            deuda = (totalPendiente - totalAbonos).coerceAtLeast(0.0),
            ultimoAbono = abonosCliente.firstOrNull()?.fecha,
            ultimoPerfume = ventasCliente.firstOrNull()?.perfumeNombre,
            fechaUltimaVenta = ventasCliente.firstOrNull()?.fecha,
            cantidadCompras = ventasCliente.size
        )
    }
}
