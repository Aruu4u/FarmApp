package com.example.farmapp.ui.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.farmapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var loginBtn: Button
    private lateinit var registerRedirect: TextView
    private val openCommunity: Boolean
        get() = intent.getBooleanExtra(EXTRA_OPEN_COMMUNITY, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        // Check if already logged in
        if (auth.currentUser != null) {
            openNextScreen()
            finish()
            return
        }

        emailEdit = findViewById(R.id.emailEdt)
        passwordEdit = findViewById(R.id.passEdt)
        loginBtn = findViewById(R.id.loginBtn)
        registerRedirect = findViewById(R.id.registerbtn)

        loginBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                // Fetch username from Database
                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(user.uid)
                                    .child("username")
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        val username = snapshot.getValue(String::class.java) ?: ""
                                        
                                        // Update Local Prefs
                                        getSharedPreferences("farmapp_prefs", MODE_PRIVATE)
                                            .edit()
                                            .putString("username", username)
                                            .apply()

                                        // Update Firebase Profile displayName if missing
                                        if (user.displayName.isNullOrEmpty() && username.isNotEmpty()) {
                                            val profileUpdates = userProfileChangeRequest {
                                                displayName = username
                                            }
                                            user.updateProfile(profileUpdates)
                                        }

                                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                                        openNextScreen()
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        // Even if fetching username fails, we allow login
                                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                                        openNextScreen()
                                        finish()
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java).apply {
                putExtra(EXTRA_OPEN_COMMUNITY, openCommunity)
            })
            finish()
        }
    }

    private fun openNextScreen() {
        val nextActivity = if (openCommunity) community::class.java else MainActivity::class.java
        startActivity(Intent(this, nextActivity))
    }

    companion object {
        const val EXTRA_OPEN_COMMUNITY = "extra_open_community"
    }
}
