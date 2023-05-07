package com.example.myapplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //botones
        ButtonLoginActivity()
        ButtonRegisterActivity()
    }

    //boton para el login
    fun ButtonLoginActivity() {
        val buttonClick = findViewById<Button>(R.id.LoginBTN)
        buttonClick.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    //boton para le registro
    fun ButtonRegisterActivity() {
        val buttonClick = findViewById<Button>(R.id.RegisterBTN)
        buttonClick.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}