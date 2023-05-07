package com.example.myapplication

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import com.example.myapplication.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.myapplication.models.Usuario
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {
    //variables
    private lateinit var binding: ActivityRegisterBinding
    private var firebaseAuth: FirebaseAuth? = null
    var SELECT_PICTURE = 200
    var CAMERA_REQUEST = 100
    var myRef: DatabaseReference? = null
    var nUser = Usuario()
    var fotoS: String? = null
    var database = FirebaseDatabase.getInstance()

    //on create
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        //binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        firebaseAuth = FirebaseAuth.getInstance()
        //boton de registro
        ButtonRegisterActivity()
        //boton para la foto
        ProfilePhoto()
    }

    //funcion para elegir la foto de perfil
    fun ProfilePhoto() {
        //listener del imagebutton
        binding.fotodeperfil.setOnClickListener { v: View? ->
            //array con las opciones para sacar o elegir una foto
            val options = arrayOf<CharSequence>("Tomar foto", "Elegir de galeria", "Cancelar")
            //armar la alerta para elegir opcion
            val builder = AlertDialog.Builder(this@RegisterActivity)
            builder.setTitle("Elige una opcion")
            builder.setItems(options) { dialog: DialogInterface, item: Int ->
                //en caso de elegir tomar foto
                if (options[item] == "Tomar foto") {
                    //revisar permisos y pedirlos
                    if (ContextCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@RegisterActivity,
                            arrayOf(Manifest.permission.CAMERA),
                            CAMERA_REQUEST
                        )
                    } else { // si se tienen ir a la camara y obtener la foto
                        if (checkAndRequestPermissions()) {
                            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            startActivityForResult(intent, CAMERA_REQUEST)
                            //cambiar la foto a la foto tomada
                            binding!!.fotodeperfil.setImageURI(Uri.parse(MediaStore.ACTION_IMAGE_CAPTURE))
                        }
                    }
                    //si se elige la galeria
                } else if (options[item] == "Elegir de galeria") {
                    //revisar los permisos de storage
                    if (checkAndRequestPermissionsStorage()) {
                        //seleccionar imagen
                        imageChooser()
                    } else { //mostrar mensaje
                        Toast.makeText(this, "No se puede acceder a la galeria", Toast.LENGTH_SHORT)
                            .show()
                    }
                    //salir de la alerta
                } else if (options[item] == "Cancelar") {
                    dialog.dismiss()
                }
            }
            builder.show()
        }
    }

    // funcion para los resultados de la actividad de las fotos
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // si se uso la camara
        if (requestCode == 100) {
            // definir el formato de envio a la base de datos y girar la imagen para que este derecha
            var pic = data?.getParcelableExtra<Bitmap>("data")
            binding!!.fotodeperfil.setImageBitmap(pic)
            binding.fotodeperfil.rotation = 90f
            val image = data!!.extras!!["data"] as Bitmap?
            val byteArrayOutputStream = ByteArrayOutputStream()
            image!!.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            fotoS = Base64.encodeToString(byteArray, Base64.DEFAULT)
            binding!!.fotodeperfil.setImageBitmap(image)
        } else if (requestCode == 200) { // en caso de haver usado la galeria
            // definir el formato de envio a la base de datos y girar la imagen para que este derecha
            binding!!.fotodeperfil.setImageURI(data?.data)
            binding.fotodeperfil.rotation = 360f
            val selectedImageUri = data!!.data
            val img = MediaStore.Images.Media.getBitmap(
                this.contentResolver,
                selectedImageUri
            ) as Bitmap
            val byteArrayOutputStream = ByteArrayOutputStream()
            img.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            fotoS = Base64.encodeToString(byteArray, Base64.DEFAULT)
        }

    }

    // revisar si se poseen los servicios
    private fun checkAndRequestPermissions(): Boolean {
        val permissionCamera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }

    // funcion para seleccionar una foto en la galeria
    fun imageChooser() {
        val i = Intent()
        i.type = "image/*"
        i.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(i, "Selecciona una foto"), SELECT_PICTURE)
    }

    //revisar los permisos de storage
    private fun checkAndRequestPermissionsStorage(): Boolean {
        val permissionWritestorage = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (permissionWritestorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                REQUEST_ID_MULTIPLE_PERMISSIONS
            )
            return false
        }
        return true
    }

    //funcion de boton de registro
    fun ButtonRegisterActivity() {
        //listener
        binding.RegisterRBTN.setOnClickListener {
            //extraer los campos llenados
            val intent = Intent(this, MapActivity::class.java)
            val email = binding.EmailText.text.toString()
            val pass = binding.PasswordText.text.toString()
            val nombre = binding!!.editTextName.text.toString()
            val apellido = binding!!.editTextLast.text.toString()
            val numerodeidentificacion = binding!!.editTextNumber.text.toString()
            //validar que el email tenga elementos de un correo electronico
            if (email.isNotEmpty() && email.contains("@") && email.contains(".")) {
                //validar que la contrasena sea de minimo 6 caracteres (ya que es el minimo que pide la autenticacion de firebase)
                if (pass.isNotEmpty() && pass.length > 6) {
                    // revisar que los demas campos no se encuentren vacios asi no se producen errores en otras pantallas
                    if (nombre.isNotEmpty() && apellido.isNotEmpty() && numerodeidentificacion.isNotEmpty() && fotoS != null) {
                        createFirebaseAuthUser(email, pass)
                    } else {// mensajes de error
                        Toast.makeText(this, "Faltan Campos por llenar", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(this, "Contrasena invalida", Toast.LENGTH_SHORT)
                        .show()
                }

            } else {
                Toast.makeText(this, "Correo invalido", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // crear un usario de autenticacion de firebase
    private fun createFirebaseAuthUser(email: String, password: String) {
        firebaseAuth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task: Task<AuthResult?> ->
                // si se logra crear el usuario se guarda la demas informacion
                if (task.isSuccessful) {
                    saveUser()
                }
            }
    }

    //crear un objeto usuario para subirlo a la base de datos
    private fun createUserObject(): Usuario {
        nUser.nombre = binding!!.editTextName.text.toString()
        nUser.apellido = binding!!.editTextLast.text.toString()
        nUser.correo = binding!!.EmailText.text.toString()
        nUser.fotodeperfil = fotoS
        nUser.numerodeidentificacion = binding!!.editTextNumber.text.toString()
        return nUser
    }

    // guardar el usuario en la base de datos
    private fun saveUser() {
        val Client = createUserObject()
        myRef = database.getReference(PATH_USERS + firebaseAuth!!.currentUser!!.uid)
        myRef!!.setValue(Client).addOnCompleteListener { task: Task<Void?> ->
            if (task.isSuccessful) {
                // si se logra guardar el usuario iniciar la activida de mapa
                startActivity(Intent(applicationContext, MapActivity::class.java))
                finish()
            } else {
                Toast.makeText(applicationContext, "Error al registrar usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    //constantes globales
    companion object {
        const val PATH_USERS = "users/"
        const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
    }
}