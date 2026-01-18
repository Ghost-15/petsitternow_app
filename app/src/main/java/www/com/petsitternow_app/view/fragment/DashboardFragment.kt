package www.com.petsitternow_app.view.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.ui.walk.OwnerWalkViewModel
import www.com.petsitternow_app.ui.walk.components.OwnerWalkContent
import www.com.petsitternow_app.ui.walk.components.WalkStatusCard
import www.com.petsitternow_app.util.TimeFormatter

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private val walkViewModel: OwnerWalkViewModel by viewModels()
    private var petAdapter: PetAdapter? = null
    private var isViewSetup = false
    private var currentLocation: WalkLocation? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            requestCurrentLocation()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isViewSetup = false
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPets()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when {
                        state.isLoading -> showLoading()
                        state.error != null -> showError(state.error)
                        else -> {
                            val userType = state.userType ?: "owner"
                            if (!isViewSetup) {
                                setupView(userType)
                                isViewSetup = true
                            }
                            if (userType == "owner" || state.userType == null) {
                                updatePetsDisplay(state.pets)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showLoading() {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        if (isViewSetup) return
        container.removeAllViews()
        container.addView(layoutInflater.inflate(R.layout.fragment_dashboard_loading, container, false))
    }

    private fun showError(error: String) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        container.removeAllViews()
        isViewSetup = false
        val errorView = layoutInflater.inflate(R.layout.fragment_dashboard_error, container, false)
        errorView.findViewById<TextView>(R.id.tvError)?.text = error
        container.addView(errorView)
    }

    private fun setupView(userType: String) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        container.removeAllViews()

        val layoutId = when (userType) {
            "petsitter" -> R.layout.fragment_dashboard_petsitter
            else -> R.layout.fragment_dashboard_owner
        }

        val contentView = layoutInflater.inflate(layoutId, container, false)
        container.addView(contentView)

        when (userType) {
            "petsitter" -> setupPetsitterView(contentView)
            else -> setupOwnerView(contentView)
        }
    }

    private fun setupPetsitterView(view: View) {
        view.findViewById<MaterialButton>(R.id.btnMissions)?.setOnClickListener { }
    }

    private fun setupOwnerView(view: View) {
        view.findViewById<MaterialButton>(R.id.btnAddPet)?.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addPet)
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewPets)
        petAdapter = PetAdapter(emptyList()) { pet ->
            findNavController().navigate(
                R.id.action_dashboard_to_editPet,
                bundleOf("petId" to pet.id)
            )
        }
        recyclerView?.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView?.adapter = petAdapter

        // Setup Walk Status Card (ComposeView)
        view.findViewById<ComposeView>(R.id.composeViewWalkStatus)?.setContent {
            val walkState by walkViewModel.uiState.collectAsStateWithLifecycle()

            walkState.activeWalk?.let { walkRequest ->
                MaterialTheme {
                    WalkStatusCard(
                        walkRequest = walkRequest,
                        activeWalk = walkState.activeWalkDetails,
                        elapsedTime = walkState.activeWalkDetails?.walkStartedAt?.let {
                            TimeFormatter.formatElapsedTime(it)
                        } ?: "",
                        onCancel = { walkViewModel.cancelWalkRequest() },
                        onDismiss = { walkViewModel.dismissFailedRequest() }
                    )
                }
            }
        }

        // Setup Walk FAB (ComposeView)
        view.findViewById<ComposeView>(R.id.composeViewWalkFab)?.setContent {
            val walkState by walkViewModel.uiState.collectAsStateWithLifecycle()

            MaterialTheme {
                OwnerWalkContent(
                    activeWalk = walkState.activeWalk,
                    activeWalkDetails = walkState.activeWalkDetails,
                    pets = walkState.pets,
                    currentLocation = currentLocation,
                    elapsedTime = walkState.activeWalkDetails?.walkStartedAt?.let {
                        TimeFormatter.formatElapsedTime(it)
                    } ?: "",
                    isLoading = walkState.isRequestingWalk,
                    onRequestWalk = { petIds, duration, location ->
                        walkViewModel.requestWalk(petIds, duration.value, location)
                    },
                    onCancelWalk = { walkViewModel.cancelWalkRequest() },
                    onDismissWalk = { walkViewModel.dismissFailedRequest() },
                    onRequestLocation = { requestLocationPermissionAndFetch() }
                )
            }
        }

        // Observe walk state to show/hide status card
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                walkViewModel.uiState.collect { state ->
                    view.findViewById<ComposeView>(R.id.composeViewWalkStatus)?.visibility =
                        if (state.activeWalk != null) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun requestLocationPermissionAndFetch() {
        when {
            hasLocationPermission() -> requestCurrentLocation()
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

    private fun requestCurrentLocation() {
        viewLifecycleOwner.lifecycleScope.launch {
            currentLocation = walkViewModel.getCurrentLocation()
        }
    }

    private fun updatePetsDisplay(pets: List<Pet>) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        val contentView = container.getChildAt(0) ?: return

        val recyclerView = contentView.findViewById<RecyclerView>(R.id.recyclerViewPets) ?: return
        val layoutNoPets = contentView.findViewById<View>(R.id.layoutNoPets)
        val cardHowItWorks = contentView.findViewById<View>(R.id.cardHowItWorks)
        val tvPetsCount = contentView.findViewById<TextView>(R.id.tvPetsCount)

        if (pets.isNotEmpty()) {
            recyclerView.visibility = View.VISIBLE
            layoutNoPets?.visibility = View.GONE
            cardHowItWorks?.visibility = View.GONE
            tvPetsCount?.text = "${pets.size} ${if (pets.size > 1) "chiens" else "chien"}"
            petAdapter?.updatePets(pets)
        } else {
            recyclerView.visibility = View.GONE
            layoutNoPets?.visibility = View.VISIBLE
            cardHowItWorks?.visibility = View.VISIBLE
            tvPetsCount?.text = ""
        }
    }
}
