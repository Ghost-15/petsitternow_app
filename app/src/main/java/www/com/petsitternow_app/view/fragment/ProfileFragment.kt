package www.com.petsitternow_app.view.fragment

import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.auth.SignInActivity

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
    private lateinit var btnEditProfile: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnDeleteAccount: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        observeState()
        observeLogoutEvent()
        observeProfileUpdated()
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
        btnEditProfile = view.findViewById(R.id.btnEditProfile)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            viewModel.logout()
        }

        btnEditProfile.setOnClickListener {
            val state = viewModel.state.value
            val bundle = Bundle().apply {
                putString("firstName", state.firstName)
                putString("lastName", state.lastName)
                putString("phone", state.phone)
                putString("gender", state.gender)
                putString("dateOfBirth", state.dateOfBirth)
                putString("address", state.address)
                putString("city", state.city)
                putString("codePostal", state.codePostal)
            }
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment, bundle)
        }

        btnChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_changePasswordFragment)
        }

        val tvPrivacy = requireView().findViewById<TextView>(R.id.tvPrivacyLink)
        tvPrivacy?.paintFlags = (tvPrivacy?.paintFlags ?: 0) or Paint.UNDERLINE_TEXT_FLAG
        tvPrivacy?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://petsitternow.vercel.app/privacy")))
        }

        btnDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Supprimer votre compte ?")
                .setMessage(
                    "Cette action est irréversible. Toutes vos données seront supprimées :\n\n" +
                    "• Informations personnelles\n" +
                    "• Animaux enregistrés\n" +
                    "• Compte Firebase Auth\n\n" +
                    "Vos promenades passées seront anonymisées."
                )
                .setPositiveButton("Supprimer") { _, _ ->
                    viewModel.deleteAccount()
                }
                .setNegativeButton("Annuler", null)
                .show()
        }
    }

    private fun observeProfileUpdated() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>("profileUpdated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    viewModel.loadProfileData()
                    findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>("profileUpdated")
                }
            }
    }

    private fun observeLogoutEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.logoutEvent.collect {
                    val intent = Intent(requireContext(), SignInActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
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

                    // Bouton changer mot de passe (visible seulement pour les utilisateurs password)
                    btnChangePassword.visibility = if (state.isPasswordUser) View.VISIBLE else View.GONE

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


