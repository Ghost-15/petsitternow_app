package www.com.petsitternow_app.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.onboarding.OnboardingActivity
import www.com.petsitternow_app.view.fragment.setupWithNavController

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private val viewModel: DashboardViewModel by viewModels()
    private var currentNavController: LiveData<NavController>? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        requestNotificationPermissionIfNeeded()

        // Style the status bar
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val bottomNav = findViewById<BottomNavigationView>(R.id.nav_view)
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0)
            insets
        }

        observeNavigationEvents()
        observeState()
        setupBackPressHandler()

        if (savedInstanceState == null) {
            setupBottomNavigationBar()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED -> { }
                else -> requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navController = currentNavController?.value
                if (navController != null && navController.previousBackStackEntry != null) {
                    navController.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun observeNavigationEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { navigation ->
                    when (navigation) {
                        is DashboardNavigation.GoToOnboarding -> {
                            val intent = Intent(this@DashboardActivity, OnboardingActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    }
                }
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->

                    val bottomNav = findViewById<BottomNavigationView>(R.id.nav_view)
                    val navHostFragment = findViewById<View>(R.id.nav_host_fragment)
                    
                    if (!state.isLoading && state.onboardingCompleted) {
                        bottomNav.visibility = View.VISIBLE
                        navHostFragment.visibility = View.VISIBLE
                    } else if (state.isLoading) {
                        bottomNav.visibility = View.INVISIBLE
                        navHostFragment.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun setupBottomNavigationBar() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)

        // Valeurs venant du ViewModel
        bottomNavigationView.selectedItemId = viewModel.defaultSelectedItemId

        currentNavController = bottomNavigationView.setupWithNavController(
            navGraphIds = viewModel.navGraphIds,
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        return currentNavController?.value?.navigateUp()
                ?: false || super.onSupportNavigateUp()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNavigationBar()
    }
}