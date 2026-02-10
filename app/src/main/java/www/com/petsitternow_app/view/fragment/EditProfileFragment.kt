package www.com.petsitternow_app.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    private val viewModel: EditProfileViewModel by viewModels()

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var spinnerGender: Spinner
    private lateinit var btnDateOfBirth: MaterialButton
    private lateinit var etAddress: TextInputEditText
    private lateinit var etCity: TextInputEditText
    private lateinit var etCodePostal: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private var selectedDateOfBirth: String = ""

    private val genderValues = listOf("homme", "femme", "autre")
    private val genderLabels = listOf("Homme", "Femme", "Autre")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupGenderSpinner()
        prefillFields()
        setupClickListeners()
        observeState()
    }

    private fun initViews(view: View) {
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etPhone = view.findViewById(R.id.etPhone)
        spinnerGender = view.findViewById(R.id.spinnerGender)
        btnDateOfBirth = view.findViewById(R.id.btnDateOfBirth)
        etAddress = view.findViewById(R.id.etAddress)
        etCity = view.findViewById(R.id.etCity)
        etCodePostal = view.findViewById(R.id.etCodePostal)
        btnSave = view.findViewById(R.id.btnSave)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            genderLabels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = adapter
    }

    private fun prefillFields() {
        val args = arguments ?: return

        etFirstName.setText(args.getString("firstName", ""))
        etLastName.setText(args.getString("lastName", ""))
        etPhone.setText(args.getString("phone", ""))
        etAddress.setText(args.getString("address", ""))
        etCity.setText(args.getString("city", ""))
        etCodePostal.setText(args.getString("codePostal", ""))

        // Gender spinner
        val gender = args.getString("gender", "")
        val genderIndex = genderValues.indexOf(gender)
        if (genderIndex >= 0) {
            spinnerGender.setSelection(genderIndex)
        }

        // Date of birth
        val dateOfBirth = args.getString("dateOfBirth", "")
        if (dateOfBirth.isNotEmpty()) {
            selectedDateOfBirth = dateOfBirth
            btnDateOfBirth.text = formatDateForDisplay(dateOfBirth)
        }
    }

    private fun setupClickListeners() {
        btnDateOfBirth.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            val genderPosition = spinnerGender.selectedItemPosition
            val gender = if (genderPosition in genderValues.indices) genderValues[genderPosition] else ""

            viewModel.updateProfile(
                firstName = etFirstName.text.toString(),
                lastName = etLastName.text.toString(),
                phone = etPhone.text.toString(),
                gender = gender,
                dateOfBirth = selectedDateOfBirth,
                address = etAddress.text.toString(),
                city = etCity.text.toString(),
                codePostal = etCodePostal.text.toString()
            )
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        // Parse existing date if available
        if (selectedDateOfBirth.isNotEmpty()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                sdf.parse(selectedDateOfBirth)?.let {
                    calendar.time = it
                }
            } catch (_: Exception) { }
        }

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
                selectedDateOfBirth = sdf.format(cal.time)
                btnDateOfBirth.text = formatDateForDisplay(selectedDateOfBirth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Max date = yesterday
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        datePicker.datePicker.maxDate = yesterday.timeInMillis

        datePicker.show()
    }

    private fun formatDateForDisplay(isoDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            val date = inputFormat.parse(isoDate)
            if (date != null) outputFormat.format(date) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    btnSave.isEnabled = !state.isLoading

                    if (state.error != null) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    if (state.isSuccess) {
                        Snackbar.make(requireView(), "Profil mis à jour avec succès", Snackbar.LENGTH_SHORT).show()
                        // Signal to ProfileFragment to refresh
                        findNavController().previousBackStackEntry?.savedStateHandle?.set("profileUpdated", true)
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}
