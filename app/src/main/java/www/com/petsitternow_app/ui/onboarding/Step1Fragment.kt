package www.com.petsitternow_app.ui.onboarding

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class Step1Fragment : Fragment(R.layout.fragment_onboarding_step1) {

    // ViewModel partagé avec les autres étapes (activityViewModels)
    private val viewModel: OnboardingViewModel by activityViewModels()

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etDateOfBirth: EditText
    private lateinit var btnNext: Button
    private lateinit var tvError: TextView

    private val genderOptions = listOf("Sélectionner...", "Homme", "Femme", "Autre")
    private var isInitializing = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupGenderSpinner()
        setupDatePicker()
        setupTextWatchers()
        setupNextButton()
        observeState()
    }

    private fun initViews(view: View) {
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etPhone = view.findViewById(R.id.etPhone)
        spinnerGender = view.findViewById(R.id.spinnerGender)
        etDateOfBirth = view.findViewById(R.id.etDateOfBirth)
        btnNext = view.findViewById(R.id.btnNext)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter

        spinnerGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitializing && position > 0) {
                    val gender = Gender.entries[position - 1]
                    viewModel.updateGender(gender)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDatePicker() {
        etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    val dateStr = format.format(calendar.time)
                    etDateOfBirth.setText(dateStr)
                    viewModel.updateDateOfBirth(dateStr)
                },
                calendar.get(Calendar.YEAR) - 25,
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                datePicker.maxDate = System.currentTimeMillis()
                show()
            }
        }
    }

    private fun setupTextWatchers() {
        etFirstName.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateFirstName(text?.toString() ?: "")
            }
        }

        etLastName.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateLastName(text?.toString() ?: "")
            }
        }

        etPhone.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updatePhone(text?.toString() ?: "")
            }
        }
    }

    private fun setupNextButton() {
        btnNext.setOnClickListener {
            if (viewModel.validateStep1()) {
                findNavController().navigate(R.id.action_step1_to_step2)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    isInitializing = true

                    // Restaure les valeurs depuis le ViewModel
                    if (etFirstName.text.toString() != state.firstName) {
                        etFirstName.setText(state.firstName)
                    }
                    if (etLastName.text.toString() != state.lastName) {
                        etLastName.setText(state.lastName)
                    }
                    if (etPhone.text.toString() != state.phone) {
                        etPhone.setText(state.phone)
                    }

                    // Spinner genre
                    val genderPosition = state.gender?.let { Gender.entries.indexOf(it) + 1 } ?: 0
                    if (spinnerGender.selectedItemPosition != genderPosition) {
                        spinnerGender.setSelection(genderPosition)
                    }

                    // Date de naissance
                    if (state.dateOfBirth.isNotBlank() && etDateOfBirth.text.toString() != state.dateOfBirth) {
                        etDateOfBirth.setText(state.dateOfBirth)
                    }

                    // Affichage erreur
                    if (state.error != null) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    isInitializing = false
                }
            }
        }
    }
}
