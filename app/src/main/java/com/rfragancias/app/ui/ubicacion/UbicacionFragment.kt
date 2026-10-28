package com.rfragancias.app.ui.ubicacion

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.rfragancias.app.BuildConfig
import com.rfragancias.app.R

/**
 * Pestaña "Ubicación": muestra el local de la perfumería en un mapa de
 * Google, con su dirección y botones para pedir la ruta.
 *
 * La dirección y las coordenadas se editan en res/values/strings.xml
 * (local_direccion, local_latitud, local_longitud).
 */
class UbicacionFragment : Fragment() {

    private val ubicacionLocal: LatLng
        get() = LatLng(
            getString(R.string.local_latitud).toDouble(),
            getString(R.string.local_longitud).toDouble()
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_ubicacion, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (BuildConfig.MAPS_KEY_CONFIGURADA) {
            val mapa = childFragmentManager.findFragmentById(R.id.contenedor_mapa) as? SupportMapFragment
                ?: SupportMapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.contenedor_mapa, it)
                        .commit()
                }
            mapa.getMapAsync { googleMap -> configurarMapa(googleMap) }
        } else {
            // Sin API key el SDK de Maps mostraría un recuadro gris; mejor avisar.
            view.findViewById<TextView>(R.id.texto_sin_mapa).visibility = View.VISIBLE
        }

        view.findViewById<View>(R.id.btn_como_llegar).setOnClickListener { abrirRuta() }
        view.findViewById<View>(R.id.btn_abrir_maps).setOnClickListener { abrirEnMaps() }
    }

    private fun configurarMapa(mapa: GoogleMap) {
        val posicion = ubicacionLocal
        mapa.addMarker(
            MarkerOptions()
                .position(posicion)
                .title(getString(R.string.local_nombre))
                .snippet(getString(R.string.local_direccion))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        )?.showInfoWindow()
        mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 17f))
        mapa.uiSettings.isZoomControlsEnabled = true
        mapa.uiSettings.isMapToolbarEnabled = true
    }

    /** Abre la navegación de Google Maps desde la ubicación del usuario hasta el local. */
    private fun abrirRuta() {
        val destino = "${ubicacionLocal.latitude},${ubicacionLocal.longitude}"
        val navegacion = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$destino"))
            .setPackage("com.google.android.apps.maps")
        abrirConRespaldo(
            navegacion,
            "https://www.google.com/maps/dir/?api=1&destination=$destino"
        )
    }

    /** Muestra el local como un punto en la app de Google Maps. */
    private fun abrirEnMaps() {
        val pos = ubicacionLocal
        val etiqueta = Uri.encode(getString(R.string.local_nombre))
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo:${pos.latitude},${pos.longitude}?q=${pos.latitude},${pos.longitude}($etiqueta)")
        )
        abrirConRespaldo(
            intent,
            "https://www.google.com/maps/search/?api=1&query=${pos.latitude},${pos.longitude}"
        )
    }

    /** Si el teléfono no tiene Google Maps instalado, abre la versión web. */
    private fun abrirConRespaldo(intent: Intent, urlWeb: String) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlWeb)))
        }
    }
}
