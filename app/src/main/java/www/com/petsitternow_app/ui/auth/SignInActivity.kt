package www.com.petsitternow_app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.login(email, password)
        }

        // Observer l'état de la connexion
        lifecycleScope.launch {
            viewModel.signInState.collect { state ->
                if (state.isLoading) {
                    // Afficher une ProgressBar si vous en avez une
                    Toast.makeText(this@SignInActivity, "Chargement...", Toast.LENGTH_SHORT).show()
                }
                state.error?.let {
                    Toast.makeText(this@SignInActivity, it, Toast.LENGTH_LONG).show()
                }
                if (state.isSignInSuccess) {
                    Toast.makeText(this@SignInActivity, "Connexion réussie !", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@SignInActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
