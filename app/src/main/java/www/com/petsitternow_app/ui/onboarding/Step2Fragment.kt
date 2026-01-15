package www.com.petsitternow_app.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.button.MaterialButton
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
class Step2Fragment : Fragment(R.layout.fragment_onboarding_step2) {

    // ViewModel partagé avec les autres étapes
    private val viewModel: OnboardingViewModel by activityViewModels()

    private lateinit var optionOwner: View
    private lateinit var optionPetsitter: View
    private lateinit var radioOwner: RadioButton
    private lateinit var radioPetsitter: RadioButton
    private lateinit var btnPrevious: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        observeState()
    }

    private fun initViews(view: View) {
        optionOwner = view.findViewById(R.id.optionOwner)
        optionPetsitter = view.findViewById(R.id.optionPetsitter)
        radioOwner = view.findViewById(R.id.radioOwner)
        radioPetsitter = view.findViewById(R.id.radioPetsitter)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnNext = view.findViewById(R.id.btnNext)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupClickListeners() {
        // Sélection Owner
        val selectOwner = {
            viewModel.updateUserType(UserType.OWNER)
        }

        // Sélection Petsitter
        val selectPetsitter = {
            viewModel.updateUserType(UserType.PETSITTER)
        }

        optionOwner.setOnClickListener { selectOwner() }
        radioOwner.setOnClickListener { selectOwner() }
        optionPetsitter.setOnClickListener { selectPetsitter() }
        radioPetsitter.setOnClickListener { selectPetsitter() }

        // Navigation
        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        btnNext.setOnClickListener {
            if (viewModel.validateStep2()) {
                findNavController().navigate(R.id.action_step2_to_step3)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateSelection(state.userType)

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

    private fun updateSelection(userType: UserType?) {
        when (userType) {
            UserType.OWNER -> {
                radioOwner.isChecked = true
                radioPetsitter.isChecked = false
                optionOwner.isSelected = true
                optionPetsitter.isSelected = false
            }
            UserType.PETSITTER -> {
                radioOwner.isChecked = false
                radioPetsitter.isChecked = true
                optionOwner.isSelected = false
                optionPetsitter.isSelected = true
            }
            null -> {
                radioOwner.isChecked = false
                radioPetsitter.isChecked = false
                optionOwner.isSelected = false
                optionPetsitter.isSelected = false
            }
        }
    }
}
