package www.com.petsitternow_app.view.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.PetsitterProfile
import www.com.petsitternow_app.domain.model.WalkLocation
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.repository.Pet
import www.com.petsitternow_app.ui.petsitter.PetsitterMissionsViewModel
import www.com.petsitternow_app.ui.walk.OwnerWalkViewModel
import www.com.petsitternow_app.ui.walk.WalkTrackingViewModel
import www.com.petsitternow_app.view.map.WalkTrackingMapView

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private val walkViewModel: OwnerWalkViewModel by viewModels()
    private val walkTrackingViewModel: WalkTrackingViewModel by viewModels()
    private val petsitterViewModel: PetsitterMissionsViewModel by viewModels()
    private var petAdapter: PetAdapter? = null
    private var isViewSetup = false
    private var currentLocation: WalkLocation? = null
    private var fabRequestWalk: ExtendedFloatingActionButton? = null
    private var hasPets = false
    private var walkTrackingMapView: WalkTrackingMapView? = null
    private var petsitterMapView: WalkTrackingMapView? = null
    private var ownerPathEnabled = true
    private var petsitterPathEnabled = true

    // Petsitter dashboard views
    private var switchOnline: SwitchMaterial? = null
    private var tvOnlineStatus: TextView? = null
    private var tvOnlineSubtitle: TextView? = null
    private var viewOnlineBackground: View? = null
    private var ivOnlineIcon: ImageView? = null
    private var progressOnline: ProgressBar? = null
    private var layoutActiveMission: View? = null
    private var layoutOnlineWaiting: View? = null
    private var layoutOffline: View? = null
    private var layoutPendingMission: View? = null
    private var lastShownPendingRequestId: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            requestCurrentLocation()
        }
    }

    private val petsitterLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            petsitterViewModel.toggleOnline(true)
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
                            ownerPathEnabled = state.ownerPathEnabled
                            petsitterPathEnabled = state.petsitterPathEnabled
                            if (!isViewSetup) {
                                setupView(userType)
                                isViewSetup = true
                            }
                            when {
                                userType == "owner" || state.userType == null -> updatePetsDisplay(state.pets)
                                userType == "petsitter" -> updatePetsitterStats(state.petsitterProfile)
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
        // Init views
        switchOnline = view.findViewById(R.id.switchOnline)
        tvOnlineStatus = view.findViewById(R.id.tvOnlineStatus)
        tvOnlineSubtitle = view.findViewById(R.id.tvOnlineSubtitle)
        viewOnlineBackground = view.findViewById(R.id.viewOnlineBackground)
        ivOnlineIcon = view.findViewById(R.id.ivOnlineIcon)
        progressOnline = view.findViewById(R.id.progressOnline)
        layoutActiveMission = view.findViewById(R.id.layoutActiveMission)
        layoutOnlineWaiting = view.findViewById(R.id.layoutOnlineWaiting)
        layoutOffline = view.findViewById(R.id.layoutOffline)
        layoutPendingMission = view.findViewById(R.id.layoutPendingMission)

        // Pending mission action buttons
        view.findViewById<MaterialButton>(R.id.btnAcceptMission)?.setOnClickListener {
            petsitterViewModel.acceptMission()
        }
        view.findViewById<MaterialButton>(R.id.btnDeclineMission)?.setOnClickListener {
            petsitterViewModel.declineMission()
        }

        // Online toggle
        switchOnline?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestPetsitterLocationPermissionAndGoOnline()
            } else {
                petsitterViewModel.toggleOnline(false)
            }
        }

        // Mission action buttons
        layoutActiveMission?.let { missionView ->
            missionView.findViewById<MaterialButton>(R.id.btnOpenMaps)?.setOnClickListener {
                openMapsWithOwnerAddress()
            }
            missionView.findViewById<MaterialButton>(R.id.btnStartWalk)?.setOnClickListener {
                petsitterViewModel.startWalk()
            }
            missionView.findViewById<MaterialButton>(R.id.btnMarkReturning)?.setOnClickListener {
                petsitterViewModel.markReturning()
            }
            missionView.findViewById<MaterialButton>(R.id.btnCompleteMission)?.setOnClickListener {
                petsitterViewModel.completeMission()
            }
        }

        // Setup map for petsitter
        val mapContainer = layoutActiveMission?.findViewById<androidx.cardview.widget.CardView>(R.id.mapContainer)
        petsitterMapView = WalkTrackingMapView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mapContainer?.addView(petsitterMapView)

        // Observe petsitter state
        observePetsitterState(view)
    }

    private fun observePetsitterState(contentView: View) {
        // Observe walk tracking for map
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

                    val mapContainer = layoutActiveMission?.findViewById<View>(R.id.mapContainer)
                    mapContainer?.visibility = if (shouldShowMap) View.VISIBLE else View.GONE

                    if (shouldShowMap) {
                        petsitterViewModel.uiState.value.activeMission?.location?.let { ownerLocation ->
                            petsitterMapView?.setOwnerLocation(ownerLocation)
                        }
                        petsitterViewModel.uiState.value.currentLocation?.let { petsitterLocation ->
                            petsitterMapView?.setPetsitterLocation(petsitterLocation)
                        }
                        petsitterMapView?.setRoute(trackingState.route, activeWalk?.status)
                    }

                    // Update timer
                    val tvTimer = layoutActiveMission?.findViewById<TextView>(R.id.tvTimer)
                    if (trackingState.formattedTime.isNotEmpty()) {
                        tvTimer?.text = trackingState.formattedTime
                    }
                    
                    // Update buttons based on RTDB status
                    activeWalk?.status?.let { rtdbStatus ->
                        updateActiveMissionUI(rtdbStatus, petsitterViewModel.uiState.value)
                    }
                }
            }
        }

        // Observe petsitter missions state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                petsitterViewModel.uiState.collect { state ->
                    // Update toggle appearance
                    switchOnline?.isChecked = state.isOnline
                    if (state.isOnline) {
                        tvOnlineStatus?.text = "En ligne"
                        tvOnlineStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
                        tvOnlineSubtitle?.text = "Vous recevez les missions"
                        viewOnlineBackground?.setBackgroundResource(R.drawable.bg_walk_status_walking)
                        ivOnlineIcon?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success))
                    } else {
                        tvOnlineStatus?.text = "Hors ligne"
                        tvOnlineStatus?.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                        tvOnlineSubtitle?.text = "Appuyez pour recevoir des missions"
                        viewOnlineBackground?.setBackgroundResource(R.drawable.bg_online_toggle_off)
                        ivOnlineIcon?.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_muted))
                    }

                    if (state.isTogglingOnline) {
                        progressOnline?.visibility = View.VISIBLE
                        switchOnline?.visibility = View.GONE
                    } else {
                        progressOnline?.visibility = View.GONE
                        switchOnline?.visibility = View.VISIBLE
                    }

                    // Handle pending mission (inline card instead of dialog)
                    state.pendingMission?.let { mission ->
                        if (!mission.isExpired()) {
                            layoutPendingMission?.visibility = View.VISIBLE
                            layoutOnlineWaiting?.visibility = View.GONE
                            updatePendingMissionCard(mission, state.missionCountdown)
                        } else {
                            layoutPendingMission?.visibility = View.GONE
                        }
                    } ?: run {
                        layoutPendingMission?.visibility = View.GONE
                    }

                    // Handle active mission
                    state.activeMission?.let { mission ->
                        android.util.Log.d("DashboardFragment", "Active mission detected: id=${mission.id}, status=${mission.status}")
                        layoutActiveMission?.visibility = View.VISIBLE
                        layoutOnlineWaiting?.visibility = View.GONE
                        layoutOffline?.visibility = View.GONE
                        layoutPendingMission?.visibility = View.GONE

                        walkTrackingViewModel.setRequestId(mission.id)
                        // Use RTDB status if available, otherwise use Firestore status
                        val rtdbStatus = walkTrackingViewModel.uiState.value.activeWalk?.status
                        val effectiveStatus = rtdbStatus ?: mission.status
                        updateActiveMissionUI(effectiveStatus, state)
                    } ?: run {
                        layoutActiveMission?.visibility = View.GONE
                        if (state.pendingMission == null || state.pendingMission.isExpired()) {
                            if (state.isOnline) {
                                layoutOnlineWaiting?.visibility = View.VISIBLE
                                layoutOffline?.visibility = View.GONE
                            } else {
                                layoutOnlineWaiting?.visibility = View.GONE
                                layoutOffline?.visibility = View.VISIBLE
                            }
                        }
                    }

                    // Update stats
                    updatePetsitterStats(viewModel.state.value.petsitterProfile)
                }
            }
        }
    }

    private fun updateActiveMissionUI(status: WalkStatus, state: www.com.petsitternow_app.ui.petsitter.PetsitterMissionsUiState) {
        val tvStatusTitle = layoutActiveMission?.findViewById<TextView>(R.id.tvStatusTitle)
        val viewStatusBackground = layoutActiveMission?.findViewById<View>(R.id.viewStatusBackground)
        val ivStatusIcon = layoutActiveMission?.findViewById<ImageView>(R.id.ivStatusIcon)
        val tvTimer = layoutActiveMission?.findViewById<View>(R.id.layoutTimer)
        val tvDistance = layoutActiveMission?.findViewById<TextView>(R.id.tvDistance)
        val btnOpenMaps = layoutActiveMission?.findViewById<View>(R.id.btnOpenMaps)
        val btnStartWalk = layoutActiveMission?.findViewById<View>(R.id.btnStartWalk)
        val btnMarkReturning = layoutActiveMission?.findViewById<View>(R.id.btnMarkReturning)
        val btnCompleteMission = layoutActiveMission?.findViewById<MaterialButton>(R.id.btnCompleteMission)

        when (status) {
            WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> {
                tvStatusTitle?.text = "Mission assignée"
                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_assigned)
                ivStatusIcon?.setImageResource(R.drawable.ic_check_circle)
                tvTimer?.visibility = View.GONE
                btnOpenMaps?.visibility = View.VISIBLE
                btnStartWalk?.visibility = View.VISIBLE
                btnMarkReturning?.visibility = View.GONE
                btnCompleteMission?.visibility = View.GONE
            }
            WalkStatus.WALKING, WalkStatus.IN_PROGRESS -> {
                tvStatusTitle?.text = "Promenade en cours"
                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_walking)
                ivStatusIcon?.setImageResource(R.drawable.ic_walk)
                tvTimer?.visibility = View.VISIBLE
                btnOpenMaps?.visibility = View.GONE
                btnStartWalk?.visibility = View.GONE
                btnMarkReturning?.visibility = View.VISIBLE
                btnCompleteMission?.visibility = View.GONE
            }
            WalkStatus.RETURNING -> {
                tvStatusTitle?.text = "Retour en cours"
                viewStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_returning)
                ivStatusIcon?.setImageResource(R.drawable.ic_home)
                tvTimer?.visibility = View.VISIBLE
                state.distanceToOwner?.let { distance ->
                    tvDistance?.text = "${distance.toInt()}m"
                    tvDistance?.visibility = View.VISIBLE
                }
                btnOpenMaps?.visibility = View.VISIBLE
                btnStartWalk?.visibility = View.GONE
                btnMarkReturning?.visibility = View.GONE
                btnCompleteMission?.visibility = View.VISIBLE
                btnCompleteMission?.isEnabled = state.isWithinCompletionRange
            }
            else -> {}
        }
        
        // Update owner and pet info
        val mission = state.activeMission
        val tvOwnerName = layoutActiveMission?.findViewById<TextView>(R.id.tvOwnerName)
        val tvOwnerInitial = layoutActiveMission?.findViewById<TextView>(R.id.tvOwnerInitial)
        val tvPetNames = layoutActiveMission?.findViewById<TextView>(R.id.tvPetNames)
        val tvAddress = layoutActiveMission?.findViewById<TextView>(R.id.tvAddress)
        
        val ownerName = mission?.owner?.let { owner ->
            val fullName = "${owner.firstName} ${owner.lastName}".trim()
            fullName.ifEmpty { owner.name.ifEmpty { "Propriétaire" } }
        } ?: "Propriétaire"
        tvOwnerName?.text = ownerName
        
        val initial = mission?.owner?.firstName?.firstOrNull()
            ?: mission?.owner?.name?.firstOrNull()
            ?: 'P'
        tvOwnerInitial?.text = initial.uppercase().toString()
        
        // Use pet names from owner info if available
        val petNamesText = mission?.owner?.petNames?.takeIf { it.isNotEmpty() }?.joinToString(", ")
            ?: "${mission?.petIds?.size ?: 0} chien${if ((mission?.petIds?.size ?: 0) > 1) "s" else ""}"
        tvPetNames?.text = petNamesText
        
        // Update address
        tvAddress?.text = mission?.location?.address ?: ""
    }

    private fun updatePendingMissionCard(mission: PetsitterMission, countdown: Int) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        val contentView = container.getChildAt(0) ?: return
        
        // Update owner info
        val tvPendingOwnerName = contentView.findViewById<TextView>(R.id.tvPendingOwnerName)
        val tvPendingOwnerInitial = contentView.findViewById<TextView>(R.id.tvPendingOwnerInitial)
        val ownerName = mission.ownerName.ifBlank { "Propriétaire" }
        tvPendingOwnerName?.text = ownerName
        tvPendingOwnerInitial?.text = ownerName.firstOrNull()?.uppercase() ?: "P"
        
        // Update pet names
        val tvPendingPetNames = contentView.findViewById<TextView>(R.id.tvPendingPetNames)
        val petNamesText = if (mission.petNames.isNotEmpty()) {
            mission.petNames.joinToString(", ")
        } else {
            getString(R.string.pet_default_name)
        }
        tvPendingPetNames?.text = petNamesText
        
        // Update duration
        val tvPendingDuration = contentView.findViewById<TextView>(R.id.tvPendingDuration)
        tvPendingDuration?.text = "${mission.duration} min"
        
        // Update distance
        val tvPendingDistance = contentView.findViewById<TextView>(R.id.tvPendingDistance)
        val distanceText = if (mission.distance > 0) {
            "%.1f km".format(mission.distance)
        } else {
            "À proximité"
        }
        tvPendingDistance?.text = distanceText
    }

    private fun openMapsWithOwnerAddress() {
        val mission = petsitterViewModel.uiState.value.activeMission ?: return
        val location = mission.location
        val uri = when {
            !location.address.isNullOrBlank() -> Uri.parse("geo:0,0?q=" + Uri.encode(location.address))
            location.lat != 0.0 && location.lng != 0.0 -> Uri.parse("geo:${location.lat},${location.lng}?q=${location.lat},${location.lng}")
            else -> return
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun requestPetsitterLocationPermissionAndGoOnline() {
        when {
            hasLocationPermission() -> petsitterViewModel.toggleOnline(true)
            else -> petsitterLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun updatePetsitterStats(profile: PetsitterProfile?) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        val contentView = container.getChildAt(0) ?: return
        val tvStatMissions = contentView.findViewById<TextView>(R.id.tvStatMissions) ?: return
        // Use completedMissionsCount from state - counts actual completed missions
        val count = viewModel.state.value.completedMissionsCount
        tvStatMissions.text = count.toString()
        android.util.Log.d("DashboardFragment", "Updated stats: completedMissionsCount=$count")
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

        // Setup Walk FAB (only if owner path feature is enabled)
        fabRequestWalk = view.findViewById<ExtendedFloatingActionButton>(R.id.fabRequestWalk)
        if (!ownerPathEnabled) {
            fabRequestWalk?.visibility = View.GONE
        }
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
        fabRequestWalk?.visibility = if (hasActiveWalk || !hasPets || !ownerPathEnabled) View.GONE else View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        walkTrackingMapView?.onStart()
        petsitterMapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        walkTrackingMapView?.onStop()
        petsitterMapView?.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        lastShownPendingRequestId = null
        switchOnline = null
        tvOnlineStatus = null
        tvOnlineSubtitle = null
        viewOnlineBackground = null
        ivOnlineIcon = null
        progressOnline = null
        layoutActiveMission = null
        layoutOnlineWaiting = null
        layoutOffline = null
        layoutPendingMission = null
        walkTrackingMapView?.onDestroy()
        walkTrackingMapView = null
        petsitterMapView?.onDestroy()
        petsitterMapView = null
    }
}
