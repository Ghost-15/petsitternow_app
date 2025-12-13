package www.com.petsitternow_app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.dashboard.DashboardActivity

@AndroidEntryPoint
class SignInActivity : AppCompatActivity() {

    private val viewModel: SignInViewModel by viewModels()
    private lateinit var googleAuthClient: GoogleAuthClient
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: Button
    private lateinit var googleSignIn: LinearLayout
    private lateinit var btnSignUp: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        googleAuthClient = GoogleAuthClient(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        googleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnSignUp = findViewById(R.id.btnCreateAccount)

        // Login avec email/password
        btnSignIn.setOnClickListener {
            viewModel.login(
                etEmail.text.toString().trim(),
                etPassword.text.toString().trim()
            )
        }

        // Login via Google
        googleSignIn.setOnClickListener {
            lifecycleScope.launch {
                val idToken = googleAuthClient.signIn()
                Toast.makeText(this@SignInActivity, "TOKEN = $idToken", Toast.LENGTH_LONG).show()
                if (idToken != null) {
                    viewModel.signInGoogle(idToken)
                } else {
                    Toast.makeText(this@SignInActivity, "Google Sign-In Failed !", Toast.LENGTH_LONG).show()
                }
            }
        }


        // Creer un compte
        btnSignUp.setOnClickListener {
            Toast.makeText(this@SignInActivity, "Creer un compte", Toast.LENGTH_LONG).show()
            startActivity(Intent(this@SignInActivity, SignUpActivity::class.java))
            finish()
        }

        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.signInState.collect { state ->
                when {
                    state.isLoading ->
                        Toast.makeText(this@SignInActivity, "Chargement...", Toast.LENGTH_SHORT).show()

                    state.error != null ->
                        Toast.makeText(this@SignInActivity, state.error, Toast.LENGTH_LONG).show()

                    state.isSignInSuccess -> {
                        Toast.makeText(this@SignInActivity, "Connexion réussie !", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@SignInActivity, DashboardActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}
