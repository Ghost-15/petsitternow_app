package www.com.petsitternow_app.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.exceptions.NoCredentialException
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

    private val addAccountLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
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

        googleSignIn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val idToken = googleAuthClient.signIn()
                    if (idToken != null) {
                        viewModel.signInGoogle(idToken)
                    }
                } catch (e: NoCredentialException) {
                    Toast.makeText(this@SignInActivity, "Aucun compte Google trouvé. Veuillez en ajouter un.", Toast.LENGTH_LONG).show()
                    val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
                    intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                    addAccountLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(this@SignInActivity, "Une erreur inattendue est survenue: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    }

}
