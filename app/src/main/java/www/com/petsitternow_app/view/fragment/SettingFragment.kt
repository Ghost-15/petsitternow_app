package www.com.petsitternow_app.view.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.auth.SignInActivity


@AndroidEntryPoint
class SettingFragment : Fragment(R.layout.fragment_setting) {

    private val viewModel: SettingViewModel by viewModels()
    private lateinit var btnLogout: Button

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnLogout = view.findViewById(R.id.btnLogout)

        btnLogout.setOnClickListener {
            viewModel.logout()
        }

        observeNavigation()
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            viewModel.navigationEvent.collect { navigation ->
                when (navigation) {
                    SettingNavigation.GoToSignIn -> {
                        Toast.makeText(requireContext(), "Déconnexion réussie", Toast.LENGTH_SHORT).show()

                        val intent = Intent(requireContext(), SignInActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        }
    }
}