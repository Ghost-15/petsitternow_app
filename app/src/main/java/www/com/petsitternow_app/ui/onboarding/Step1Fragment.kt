package www.com.petsitternow_app.ui.onboarding

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import www.com.petsitternow_app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class Step1Fragment : Fragment(R.layout.fragment_onboarding_step1) {

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPhone: EditText
    private lateinit var spinnerGender: Spinner
    private lateinit var etDateOfBirth: EditText
    private lateinit var btnNext: Button
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupGenderSpinner()
        setupDatePicker()
        setupNextButton()
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
        val genderOptions = listOf("Sélectionner...", "Homme", "Femme", "Autre")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter
    }

    private fun setupDatePicker() {
        etDateOfBirth.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                    etDateOfBirth.setText(format.format(calendar.time))
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

    private fun setupNextButton() {
        btnNext.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val gender = spinnerGender.selectedItemPosition
            val dateOfBirth = etDateOfBirth.text.toString()

            val error = when {
                firstName.length < 2 -> "Le prénom doit contenir au moins 2 caractères"
                lastName.length < 2 -> "Le nom doit contenir au moins 2 caractères"
                !phone.matches(Regex("^0[1-9][0-9]{8}$")) -> "Numéro invalide (ex: 0612345678)"
                gender == 0 -> "Veuillez sélectionner un genre"
                dateOfBirth.isBlank() -> "Veuillez sélectionner une date"
                else -> null
            }

            if (error != null) {
                tvError.text = error
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
                // Navigation vers l'étape 2
                findNavController().navigate(R.id.action_step1_to_step2)
            }
        }
    }
}

