package www.com.petsitternow_app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.dashboard.DashboardActivity

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        supportActionBar?.hide()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_onboarding) as NavHostFragment
        navController = navHostFragment.navController
    }

    fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Empêcher de quitter l'onboarding avec le bouton retour
        // L'utilisateur doit compléter l'onboarding
        if (!navController.popBackStack()) {
            // Ne rien faire - bloquer le retour au premier écran
        }
    }
}
