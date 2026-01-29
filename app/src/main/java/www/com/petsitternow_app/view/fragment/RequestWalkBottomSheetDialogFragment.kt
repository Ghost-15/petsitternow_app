package www.com.petsitternow_app.view.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkDuration
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.ui.walk.OwnerWalkViewModel
import www.com.petsitternow_app.view.adapter.PetSelectionAdapter

@AndroidEntryPoint
class RequestWalkBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: OwnerWalkViewModel by viewModels({ requireParentFragment() })

    private var petAdapter: PetSelectionAdapter? = null
    private var currentLocation: WalkLocation? = null
    private var selectedDuration: WalkDuration = WalkDuration.DURATION_30
    private var onWalkRequestListener: ((List<String>, Int, WalkLocation) -> Unit)? = null

    private var btnRequestWalk: MaterialButton? = null
    private var progressSubmit: ProgressBar? = null
    private var progressLocation: ProgressBar? = null
    private var tvLocation: TextView? = null
    private var tvNoPets: TextView? = null
    private var recyclerViewPets: RecyclerView? = null
    private var layoutLocation: LinearLayout? = null

    fun setOnWalkRequestListener(listener: (List<String>, Int, WalkLocation) -> Unit) {
        onWalkRequestListener = listener
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_request_walk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupPetsRecyclerView()
        setupDurationRadioGroup(view)
        setupLocationButton(view)
        setupSubmitButton()
        observeState()
        
        // Si la localisation est déjà disponible, masquer le bouton de localisation
        if (currentLocation != null) {
            layoutLocation?.visibility = View.GONE
            updateSubmitButtonState()
        } else {
            requestLocationPermission()
        }
    }

    private fun initViews(view: View) {
        btnRequestWalk = view.findViewById(R.id.btnRequestWalk)
        progressSubmit = view.findViewById(R.id.progressSubmit)
        progressLocation = view.findViewById(R.id.progressLocation)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvNoPets = view.findViewById(R.id.tvNoPets)
        recyclerViewPets = view.findViewById(R.id.recyclerViewPets)
        layoutLocation = view.findViewById(R.id.layoutLocation)
    }

    private fun setupPetsRecyclerView() {
        petAdapter = PetSelectionAdapter(emptyList()) { selectedIds ->
            updateSubmitButtonState()
        }
        recyclerViewPets?.layoutManager = LinearLayoutManager(requireContext())
        recyclerViewPets?.adapter = petAdapter
    }

    private fun setupDurationRadioGroup(view: View) {
        view.findViewById<RadioGroup>(R.id.radioGroupDuration)?.setOnCheckedChangeListener { _, checkedId ->
            selectedDuration = when (checkedId) {
                R.id.radio30min -> WalkDuration.DURATION_30
                R.id.radio45min -> WalkDuration.DURATION_45
                R.id.radio60min -> WalkDuration.DURATION_60
                else -> WalkDuration.DURATION_30
            }
        }
    }

    private fun setupLocationButton(view: View) {
        view.findViewById<LinearLayout>(R.id.layoutLocation)?.setOnClickListener {
            requestLocationPermission()
        }
    }

    private fun setupSubmitButton() {
        btnRequestWalk?.setOnClickListener {
            val selectedPetIds = petAdapter?.getSelectedPetIds() ?: emptyList()
            val location = currentLocation

            if (selectedPetIds.isNotEmpty() && location != null) {
                onWalkRequestListener?.invoke(selectedPetIds, selectedDuration.minutes, location)
                    ?: viewModel.requestWalk(selectedPetIds, selectedDuration.minutes.toString(), location)
                dismiss()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update pets list
                    if (state.pets.isNotEmpty()) {
                        petAdapter?.updatePets(state.pets)
                        recyclerViewPets?.visibility = View.VISIBLE
                        tvNoPets?.visibility = View.GONE
                    } else {
                        recyclerViewPets?.visibility = View.GONE
                        tvNoPets?.visibility = View.VISIBLE
                    }

                    // Update loading state
                    if (state.isRequestingWalk) {
                        btnRequestWalk?.visibility = View.INVISIBLE
                        progressSubmit?.visibility = View.VISIBLE
                    } else {
                        btnRequestWalk?.visibility = View.VISIBLE
                        progressSubmit?.visibility = View.GONE
                    }

                    updateSubmitButtonState()
                }
            }
        }
    }

    private fun updateSubmitButtonState() {
        val hasSelectedPets = (petAdapter?.getSelectedPetIds()?.size ?: 0) > 0
        val hasLocation = currentLocation != null
        btnRequestWalk?.isEnabled = hasSelectedPets && hasLocation
    }

    private fun requestLocationPermission() {
        when {
            hasLocationPermission() -> fetchCurrentLocation()
            else -> locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchCurrentLocation() {
        progressLocation?.visibility = View.VISIBLE
        tvLocation?.text = "Recherche de votre position..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                currentLocation = viewModel.getCurrentLocation()
                progressLocation?.visibility = View.GONE

                currentLocation?.let { location ->
                    // Masquer le bouton de localisation une fois la position obtenue
                    layoutLocation?.visibility = View.GONE
                } ?: run {
                    tvLocation?.text = "Position non disponible. Cliquez pour réessayer"
                    layoutLocation?.isClickable = true
                }
            } catch (e: Exception) {
                progressLocation?.visibility = View.GONE
                tvLocation?.text = "Erreur de localisation. Cliquez pour réessayer"
                currentLocation = null
                layoutLocation?.isClickable = true
            }

            updateSubmitButtonState()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        petAdapter = null
        btnRequestWalk = null
        progressSubmit = null
        progressLocation = null
        tvLocation = null
        tvNoPets = null
        recyclerViewPets = null
        layoutLocation = null
    }

    companion object {
        const val TAG = "RequestWalkBottomSheet"

        fun newInstance(
            pets: List<www.com.petsitternow_app.domain.repository.Pet> = emptyList(),
            location: WalkLocation? = null
        ): RequestWalkBottomSheetDialogFragment {
            return RequestWalkBottomSheetDialogFragment().apply {
                this.currentLocation = location
            }
        }
    }
}
