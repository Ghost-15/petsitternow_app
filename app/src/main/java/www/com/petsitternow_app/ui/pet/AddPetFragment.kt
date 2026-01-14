package www.com.petsitternow_app.ui.pet

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
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
    private lateinit var cardPhoto: CardView
    private lateinit var ivPhoto: ImageView
    private lateinit var layoutAddPhoto: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var isInitializing = true

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.updatePhotoUri(it)
            ivPhoto.setImageURI(it)
            ivPhoto.visibility = View.VISIBLE
            layoutAddPhoto.visibility = View.GONE
        }
    }

    private val dogBreeds = listOf(
        "Affenpinscher",
        "Airedale Terrier",
        "Akita",
        "Akita Américain",
        "Alaskan Husky",
        "Alaskan Malamute",
        "American Bully",
        "American Pit Bull Terrier",
        "American Staffordshire Terrier",
        "American Water Spaniel",
        "Anglais Springer Spaniel",
        "Australian Cattle Dog",
        "Australian Kelpie",
        "Australian Shepherd",
        "Australian Terrier",
        "Basenji",
        "Basset Hound",
        "Beagle",
        "Bearded Collie",
        "Bedlington Terrier",
        "Berger Allemand",
        "Berger Australien",
        "Berger Belge",
        "Berger Blanc Suisse",
        "Berger de Brie",
        "Berger des Pyrénées",
        "Berger Hollandais",
        "Berger Shetland",
        "Bernois",
        "Bichon Frisé",
        "Bichon Maltais",
        "Border Collie",
        "Border Terrier",
        "Boston Terrier",
        "Bouledogue Américain",
        "Bouledogue Anglais",
        "Bouledogue Français",
        "Boxer",
        "Braque Allemand",
        "Braque de Weimar",
        "Briard",
        "Bull Terrier",
        "Bulldog",
        "Bullmastiff",
        "Cairn Terrier",
        "Caniche",
        "Cane Corso",
        "Cavalier King Charles Spaniel",
        "Chihuahua",
        "Chow Chow",
        "Cocker Spaniel",
        "Colley",
        "Dalmatien",
        "Doberman",
        "Dogue Allemand",
        "Dogue Argentin",
        "Dogue de Bordeaux",
        "English Setter",
        "Épagneul Breton",
        "Épagneul Papillon",
        "Fox Terrier",
        "Foxhound",
        "Golden Retriever",
        "Grand Bouvier Suisse",
        "Greyhound",
        "Griffon Bruxellois",
        "Husky Sibérien",
        "Jack Russell Terrier",
        "Labrador Retriever",
        "Lévrier Afghan",
        "Lévrier Espagnol",
        "Malamute",
        "Mastiff",
        "Montagne des Pyrénées",
        "Newfoundland",
        "Pékinois",
        "Pinscher",
        "Pointer",
        "Pomeranian",
        "Poodle",
        "Pug",
        "Rottweiler",
        "Saint-Bernard",
        "Samoyède",
        "Schnauzer",
        "Setter Irlandais",
        "Shar Pei",
        "Shiba Inu",
        "Shih Tzu",
        "Staffordshire Bull Terrier",
        "Teckel",
        "Terrier",
        "Terre-Neuve",
        "Vizsla",
        "Weimaraner",
        "Welsh Corgi",
        "West Highland White Terrier",
        "Whippet",
        "Yorkshire Terrier"
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
        cardPhoto = view.findViewById(R.id.cardPhoto)
        ivPhoto = view.findViewById(R.id.ivPhoto)
        layoutAddPhoto = view.findViewById(R.id.layoutAddPhoto)
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
        cardPhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        etName.doAfterTextChanged { text ->
            if (!isInitializing) {
                viewModel.updateName(text?.toString() ?: "")
            }
        }

        spinnerBreed.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isInitializing) {
                    viewModel.updateBreed(dogBreeds[position])
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

                    state.photoUri?.let { uri ->
                        ivPhoto.setImageURI(uri)
                        ivPhoto.visibility = View.VISIBLE
                        layoutAddPhoto.visibility = View.GONE
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
