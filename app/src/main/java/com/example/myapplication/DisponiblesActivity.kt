package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.adapters.UsersAdapter
import com.example.myapplication.databinding.ActivityDisponiblesBinding
import com.example.myapplication.listeners.UserListener
import com.example.myapplication.models.Usuario
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Objects

class DisponiblesActivity : AppCompatActivity(), UserListener {
    //variables
    var binding: ActivityDisponiblesBinding? = null
    var Client: Usuario? = Usuario()
    var database = FirebaseDatabase.getInstance()
    var myRef: DatabaseReference? = null
    private var mAuth: FirebaseAuth? = null
    var personadispo: Usuario? = Usuario()

    //on create
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //binding
        binding = ActivityDisponiblesBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        mAuth = FirebaseAuth.getInstance()
        myRef = database.getReference(PATH_USERS)
        // si no existe el usuario volver al login
        if (mAuth!!.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            users
        }
    }

    // se extren los usuarios disponibles de la base de datos
    private val users: Unit
        private get() {
            val users: MutableList<Usuario> = ArrayList()
            myRef = database.getReference(PATH_USERS)
            myRef!!.database.getReference(PATH_USERS).get()
                .addOnCompleteListener { task: Task<DataSnapshot> ->
                    if (task.isSuccessful) {
                        //se define si esta disponible o no para ponerlo en la lista y se usa un RecyclerView para ponerlos en lista en la pantalla
                        for (walker in task.result.children) {
                            if (Objects.requireNonNull(mAuth!!.currentUser)!!.uid != walker.key) {
                                personadispo = walker.getValue(Usuario::class.java)
                                assert(personadispo != null)
                                if (personadispo!!.isIsdisponible) {
                                    users.add(
                                        Usuario(
                                            walker.key,
                                            personadispo!!.nombre,
                                            personadispo!!.apellido,
                                            personadispo!!.correo,
                                            personadispo!!.fotodeperfil,
                                            personadispo!!.numerodeidentificacion,
                                            personadispo!!.latitud,
                                            personadispo!!.longitud
                                        )
                                    )
                                }
                            }
                        }
                        if (users.size > 0) {
                            val usersAdapter = UsersAdapter(users, this)
                            binding!!.usersList.adapter = usersAdapter
                            binding!!.usersList.visibility = View.VISIBLE
                            binding!!.progressBar.visibility = View.GONE
                        }
                    }
                }
        }

    // funcion para definir a quien sigue el usuario
    override fun onUserClicked(user: Usuario) {
        val intent = Intent(applicationContext, MapActivity::class.java)
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
                assert(Client != null)
                if (Client!!.siguiendoa == null) {
                    Client!!.siguiendoa = user.id
                    myRef!!.setValue(Client)
                } else {
                    if (Client!!.siguiendoa != user.id) {
                        Client!!.siguiendoa = user.id
                        myRef!!.setValue(Client)
                    }
                }
            }
        }
        startActivity(intent)
        finish()
    }

    //si se le da a atras se devuelve a mapas
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(applicationContext, MapActivity::class.java))
        finish()
    }

    //variables globales
    companion object {
        const val PATH_USERS = "users/"
    }
}