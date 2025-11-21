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
class SignUpActivity : AppCompatActivity() {

    private val viewModel: SignUpViewModel by viewModels()

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignUp: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignUp = findViewById(R.id.btnSignUp)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.signUp(email, password)
        }

        lifecycleScope.launch {
            viewModel.signUpState.collect { state ->
                if (state.isLoading) {
                    Toast.makeText(this@SignUpActivity, "Chargement...", Toast.LENGTH_SHORT).show()
                }
                state.error?.let {
                    Toast.makeText(this@SignUpActivity, it, Toast.LENGTH_LONG).show()
                }
                if (state.isSignUpSuccess) {
                    Toast.makeText(this@SignUpActivity, "Inscription réussie !", Toast.LENGTH_LONG).show()
                    val intent = Intent(this@SignUpActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
