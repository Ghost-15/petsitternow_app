package www.com.petsitternow_app.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R

@AndroidEntryPoint
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var tvEmail: TextView
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvDateOfBirth: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvCityPostal: TextView
    private lateinit var tvUserType: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvOnboardingStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        observeState()
    }

    private fun initViews(view: View) {
        tvEmail = view.findViewById(R.id.tvEmail)
        tvFirstName = view.findViewById(R.id.tvFirstName)
        tvLastName = view.findViewById(R.id.tvLastName)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvGender = view.findViewById(R.id.tvGender)
        tvDateOfBirth = view.findViewById(R.id.tvDateOfBirth)
        tvAddress = view.findViewById(R.id.tvAddress)
        tvCityPostal = view.findViewById(R.id.tvCityPostal)
        tvUserType = view.findViewById(R.id.tvUserType)
        tvRole = view.findViewById(R.id.tvRole)
        tvOnboardingStatus = view.findViewById(R.id.tvOnboardingStatus)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    // Informations personnelles
                    tvEmail.text = state.email
                    tvFirstName.text = state.firstName
                    tvLastName.text = state.lastName
                    tvPhone.text = state.phone
                    tvGender.text = formatGender(state.gender)
                    tvDateOfBirth.text = state.dateOfBirth

                    // Adresse
                    tvAddress.text = state.address
                    tvCityPostal.text = "${state.codePostal} ${state.city}"

                    // Compte
                    tvUserType.text = formatUserType(state.userType)
                    tvRole.text = formatRole(state.role)
                    tvOnboardingStatus.text = formatOnboardingStatus(state.onboardingCompleted)

                    // États UI
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    if (state.error != null) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun formatGender(gender: String): String {
        return when (gender) {
            "male" -> "Homme"
            "female" -> "Femme"
            "other" -> "Autre"
            else -> gender
        }
    }

    private fun formatUserType(type: String): String {
        return when (type) {
            "owner" -> "Propriétaire"
            "petsitter" -> "Pet-sitter"
            else -> type
        }
    }

    private fun formatRole(role: String?): String {
        return when (role) {
            "owner" -> "Propriétaire"
            "petsitter" -> "Pet-sitter"
            "admin" -> "Administrateur"
            else -> "Non défini"
        }
    }

    private fun formatOnboardingStatus(isCompleted: Boolean?): String {
        return when (isCompleted) {
            true -> "Complet"
            false -> "Incomplet"
            else -> "Non défini"
        }
    }
}


