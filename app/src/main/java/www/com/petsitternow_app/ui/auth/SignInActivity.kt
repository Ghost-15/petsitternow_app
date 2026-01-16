package www.com.petsitternow_app.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.dashboard.DashboardActivity
import www.com.petsitternow_app.ui.onboarding.OnboardingActivity

@AndroidEntryPoint
class SignInActivity : AppCompatActivity() {

    private val viewModel: SignInViewModel by viewModels()
    private lateinit var googleAuthClient: GoogleAuthClient
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSignIn: MaterialButton
    private lateinit var googleSignIn: View
    private lateinit var btnSignUp: View

    private val addAccountLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Compte ajouté, veuillez réessayer de vous connecter avec Google.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        googleAuthClient = GoogleAuthClient(this)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSignIn = findViewById(R.id.btnSignIn)
        googleSignIn = findViewById(R.id.btnGoogleSignIn)
        btnSignUp = findViewById(R.id.btnCreateAccount)

        btnSignIn.setOnClickListener {
            viewModel.login(etEmail.text.toString().trim(), etPassword.text.toString().trim())
        }

        googleSignIn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val idToken = googleAuthClient.signIn()
                    idToken?.let { viewModel.signInGoogle(it) }
                } catch (e: NoCredentialException) {
                    Toast.makeText(this@SignInActivity, "Aucun compte Google trouvé. Veuillez en ajouter un.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                        putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                    }
                    addAccountLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@SignInActivity, "Une erreur inattendue est survenue: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signInState.collect { state ->
                    btnSignIn.isEnabled = !state.isLoading
                    
                    state.error?.let {
                        Toast.makeText(this@SignInActivity, it, Toast.LENGTH_LONG).show()
                    }
                    
                    if (state.isSignInSuccess) {
                        Toast.makeText(this@SignInActivity, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                        
                        val targetActivity = if (state.needsOnboarding) {
                            OnboardingActivity::class.java
                        } else {
                            DashboardActivity::class.java
                        }
                        
                        val intent = Intent(this@SignInActivity, targetActivity)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            }
        }
    }
}
