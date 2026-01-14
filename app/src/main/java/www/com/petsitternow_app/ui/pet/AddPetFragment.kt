package www.com.petsitternow_app.ui.pet

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
class AddPetFragment : Fragment(R.layout.fragment_add_pet) {

    private val viewModel: AddPetViewModel by viewModels()
    private lateinit var etName: EditText
    private lateinit var spinnerBreed: Spinner
    private lateinit var tvBirthDate: TextView
    private lateinit var btnSubmit: Button
    private lateinit var tvError: TextView

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var isInitializing = true

    private val dogBreeds = listOf(
        "Labrador Retriever",
        "Golden Retriever",
        "Berger Allemand",
        "Bulldog Français",
        "Beagle",
        "Caniche",
        "Rottweiler",
        "Yorkshire Terrier",
        "Boxer",
        "Dachshund",
        "Shiba Inu",
        "Border Collie",
        "Autre"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupSpinner()
        setupClickListeners()
        observeState()
    }

    private fun initViews(view: View) {
        etName = view.findViewById(R.id.etName)
        spinnerBreed = view.findViewById(R.id.spinnerBreed)
        tvBirthDate = view.findViewById(R.id.tvBirthDate)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            dogBreeds
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBreed.adapter = adapter
    }

    private fun setupClickListeners() {
        etName.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateName(text?.toString() ?: "")
            }
        }

        spinnerBreed.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitializing) {
                    val selectedBreed = dogBreeds[position]
                    if (selectedBreed != "Autre") {
                        viewModel.updateBreed(selectedBreed)
                    } else {
                        viewModel.updateBreed("")
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        tvBirthDate.setOnClickListener {
            showDatePicker()
        }

        btnSubmit.setOnClickListener {
            viewModel.submitPet()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val dateString = dateFormat.format(calendar.time)
                tvBirthDate.text = dateString
                viewModel.updateBirthDate(dateString)
            },
            year,
            month,
            day
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    isInitializing = true

                    if (etName.text.toString() != state.name) {
                        etName.setText(state.name)
                    }

                    if (state.birthDate.isNotEmpty() && tvBirthDate.text.toString() != state.birthDate) {
                        tvBirthDate.text = state.birthDate
                    }

                    if (state.error != null) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    btnSubmit.isEnabled = !state.isLoading
                    btnSubmit.text = if (state.isLoading) "Enregistrement..." else "Enregistrer"

                    isInitializing = false
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvent.collect { navigation ->
                    when (navigation) {
                        is AddPetNavigation.GoBack -> {
                            if (findNavController().currentDestination?.id == R.id.addPetFragment) {
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}
