package com.rfragancias.app.network

import com.rfragancias.app.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ---------- Auth ----------
    // Un solo endpoint de login para cliente y admin: el backend detecta
    // el rol y lo devuelve en la respuesta (ver AuthResponse.rol).
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("api/auth/registro")
    suspend fun registro(@Body body: RegistroRequest): Response<AuthResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<OkResponse>

    @GET("api/auth/session")
    suspend fun session(): Response<SessionResponse>

    // ---------- Perfumes (catálogo público) ----------
    @GET("api/perfumes")
    suspend fun getPerfumes(
        @Query("genero") genero: String? = null,
        @Query("buscar") buscar: String? = null
    ): Response<List<Perfume>>

    @GET("api/perfumes/{id}")
    suspend fun getPerfume(@Path("id") id: Int): Response<Perfume>

    // ---------- Perfumes (admin) ----------
    @GET("api/perfumes/admin/todos")
    suspend fun getPerfumesAdmin(): Response<List<Perfume>>

    @Multipart
    @POST("api/perfumes")
    suspend fun crearPerfume(
        @Part("nombre") nombre: RequestBody,
        @Part("disenador") disenador: RequestBody?,
        @Part("genero") genero: RequestBody,
        @Part("notas") notas: RequestBody?,
        @Part("precio") precio: RequestBody,
        @Part("gramos_disponibles") gramos: RequestBody,
        @Part imagen: MultipartBody.Part?
    ): Response<Perfume>

    @Multipart
    @PUT("api/perfumes/{id}")
    suspend fun actualizarPerfume(
        @Path("id") id: Int,
        @Part("nombre") nombre: RequestBody,
        @Part("disenador") disenador: RequestBody?,
        @Part("genero") genero: RequestBody,
        @Part("notas") notas: RequestBody?,
        @Part("precio") precio: RequestBody,
        @Part("gramos_disponibles") gramos: RequestBody,
        @Part("activo") activo: RequestBody,
        @Part imagen: MultipartBody.Part?
    ): Response<Perfume>

    @DELETE("api/perfumes/{id}")
    suspend fun eliminarPerfume(@Path("id") id: Int): Response<OkResponse>

    // ---------- Clientes ----------
    @GET("api/clientes")
    suspend fun getClientes(): Response<List<Cliente>>

    @GET("api/clientes/{id}/historial")
    suspend fun getHistorialCliente(@Path("id") id: Int): Response<HistorialCliente>

    @POST("api/clientes")
    suspend fun crearCliente(@Body body: Map<String, String?>): Response<Cliente>

    @PUT("api/clientes/{id}")
    suspend fun actualizarCliente(@Path("id") id: Int, @Body body: Map<String, String?>): Response<Cliente>

    @DELETE("api/clientes/{id}")
    suspend fun eliminarCliente(@Path("id") id: Int): Response<OkResponse>

    @POST("api/clientes/{id}/abonos")
    suspend fun registrarAbono(@Path("id") id: Int, @Body body: Map<String, String>): Response<Cliente>

    // ---------- Ventas ----------
    @GET("api/ventas")
    suspend fun getVentas(): Response<List<Venta>>

    @POST("api/ventas")
    suspend fun crearVenta(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Venta>

    @PUT("api/ventas/{id}")
    suspend fun actualizarVenta(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Venta>

    @DELETE("api/ventas/{id}")
    suspend fun eliminarVenta(@Path("id") id: Int): Response<OkResponse>

    // ---------- Pedidos de materia prima ----------
    @GET("api/pedidos")
    suspend fun getPedidos(): Response<List<Pedido>>

    @POST("api/pedidos")
    suspend fun crearPedido(@Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Pedido>

    @PUT("api/pedidos/{id}")
    suspend fun actualizarPedido(@Path("id") id: Int, @Body body: Map<String, @JvmSuppressWildcards Any?>): Response<Pedido>

    @DELETE("api/pedidos/{id}")
    suspend fun eliminarPedido(@Path("id") id: Int): Response<OkResponse>

    // ---------- Dashboard ----------
    @GET("api/dashboard/resumen")
    suspend fun getResumenDashboard(): Response<DashboardResumen>
}
