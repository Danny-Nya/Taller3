package com.example.myapplication

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.myapplication.databinding.ActivityMapBinding
import com.example.myapplication.models.Usuario
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.util.Objects

class MapActivity : FragmentActivity(), OnMapReadyCallback {
    //variables
    private var mMap: GoogleMap? = null
    private var mAuth: FirebaseAuth? = null
    var database = FirebaseDatabase.getInstance()
    var myRef: DatabaseReference? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationCallback: LocationCallback? = null
    var mCurrentLocation: Location? = null
    fun setmCurrentLocation(mCurrentLocation: Location?) {
        this.mCurrentLocation = mCurrentLocation
    }

    override fun onBackPressed() {}
    var Client: Usuario? = Usuario()
    var Client2: Usuario? = Usuario()
    var siguiendoa: String? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var currentLat = 0.0
    private var currentLong = 0.0

    //on create
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding
        val binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //inicializar las instancias del login
        mAuth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()
        //funciones de botones y switch
        Botonlogout(binding)
        BotonDisp(binding)
        CheckDisp(binding)
        //Revisar cambios en la base de datos
        Changes()
        //crear la fraccion para el mapa
        createFraction()
    }

    //funcion para revisar los cambios de estado de disponibilidad y realizar los toast
    fun Changes() {
        myRef = database.getReference(PATH_USERS)
        myRef!!.addValueEventListener(object : ValueEventListener {
            //en caso de un cambio en los datos realizar el toast
            override fun onDataChange(snapshot: DataSnapshot) {
                for (child in snapshot.children) {
                    val user = child.getValue(Usuario::class.java)
                    if (user != null) {
                        if (child.key != mAuth!!.currentUser!!.uid) {
                            if (user.isIsdisponible) {
                                toastDisponible(user)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //Funcion para poner el mapa en la pantalla
    fun createFraction() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    //funcion para el switch de disponibilidad y cambiar el estado de verdadero a falso en la base de datos
    fun CheckDisp(binding: ActivityMapBinding) {
        //listener para el switch
        binding.switch3.setOnClickListener { v: View ->
            Log.d("Switch", "Switch" + (v as Switch).isChecked)
            //si esta cheked cambiar esta disponible a verdadero
            if (v.isChecked) {
                myRef = database.getReference(
                    PATH_USERS + Objects.requireNonNull(
                        mAuth!!.currentUser
                    )!!.uid
                )
                myRef!!.database.getReference(
                    PATH_USERS + mAuth!!.currentUser!!
                        .uid
                ).get().addOnCompleteListener { task: Task<DataSnapshot> ->
                    if (task.isSuccessful) {
                        Client = task.result.getValue(Usuario::class.java)
                        Client!!.isIsdisponible = true
                        myRef!!.setValue(Client)
                    }
                }
            } else { //si no esta cheked cambiar esta disponible a falso
                myRef = database.getReference(
                    PATH_USERS + mAuth!!.currentUser!!
                        .uid
                )
                myRef!!.database.getReference(
                    PATH_USERS + mAuth!!.currentUser!!
                        .uid
                ).get().addOnCompleteListener { task: Task<DataSnapshot> ->
                    if (task.isSuccessful) {
                        Client = task.result.getValue(Usuario::class.java)
                        Client!!.isIsdisponible = false
                        myRef!!.setValue(Client)
                    }
                }
            }
        }
    }

    //funcion para pasar a la actividad de la lista de usuarios diponibles
    fun BotonDisp(binding: ActivityMapBinding) {
        binding.DisponiblesBTN.setOnClickListener { view: View? ->
            startActivity(Intent(applicationContext, DisponiblesActivity::class.java))
            finish()
        }
    }

    //funcion para cerrar sesion
    fun Botonlogout(binding: ActivityMapBinding) {
        //listener del boton
        binding.logoutBTN.setOnClickListener {
            try { //si se cliquea cerrar la sesion y parar las actualizaciones de localizacion en el mapa y regrezar al main
                stopLocationUpdates()
                mAuth!!.signOut()
                Thread.sleep(1000)
                startActivity(Intent(applicationContext, MainActivity::class.java))
                finish()
                finishActivity(1)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    //funcion cuando el mapa esta listo
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap!!.isBuildingsEnabled = true
        mMap!!.uiSettings.setAllGesturesEnabled(true)
        mMap!!.uiSettings.isCompassEnabled = true
        mMap!!.uiSettings.isMyLocationButtonEnabled = false
        //revisar si se poseen permisos
        if (checkPermissions()) {
            //funcion para sacar los puntos de los marcadores del JSON en la carpeta de assets
            loadMarkersjson()
            //Actualizar a la localizacion actual del usuario
            mLocationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (locationResult != null) {
                        for (location in locationResult.locations) {
                            setmCurrentLocation(location)
                            mCurrentLocation = location
                            currentLat = location.latitude
                            currentLong = location.longitude
                        }
                    }
                }
            }

            // Customize the map here
            myRef = database.getReference(
                PATH_USERS + mAuth!!.currentUser!!
                    .uid
            )
            myRef!!.database.getReference(
                PATH_USERS + mAuth!!.currentUser!!
                    .uid
            ).get().addOnCompleteListener { task: Task<DataSnapshot> ->
                if (task.isSuccessful) {
                    Client = task.result.getValue(Usuario::class.java)
                    val location = Location("locationA")
                    location.latitude = currentLat
                    location.longitude = currentLong
                    setmCurrentLocation(location)
                    mCurrentLocation = location
                    if (Client!!.latitud != null && Client!!.longitud != null) {
                        if (Client!!.latitud != currentLat && Client!!.longitud != currentLong) {
                            mMap!!.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL))
                            val clatlng = LatLng(currentLat, currentLong)
                            mMap!!.animateCamera(CameraUpdateFactory.newLatLng(clatlng))
                            mMap!!.moveCamera(
                                CameraUpdateFactory.newLatLng(
                                    LatLng(
                                        currentLat,
                                        currentLong
                                    )
                                )
                            )
                        } else {
                            Client!!.latitud = currentLat
                            Client!!.longitud = currentLong
                            myRef!!.setValue(Client)
                        }
                    }
                    if (Client!!.siguiendoa != null) {
                        if (!Client!!.siguiendoa.isEmpty()) {
                            siguiendoa = Client!!.siguiendoa
                            siguiendoa = Client!!.siguiendoa
                            myRef = database.getReference(PATH_USERS + siguiendoa)
                            myRef!!.addValueEventListener(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    Client2 = snapshot.getValue(Usuario::class.java)
                                    if (Client2 != null) {
                                        val sydney = LatLng(
                                            Client2!!.latitud, Client2!!.longitud
                                        )
                                        mMap!!.addMarker(
                                            MarkerOptions().position(sydney).title(Client2!!.nombre)
                                        )
                                        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(sydney))
                                        mMap!!.moveCamera(
                                            CameraUpdateFactory.zoomTo(
                                                INITIAL_ZOOM_LEVEL
                                            )
                                        )
                                        val location = Location("locationA")
                                        location.latitude = Client2!!.latitud
                                        location.longitude = Client2!!.longitud
                                        val distance = mCurrentLocation!!.distanceTo(location)
                                        Toast.makeText(
                                            applicationContext,
                                            "Distancia: $distance",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {}
                            })
                        }
                    }
                }
            }

        }
        mMap!!.isMyLocationEnabled = true
        mMap!!.isMyLocationEnabled
        val location2 = mMap?.myLocation
        if (location2 != null) {
            val latLng = LatLng(location2.latitude, location2.latitude)
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, INITIAL_ZOOM_LEVEL))
        }
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult != null) {
                    for (location in locationResult.locations) {
                        setmCurrentLocation(location)
                        mCurrentLocation = location
                        currentLat = location.latitude
                        currentLong = location.longitude
                        if (mAuth!!.currentUser == null) {
                            break
                        }
                        myRef = database.getReference(
                            PATH_USERS + mAuth!!.currentUser!!
                                .uid
                        )
                        myRef!!.database.getReference(
                            PATH_USERS + mAuth!!.currentUser!!
                                .uid
                        ).get().addOnCompleteListener { task: Task<DataSnapshot> ->
                            if (task.isSuccessful) {
                                Client = task.result.getValue(Usuario::class.java)
                                if (Client!!.latitud != null && Client!!.longitud != null) {
                                    if (Client!!.latitud != currentLat && Client!!.longitud != currentLong) {
                                        val locationA = Location("point A")
                                        locationA.latitude = mCurrentLocation!!.latitude
                                        locationA.longitude = mCurrentLocation!!.longitude
                                        val locationB = Location("point B")
                                        locationB.latitude = Client!!.latitud
                                        locationB.longitude = Client!!.longitud
                                        val distance = locationA.distanceTo(locationB)
                                        val distanceKm = distance / 1000
                                        if (distanceKm > 0.01) {
                                            mMap!!.moveCamera(
                                                CameraUpdateFactory.zoomTo(
                                                    INITIAL_ZOOM_LEVEL
                                                )
                                            )
                                            mMap!!.uiSettings.setAllGesturesEnabled(true)
                                            mMap!!.uiSettings.isCompassEnabled = true
                                            mMap!!.uiSettings.isMyLocationButtonEnabled = false
                                            val clatlng =
                                                LatLng(location.latitude, location.longitude)
                                            mMap!!.animateCamera(
                                                CameraUpdateFactory.newLatLng(
                                                    clatlng
                                                )
                                            )
                                            Client!!.latitud = currentLat
                                            Client!!.longitud = currentLong
                                            myRef!!.setValue(Client)
                                        }
                                    }
                                } else {
                                    Client!!.latitud = currentLat
                                    Client!!.longitud = currentLong
                                    myRef!!.setValue(Client)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (siguiendoa != null) {
            myRef = database.getReference(PATH_USERS + siguiendoa)
            myRef!!.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Client2 = snapshot.getValue(Usuario::class.java)
                    if (Client2 != null) {
                        val sydney = LatLng(
                            Client2!!.latitud, Client2!!.longitud
                        )
                        mMap!!.addMarker(
                            MarkerOptions().position(sydney).title(Client2!!.nombre)
                        )
                        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(sydney))
                        mMap!!.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL))
                        val location = Location("locationA")
                        location.latitude = Client2!!.latitud
                        location.longitude = Client2!!.longitud
                        val distance = mCurrentLocation!!.distanceTo(location)
                        Toast.makeText(
                            applicationContext,
                            "Distancia: $distance",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })

        }
        startLocationUpdates()
    }

    //Funcion para revisar los permisos de localizacion
    private fun checkPermissions(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            //en caso de no tener permisos se piden
            requestPermissions()
            false
        }
    }

    //funcion para pedir permisos
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    //funcion para sacar los elementos del JSON
    fun loadJSONFromAssetActivity(): String? {
        var json: String? = null
        try {
            val istream: InputStream = this.assets.open("locations.json")
            val size: Int = istream.available()
            val buffer = ByteArray(size)
            istream.read(buffer)
            istream.close()
            json = String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    //funcion para mostrar los puntos de JSON en el mapa
    fun loadMarkersjson() {
        val json = JSONObject(loadJSONFromAssetActivity())
        val loc = json.getJSONArray("locationsArray")
        for (i in 0 until loc.length()) {
            val jsonobj = loc.getJSONObject(i)
            val nombre = jsonobj.getString("name")
            val latitud = jsonobj.getDouble("latitude")
            val longitud = jsonobj.getDouble("longitude")
            val coordinates = LatLng(latitud, longitud)
            val marker: MarkerOptions = MarkerOptions().position(coordinates).title(nombre)
            mMap!!.addMarker(marker)
            mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 18f), 4000, null)
        }
    }

    @SuppressLint("MissingPermission")
    //funcion  para iniciar a hacer actualizaciones de localizacion del usuario
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient!!.requestLocationUpdates(
                mLocationRequest!!,
                mLocationCallback!!,
                null
            )
        }
    }

    //Funcion para detener las actualizaciones
    private fun stopLocationUpdates() {
        fusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
    }

    //Funcion para definir los intervalos de tiempo con los que se realizan las actualizaciones de la localizacion
    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create()
            .setInterval(3000)
            .setFastestInterval(500)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    }

    //Funcion para manejar los resultados de la peticion de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val LOCATION_PERMISSION_ID = 103
        if (requestCode == LOCATION_PERMISSION_ID) {
            //en caso de tener permizos se inicia el uso del gps y empieza a realizas actualizaciones
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Ya hay permiso para acceder a la localizacion",
                    Toast.LENGTH_LONG
                ).show()
                turnOnLocationAndStartUpdates()
            } else { //de lo contrario no se realiza nada
                Toast.makeText(this, "Permiso de ubicacion denegado", Toast.LENGTH_LONG).show()
            }
        }
    }

    //Funcion para iniciar actualizaciones y los marcadores
    private fun turnOnLocationAndStartUpdates() {
        //crear peticion para actualizaciones
        mLocationRequest = createLocationRequest()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(
            mLocationRequest!!
        )
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(this) { locationSettingsResponse: LocationSettingsResponse? ->
            mLocationCallback = object : LocationCallback() {
                @SuppressLint("MissingPermission")
                //funcion para poner el zoom en el mapa, dirigir la camara a la ultima localizacion del usuario y actualiza las coordenadas en el firebase
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        mMap!!.isMyLocationEnabled = true
                        mMap!!.moveCamera(CameraUpdateFactory.zoomTo(INITIAL_ZOOM_LEVEL))
                        mMap!!.uiSettings.setAllGesturesEnabled(true)
                        mMap!!.uiSettings.isCompassEnabled = true
                        mMap!!.uiSettings.isMyLocationButtonEnabled = false
                        val clatlng = LatLng(location.latitude, location.longitude)
                        mMap!!.animateCamera(CameraUpdateFactory.newLatLng(clatlng))
                        setmCurrentLocation(location)
                        mCurrentLocation = location
                        myRef = database.getReference(
                            PATH_USERS + mAuth!!.currentUser!!
                                .uid
                        )
                        myRef!!.database.getReference(
                            PATH_USERS + mAuth!!.currentUser!!
                                .uid
                        ).get().addOnCompleteListener { task: Task<DataSnapshot> ->
                            if (task.isSuccessful) {
                                //actualiza los datos en el firebase
                                Client = task.result.getValue(Usuario::class.java)
                                Client!!.latitud = mCurrentLocation!!.latitude
                                Client!!.longitud = mCurrentLocation!!.longitude
                                myRef!!.setValue(Client)
                            }
                        }
                    }
                }
            }
            //cargar los marcadores del mapa
            loadMarkersjson()
            //iniciar las actualizaciones de localizacion
            startLocationUpdates()
        }
        //en caso de un fallo se intenta resolver los conflictos con la localizacion
        task.addOnFailureListener(this) { e ->
            val statusCode = (e as ApiException).statusCode
            when (statusCode) {
                CommonStatusCodes.RESOLUTION_REQUIRED -> try {
                    val resolvable = e as ResolvableApiException
                    resolvable.startResolutionForResult(this@MapActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: SendIntentException) {
                }

                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
            }
        }
    }

    //Funcion que le da un toast al usuario con los usuarios disponibles
    private fun toastDisponible(u: Usuario) {
        val intent = Intent(this, DisponiblesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        Toast.makeText(applicationContext, u.nombre + " esta disponible", Toast.LENGTH_LONG).show()
    }

    //funcion para actuar respecto a la respuesta del usuario si el permiso es el mismo se inician las actulaizaciones de localizacion
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                startLocationUpdates()
            }
        }
    }

    //funcion para cuando se retorna a la actividad, revisa permisos y si estan activos comenza actualizaciones
    override fun onResume() {
        super.onResume()
        if (checkPermissions()) {
            turnOnLocationAndStartUpdates()
        }
    }

    //parar las actualizaciones si se pausa la actividad
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    public override fun onRestart() {
        super.onRestart()
    }

    //parar las actualizaciones si se destruye la actividad
    public override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    public override fun onStart() {
        super.onStart()
    }

    //parar las actualizaciones si se detiene la actividad
    public override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    //constantes globales
    companion object {
        const val REQUEST_CHECK_SETTINGS = 201
        private const val INITIAL_ZOOM_LEVEL = 14.5f
        const val PATH_USERS = "users/"
    }
}

