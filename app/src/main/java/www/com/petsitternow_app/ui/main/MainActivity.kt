package www.com.petsitternow_app.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.auth.SignInActivity
import www.com.petsitternow_app.ui.dashboard.DashboardActivity
import www.com.petsitternow_app.ui.onboarding.OnboardingActivity

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        lifecycleScope.launch {
            viewModel.navigationEvent.collect { navigation ->
                when (navigation) {
                    is MainNavigation.GoToDashboard -> {
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                    }
                    is MainNavigation.GoToSignIn -> {
                        startActivity(Intent(this@MainActivity, SignInActivity::class.java))
                    }
                    is MainNavigation.GoToOnboarding -> {
                        startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                    }
                }
                finish()
            }
        }
    }
}
