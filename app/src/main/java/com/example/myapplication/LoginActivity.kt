package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.android.gms.tasks.Task
import java.util.Objects
import com.example.myapplication.models.Usuario


class LoginActivity : AppCompatActivity() {
    //variables
    var binding: ActivityLoginBinding? = null
    private var firebaseAuth: FirebaseAuth? = null
    var myRef: DatabaseReference? = null

    //on create
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        //binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        firebaseAuth = FirebaseAuth.getInstance()
        //boton de login
        ButtonLoginActivity()
    }

    // bonton de login
    fun ButtonLoginActivity() {
        val currentUser = firebaseAuth!!.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
        //listener
        binding!!.LoginLBTN.setOnClickListener { v: View? ->
            //verificar si el email y la contrasena son validos o no estan vacios
            val email = binding!!.EmailText.text.toString()
            val password = binding!!.PasswordText.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                binding!!.EmailText.error = "Email is required"
                binding!!.PasswordText.error = "Password is required"
            }
            if (!isEmail(binding!!.EmailText)) {
                binding!!.EmailText.error = "Email is not valid"
            } else {
                login(binding!!.EmailText.text.toString(), binding!!.PasswordText.text.toString())
            }
        }
    }

    //funcion para validar el correo y contrasena
    private fun login(email: String, password: String) {
        firebaseAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task: Task<AuthResult?> ->
                if (task.isSuccessful) {
                    updateUI(firebaseAuth!!.currentUser)
                } else {
                    showMessage(Objects.requireNonNull(task.exception)!!.message)
                }
            }
    }

    //mensaje de error
    private fun showMessage(text: String?) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_LONG).show()
    }

    //actualizar el usario si se logra encontrar
    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            myRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            myRef!!.get().addOnCompleteListener { task: Task<DataSnapshot> ->
                if (task.isSuccessful) {
                    if (task.result.exists()) {
                        val usuario = task.result.getValue(Usuario::class.java)!!
                        // iniciar la activida de mapas
                        startActivity(Intent(applicationContext, MapActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }

    //corroborar que es un correo
    fun isEmail(text: EditText): Boolean {
        val email: CharSequence = text.text.toString()
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

}