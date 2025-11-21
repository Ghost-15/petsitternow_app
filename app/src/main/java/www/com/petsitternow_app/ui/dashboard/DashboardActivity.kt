package www.com.petsitternow_app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.auth.SignInActivity

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // NOTE: Assurez-vous d'avoir un bouton avec l'ID 'btnLogout' dans votre layout 'activity_dashboard.xml'
        val btnLogout: Button = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            viewModel.logout()
        }

        lifecycleScope.launch {
            viewModel.navigationEvent.collect { navigation ->
                when (navigation) {
                    is DashboardNavigation.GoToSignIn -> {
                        Toast.makeText(this@DashboardActivity, "Déconnexion réussie", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@DashboardActivity, SignInActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        }
    }
}
