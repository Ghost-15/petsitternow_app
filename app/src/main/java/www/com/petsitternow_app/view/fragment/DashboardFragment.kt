package www.com.petsitternow_app.view.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.ui.walk.OwnerWalkViewModel
import www.com.petsitternow_app.ui.walk.WalkTrackingViewModel
import www.com.petsitternow_app.view.map.WalkTrackingMapView

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private val walkViewModel: OwnerWalkViewModel by viewModels()
    private val walkTrackingViewModel: WalkTrackingViewModel by viewModels()
    private var petAdapter: PetAdapter? = null
    private var isViewSetup = false
    private var currentLocation: WalkLocation? = null
    private var fabRequestWalk: ExtendedFloatingActionButton? = null
    private var hasPets = false
    private var walkTrackingMapView: WalkTrackingMapView? = null

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

        // Setup Walk Status Card (XML)
        val walkStatusCard = view.findViewById<View>(R.id.walkStatusCard)
        val tvStatusTitle = walkStatusCard?.findViewById<TextView>(R.id.tvStatusTitle)
        val tvStatusSubtitle = walkStatusCard?.findViewById<TextView>(R.id.tvStatusSubtitle)
        val ivStatusIcon = walkStatusCard?.findViewById<ImageView>(R.id.ivStatusIcon)
        val progressStatus = walkStatusCard?.findViewById<ProgressBar>(R.id.progressStatus)
        val viewStatusBackground = walkStatusCard?.findViewById<View>(R.id.viewStatusBackground)
        val layoutTimer = walkStatusCard?.findViewById<View>(R.id.layoutTimer)
        val tvTimer = walkStatusCard?.findViewById<TextView>(R.id.tvTimer)
        val layoutPetsitterInfo = walkStatusCard?.findViewById<View>(R.id.layoutPetsitterInfo)
        val tvPetsitterName = walkStatusCard?.findViewById<TextView>(R.id.tvPetsitterName)
        val tvPetsitterInitial = walkStatusCard?.findViewById<TextView>(R.id.tvPetsitterInitial)
        val btnCancel = walkStatusCard?.findViewById<MaterialButton>(R.id.btnCancel)
        val btnDismiss = walkStatusCard?.findViewById<MaterialButton>(R.id.btnDismiss)

        btnCancel?.setOnClickListener { walkViewModel.cancelWalkRequest() }
        btnDismiss?.setOnClickListener { walkViewModel.dismissFailedRequest() }

        // Setup Walk Tracking Map inside the card
        val mapContainer = walkStatusCard?.findViewById<androidx.cardview.widget.CardView>(R.id.mapContainer)
        walkTrackingMapView = WalkTrackingMapView(requireContext())
        walkTrackingMapView?.let { mapView ->
            mapView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            mapContainer?.addView(mapView)
        }

        // Setup Walk FAB
        fabRequestWalk = view.findViewById<ExtendedFloatingActionButton>(R.id.fabRequestWalk)
        fabRequestWalk?.setOnClickListener {
            requestLocationPermissionAndFetch()
            val bottomSheet = RequestWalkBottomSheetDialogFragment.newInstance(
                walkViewModel.uiState.value.pets,
                currentLocation
            )
            bottomSheet.setOnWalkRequestListener { petIds, duration, location ->
                walkViewModel.requestWalk(petIds, duration.toString(), location)
            }
            bottomSheet.show(childFragmentManager, "RequestWalkBottomSheet")
        }

        // Observe walk tracking state for map and timer updates
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                walkTrackingViewModel.uiState.collect { trackingState ->
                    val activeWalk = trackingState.activeWalk
                    val shouldShowMap = activeWalk != null && 
                        activeWalk.status in listOf(
                            WalkStatus.ASSIGNED, 
                            WalkStatus.GOING_TO_OWNER, 
                            WalkStatus.WALKING, 
                            WalkStatus.IN_PROGRESS,
                            WalkStatus.RETURNING
                        )

                    // Show/hide map container inside the card
                    mapContainer?.visibility = if (shouldShowMap) View.VISIBLE else View.GONE

                    if (shouldShowMap) {
                        // Get owner location from walk request
                        walkViewModel.uiState.value.activeWalk?.location?.let { ownerLocation ->
                            walkTrackingMapView?.setOwnerLocation(ownerLocation)
                        }
                        activeWalk?.petsitterLocation?.let { petsitterLocation ->
                            walkTrackingMapView?.setPetsitterLocation(petsitterLocation)
                        }
                        // Update route
                        walkTrackingMapView?.setRoute(trackingState.route, activeWalk?.status)
                    }

                    // Update timer from tracking ViewModel (updates every second)
                    if (trackingState.formattedTime.isNotEmpty()) {
                        tvTimer?.text = trackingState.formattedTime
                    }
                }
            }
        }

        // Observe walk state to update UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                walkViewModel.uiState.collect { state ->
                    val hasActiveWalk = state.activeWalk != null
                    walkStatusCard?.visibility = if (hasActiveWalk) View.VISIBLE else View.GONE
                    updateFabVisibility(hasActiveWalk)

                    // Setup walk tracking when active walk changes
                    state.activeWalk?.id?.let { requestId ->
                        walkTrackingViewModel.setRequestId(requestId)
                    }

                    state.activeWalk?.let { walkRequest ->
                        // Get petsitter name from WalkRequest
                        val petsitterName = walkRequest.petsitter?.let { petsitter ->
                            "${petsitter.firstName} ${petsitter.lastName}".trim().ifEmpty { petsitter.name }
                        }

                        // Use real-time status from activeWalkDetails if available,
                        // unless Firestore status is final (like web implementation)
                        val firestoreStatus = walkRequest.status
                        val realtimeStatus = state.activeWalkDetails?.status
                        val displayStatus = if (firestoreStatus in WalkStatus.FINAL_STATUSES) {
                            firestoreStatus
                        } else {
                            realtimeStatus ?: firestoreStatus
                        }

                        // Update status UI based on walk status
                        when (displayStatus) {
                            WalkStatus.MATCHING, WalkStatus.PENDING -> {
                                tvStatusTitle?.text = "Recherche en cours..."
                                tvStatusSubtitle?.text = "Nous recherchons un petsitter disponible"
                                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_matching)
                                ivStatusIcon?.setImageResource(R.drawable.ic_search)
                                ivStatusIcon?.visibility = View.GONE
                                progressStatus?.visibility = View.VISIBLE
                                layoutTimer?.visibility = View.GONE
                                layoutPetsitterInfo?.visibility = View.GONE
                                btnCancel?.visibility = View.VISIBLE
                                btnDismiss?.visibility = View.GONE
                            }
                            WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> {
                                tvStatusTitle?.text = "Petsitter trouvé !"
                                tvStatusSubtitle?.text = "Votre petsitter arrive bientôt"
                                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_assigned)
                                ivStatusIcon?.setImageResource(R.drawable.ic_check_circle)
                                ivStatusIcon?.visibility = View.VISIBLE
                                progressStatus?.visibility = View.GONE
                                layoutTimer?.visibility = View.GONE
                                layoutPetsitterInfo?.visibility = View.VISIBLE
                                petsitterName?.let { name ->
                                    tvPetsitterName?.text = name
                                    tvPetsitterInitial?.text = name.firstOrNull()?.uppercase()?.toString() ?: "P"
                                }
                                btnCancel?.visibility = View.VISIBLE
                                btnDismiss?.visibility = View.GONE
                            }
                            WalkStatus.WALKING, WalkStatus.IN_PROGRESS -> {
                                tvStatusTitle?.text = "Promenade en cours"
                                tvStatusSubtitle?.text = "Votre chien profite de sa balade"
                                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_walking)
                                ivStatusIcon?.setImageResource(R.drawable.ic_walk)
                                ivStatusIcon?.visibility = View.VISIBLE
                                progressStatus?.visibility = View.GONE
                                layoutTimer?.visibility = View.VISIBLE
                                // Timer text is updated by walkTrackingViewModel observer
                                layoutPetsitterInfo?.visibility = View.VISIBLE
                                petsitterName?.let { name ->
                                    tvPetsitterName?.text = name
                                    tvPetsitterInitial?.text = name.firstOrNull()?.uppercase()?.toString() ?: "P"
                                }
                                btnCancel?.visibility = View.GONE
                                btnDismiss?.visibility = View.GONE
                            }
                            WalkStatus.RETURNING -> {
                                tvStatusTitle?.text = "Le petsitter ramène votre chien"
                                tvStatusSubtitle?.text = "La promenade est terminée, préparez-vous !"
                                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_returning)
                                ivStatusIcon?.setImageResource(R.drawable.ic_home)
                                ivStatusIcon?.visibility = View.VISIBLE
                                progressStatus?.visibility = View.GONE
                                layoutTimer?.visibility = View.VISIBLE
                                // Timer text is updated by walkTrackingViewModel observer
                                layoutPetsitterInfo?.visibility = View.VISIBLE
                                petsitterName?.let { name ->
                                    tvPetsitterName?.text = name
                                    tvPetsitterInitial?.text = name.firstOrNull()?.uppercase()?.toString() ?: "P"
                                }
                                btnCancel?.visibility = View.GONE
                                btnDismiss?.visibility = View.GONE
                            }
                            WalkStatus.COMPLETED -> {
                                tvStatusTitle?.text = "Promenade terminée"
                                tvStatusSubtitle?.text = "Votre chien est de retour"
                                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_completed)
                                ivStatusIcon?.setImageResource(R.drawable.ic_check_circle)
                                ivStatusIcon?.visibility = View.VISIBLE
                                progressStatus?.visibility = View.GONE
                                layoutTimer?.visibility = View.GONE
                                layoutPetsitterInfo?.visibility = View.GONE
                                btnCancel?.visibility = View.GONE
                                btnDismiss?.visibility = View.VISIBLE
                            }
                            WalkStatus.FAILED -> {
                                tvStatusTitle?.text = "Échec de la demande"
                                tvStatusSubtitle?.text = "Aucun petsitter disponible"
                                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_failed)
                                ivStatusIcon?.setImageResource(R.drawable.ic_close)
                                ivStatusIcon?.visibility = View.VISIBLE
                                progressStatus?.visibility = View.GONE
                                layoutTimer?.visibility = View.GONE
                                layoutPetsitterInfo?.visibility = View.GONE
                                btnCancel?.visibility = View.GONE
                                btnDismiss?.visibility = View.VISIBLE
                            }
                            WalkStatus.CANCELLED, WalkStatus.DISMISSED -> {
                                walkStatusCard?.visibility = View.GONE
                            }
                        }
                    }
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

        hasPets = pets.isNotEmpty()

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

        // Update FAB visibility based on pets
        val hasActiveWalk = walkViewModel.uiState.value.activeWalk != null
        updateFabVisibility(hasActiveWalk)
    }

    private fun updateFabVisibility(hasActiveWalk: Boolean) {
        fabRequestWalk?.visibility = if (hasActiveWalk || !hasPets) View.GONE else View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        walkTrackingMapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        walkTrackingMapView?.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        walkTrackingMapView?.onDestroy()
        walkTrackingMapView = null
    }
}
