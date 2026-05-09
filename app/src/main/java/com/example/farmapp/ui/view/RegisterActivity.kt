package com.example.farmapp.ui.view

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.farmapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var nameEdit: EditText
    private lateinit var usernameEdit: EditText
    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText
    private lateinit var registerBtn: Button
    private lateinit var loginRedirect: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        nameEdit = findViewById(R.id.nameEt)
        usernameEdit = findViewById(R.id.usernameEt)
        emailEdit = findViewById(R.id.email)
        passwordEdit = findViewById(R.id.pass)
        registerBtn = findViewById(R.id.registerBtn)
        loginRedirect = findViewById(R.id.loginBtn)

        registerBtn.setOnClickListener {
            val name = nameEdit.text.toString().trim()
            val username = usernameEdit.text.toString().trim()
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (name.isNotEmpty() && username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                                displayName = username
                            }

                            user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    val userProfile = mapOf(
                                        "name" to name,
                                        "username" to username,
                                        "email" to email
                                    )
                                    FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(user.uid)
                                        .setValue(userProfile)
                                    getSharedPreferences("farmapp_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putString("user_name", name)
                                        .putString("username", username)
                                        .apply()
                                    Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, LoginActivity::class.java).apply {
                                        putExtra(
                                            LoginActivity.EXTRA_OPEN_COMMUNITY,
                                            intent.getBooleanExtra(LoginActivity.EXTRA_OPEN_COMMUNITY, false)
                                        )
                                    })
                                    finish()
                                } else {
                                    Toast.makeText(this, "Profile Update Failed: ${profileTask.exception?.message}", Toast.LENGTH_SHORT).show()
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

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra(
                    LoginActivity.EXTRA_OPEN_COMMUNITY,
                    intent.getBooleanExtra(LoginActivity.EXTRA_OPEN_COMMUNITY, false)
                )
            })
            finish()
        }
    }
}
