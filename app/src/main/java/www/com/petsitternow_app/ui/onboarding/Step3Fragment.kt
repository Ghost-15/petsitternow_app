package www.com.petsitternow_app.ui.onboarding

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R

@AndroidEntryPoint
class Step3Fragment : Fragment(R.layout.fragment_onboarding_step3) {

    // ViewModel partagé avec les autres étapes
    private val viewModel: OnboardingViewModel by activityViewModels()

    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etCodePostal: EditText
    private lateinit var btnPrevious: Button
    private lateinit var btnFinish: Button
    private lateinit var tvError: TextView

    private var isInitializing = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupTextWatchers()
        setupClickListeners()
        observeState()
    }

    private fun initViews(view: View) {
        etAddress = view.findViewById(R.id.etAddress)
        etCity = view.findViewById(R.id.etCity)
        etCodePostal = view.findViewById(R.id.etCodePostal)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnFinish = view.findViewById(R.id.btnFinish)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupTextWatchers() {
        etAddress.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateAddress(text?.toString() ?: "")
            }
        }

        etCity.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateCity(text?.toString() ?: "")
            }
        }

        etCodePostal.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateCodePostal(text?.toString() ?: "")
            }
        }
    }

    private fun setupClickListeners() {
        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        btnFinish.setOnClickListener {
            viewModel.submitOnboarding()
        }
    }

    private fun showDataSummaryDialog(state: OnboardingState) {
        val message = """
            |📋 DONNÉES COLLECTÉES
            |
            |━━━ Étape 1 : Infos personnelles ━━━
            |👤 Prénom : ${state.firstName}
            |👤 Nom : ${state.lastName}
            |📱 Téléphone : ${state.phone}
            |⚧ Genre : ${state.gender?.label ?: "Non défini"}
            |🎂 Date de naissance : ${state.dateOfBirth}
            |
            |━━━ Étape 2 : Type d'utilisateur ━━━
            |🏷 Type : ${state.userType?.value ?: "Non défini"}
            |
            |━━━ Étape 3 : Adresse ━━━
            |🏠 Adresse : ${state.address}
            |🏙 Ville : ${state.city}
            |📮 Code postal : ${state.codePostal}
        """.trimMargin()

        AlertDialog.Builder(requireContext())
            .setTitle("✅ Onboarding terminé !")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                // TODO: Naviguer vers le Dashboard
            }
            .setCancelable(false)
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    isInitializing = true

                    // Restaure les valeurs depuis le ViewModel
                    if (etAddress.text.toString() != state.address) {
                        etAddress.setText(state.address)
                    }
                    if (etCity.text.toString() != state.city) {
                        etCity.setText(state.city)
                    }
                    if (etCodePostal.text.toString() != state.codePostal) {
                        etCodePostal.setText(state.codePostal)
                    }

                    // Affichage erreur
                    if (state.error != null && state.currentStep == 3) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    // Loading state
                    btnFinish.isEnabled = !state.isLoading
                    btnFinish.text = if (state.isLoading) "Chargement..." else "Terminer"

                    // Onboarding terminé - Affiche les données collectées
                    if (state.isCompleted) {
                        showDataSummaryDialog(state)
                    }

                    isInitializing = false
                }
            }
        }
    }
}
