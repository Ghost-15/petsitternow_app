package www.com.petsitternow_app.ui.petsitter

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.PetsitterMission
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.domain.navigation.RouteProtectionResult
import www.com.petsitternow_app.domain.navigation.RouteProtectionManager
import www.com.petsitternow_app.ui.walk.WalkTrackingViewModel
import www.com.petsitternow_app.view.map.WalkTrackingMapView
import javax.inject.Inject

/**
 * Fragment displaying missions for petsitters.
 */
@AndroidEntryPoint
class MissionsFragment : Fragment(R.layout.fragment_missions) {

    @Inject
    lateinit var routeProtectionManager: RouteProtectionManager

    private val viewModel: PetsitterMissionsViewModel by viewModels()
    private val walkTrackingViewModel: WalkTrackingViewModel by viewModels()

    // Online toggle views
    private var switchOnline: SwitchMaterial? = null
    private var tvOnlineStatus: TextView? = null
    private var progressToggle: ProgressBar? = null

    // Active mission views
    private var layoutActiveMission: View? = null
    private var tvMissionStatus: TextView? = null
    private var tvMissionTimer: TextView? = null
    private var tvMissionDistance: TextView? = null
    private var tvOwnerName: TextView? = null
    private var tvOwnerInitial: TextView? = null
    private var tvPetNames: TextView? = null
    private var btnOpenMaps: MaterialButton? = null
    private var btnStartWalk: MaterialButton? = null
    private var btnMarkReturning: MaterialButton? = null
    private var btnCompleteMission: MaterialButton? = null
    private var viewMissionStatusBackground: View? = null
    private var ivMissionStatusIcon: ImageView? = null

    // Waiting/offline state views
    private var layoutWaiting: View? = null
    private var layoutOffline: View? = null


    // Map view
    private var mapContainer: androidx.cardview.widget.CardView? = null
    private var walkTrackingMapView: WalkTrackingMapView? = null

    // Pending mission dialog: show only once per requestId
    private var lastShownPendingRequestId: String? = null
    private var pendingMissionDialog: androidx.appcompat.app.AlertDialog? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.toggleOnline(true)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            when (routeProtectionManager.protectPetsitterRoute()) {
                RouteProtectionResult.Allowed -> {
                    initViews(view)
                    setupOnlineToggle()
                    setupMissionButtons()
                    observeState()
                }
                RouteProtectionResult.FeatureDisabled -> {
                    Snackbar.make(view, R.string.feature_temporarily_unavailable, Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
                RouteProtectionResult.WrongRole, RouteProtectionResult.NotAuthenticated -> {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun initViews(view: View) {
        // Online toggle
        switchOnline = view.findViewById(R.id.switchOnline)
        tvOnlineStatus = view.findViewById(R.id.tvOnlineStatus)
        progressToggle = view.findViewById(R.id.progressOnline)

        // Active mission (from included layout)
        layoutActiveMission = view.findViewById(R.id.layoutActiveMission)
        // These IDs are inside layout_active_mission.xml
        layoutActiveMission?.let { activeMissionView ->
            tvMissionStatus = activeMissionView.findViewById(R.id.tvStatusTitle)
            tvMissionTimer = activeMissionView.findViewById(R.id.tvTimer)
            tvMissionDistance = activeMissionView.findViewById(R.id.tvDistance)
            tvOwnerName = activeMissionView.findViewById(R.id.tvOwnerName)
            tvOwnerInitial = activeMissionView.findViewById(R.id.tvOwnerInitial)
            tvPetNames = activeMissionView.findViewById(R.id.tvPetNames)
            btnOpenMaps = activeMissionView.findViewById(R.id.btnOpenMaps)
            btnStartWalk = activeMissionView.findViewById(R.id.btnStartWalk)
            btnMarkReturning = activeMissionView.findViewById(R.id.btnMarkReturning)
            btnCompleteMission = activeMissionView.findViewById(R.id.btnCompleteMission)
            viewMissionStatusBackground = activeMissionView.findViewById(R.id.viewStatusBackground)
            ivMissionStatusIcon = activeMissionView.findViewById(R.id.ivStatusIcon)
        }

        // Waiting/offline states
        layoutWaiting = view.findViewById(R.id.layoutOnlineWaiting)
        layoutOffline = view.findViewById(R.id.layoutOffline)


        // Map view - inside the active mission card
        mapContainer = layoutActiveMission?.findViewById(R.id.mapContainer)
        walkTrackingMapView = WalkTrackingMapView(requireContext()).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        mapContainer?.addView(walkTrackingMapView)
    }

    private fun setupOnlineToggle() {
        switchOnline?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermissionAndGoOnline()
            } else {
                viewModel.toggleOnline(false)
            }
        }
    }

    private fun setupMissionButtons() {
        btnOpenMaps?.setOnClickListener { openMapsWithOwnerAddress() }
        btnStartWalk?.setOnClickListener { viewModel.startWalk() }
        btnMarkReturning?.setOnClickListener { viewModel.markReturning() }
        btnCompleteMission?.setOnClickListener { viewModel.completeMission() }

    }

    private fun openMapsWithOwnerAddress() {
        val mission = viewModel.uiState.value.activeMission ?: return
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

    private fun observeState() {
        // Observe walk tracking for map updates
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
                        // Update map with locations from active mission
                        viewModel.uiState.value.activeMission?.location?.let { ownerLocation ->
                            walkTrackingMapView?.setOwnerLocation(ownerLocation)
                        }
                        // Petsitter location comes from current location
                        viewModel.uiState.value.currentLocation?.let { petsitterLocation ->
                            walkTrackingMapView?.setPetsitterLocation(petsitterLocation)
                        }
                        // Update route
                        walkTrackingMapView?.setRoute(trackingState.route, activeWalk?.status)
                    }

                    // Update timer from tracking ViewModel (updates every second)
                    if (trackingState.formattedTime.isNotEmpty()) {
                        tvMissionTimer?.text = trackingState.formattedTime
                    }
                    
                    // Update buttons based on RTDB status, sauf si Firestore est final
                    val firestoreStatus = viewModel.uiState.value.activeMission?.status
                    if (firestoreStatus != null && firestoreStatus !in WalkStatus.FINAL_STATUSES) {
                        activeWalk?.status?.let { rtdbStatus ->
                            updateMissionStatusUI(rtdbStatus, viewModel.uiState.value)
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Update toggle state
                    switchOnline?.isChecked = state.isOnline
                    tvOnlineStatus?.text = if (state.isOnline) "En ligne" else "Hors ligne"

                    if (state.isTogglingOnline) {
                        progressToggle?.visibility = View.VISIBLE
                        switchOnline?.isEnabled = false
                    } else {
                        progressToggle?.visibility = View.GONE
                        switchOnline?.isEnabled = true
                    }

                    // Handle pending mission - show dialog once per mission
                    state.pendingMission?.let { mission ->
                        if (mission.requestId != lastShownPendingRequestId && !mission.isExpired()) {
                            lastShownPendingRequestId = mission.requestId
                            showPendingMissionDialog(mission, state.missionCountdown)
                        }
                    } ?: run {
                        lastShownPendingRequestId = null
                        pendingMissionDialog?.dismiss()
                        pendingMissionDialog = null
                    }

                    // Handle active mission
                    state.activeMission?.let { mission ->
                        layoutActiveMission?.visibility = View.VISIBLE
                        layoutWaiting?.visibility = View.GONE
                        layoutOffline?.visibility = View.GONE

                        walkTrackingViewModel.setRequestId(mission.id)

                        val rtdbStatus = walkTrackingViewModel.uiState.value.activeWalk?.status
                        val effectiveStatus = rtdbStatus ?: mission.status
                        updateMissionStatusUI(effectiveStatus, state)

                        val ownerName = mission.owner.let { owner ->
                            val fullName = "${owner.firstName} ${owner.lastName}".trim()
                            fullName.ifEmpty { owner.name.ifEmpty { "Propriétaire" } }
                        }
                        tvOwnerName?.text = ownerName

                        val initial = mission.owner.firstName.firstOrNull()
                            ?: mission.owner.name.firstOrNull()
                            ?: 'P'
                        tvOwnerInitial?.text = initial.uppercase().toString()

                        val petNames = mission.owner.pets.map { it.name }
                        val petNamesText = petNames.takeIf { it.isNotEmpty() }?.joinToString(", ")
                            ?: "${mission.owner.pets.size} chien${if (mission.owner.pets.size > 1) "s" else ""}"
                        tvPetNames?.text = petNamesText
                    } ?: run {
                        layoutActiveMission?.visibility = View.GONE

                        if (state.isOnline) {
                            layoutWaiting?.visibility = View.VISIBLE
                            layoutOffline?.visibility = View.GONE
                        } else {
                            layoutWaiting?.visibility = View.GONE
                            layoutOffline?.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun updateMissionStatusUI(status: WalkStatus, state: PetsitterMissionsUiState) {
        when (status) {
            WalkStatus.ASSIGNED, WalkStatus.GOING_TO_OWNER -> {
                tvMissionStatus?.text = "Mission assignée"
                viewMissionStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_assigned)
                ivMissionStatusIcon?.setImageResource(R.drawable.ic_check_circle)
                btnStartWalk?.visibility = View.VISIBLE
                btnMarkReturning?.visibility = View.GONE
                btnCompleteMission?.visibility = View.GONE
            }
            WalkStatus.WALKING, WalkStatus.IN_PROGRESS -> {
                tvMissionStatus?.text = "Promenade en cours"
                viewMissionStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_walking)
                ivMissionStatusIcon?.setImageResource(R.drawable.ic_walk)
                tvMissionTimer?.visibility = View.VISIBLE
                btnStartWalk?.visibility = View.GONE
                btnMarkReturning?.visibility = View.VISIBLE
                btnCompleteMission?.visibility = View.GONE
            }
            WalkStatus.RETURNING -> {
                tvMissionStatus?.text = "Retour en cours"
                viewMissionStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_returning)
                ivMissionStatusIcon?.setImageResource(R.drawable.ic_home)
                tvMissionTimer?.visibility = View.VISIBLE
                state.distanceToOwner?.let { distance ->
                    tvMissionDistance?.text = "${distance.toInt()}m"
                    tvMissionDistance?.visibility = View.VISIBLE
                }
                btnStartWalk?.visibility = View.GONE
                btnMarkReturning?.visibility = View.GONE
                btnCompleteMission?.visibility = View.VISIBLE
                btnCompleteMission?.isEnabled = state.isWithinCompletionRange
            }
            else -> {}
        }
    }
    
    private fun showPendingMissionDialog(mission: PetsitterMission, countdown: Int) {
        pendingMissionDialog?.dismiss()
        val petNamesText = if (mission.petNames.isNotEmpty()) {
            mission.petNames.joinToString(", ")
        } else {
            getString(R.string.pet_default_name)
        }
        val distanceText = if (mission.distance > 0) {
            "%.1f km".format(mission.distance)
        } else {
            "—"
        }
        val message = getString(
            R.string.mission_notification_dialog_message,
            petNamesText,
            mission.duration,
            distanceText,
            countdown
        )
        pendingMissionDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.mission_notification_dialog_title)
            .setMessage(message)
            .setNegativeButton(R.string.decline) { dialog, _ ->
                viewModel.declineMission()
                dialog.dismiss()
                pendingMissionDialog = null
            }
            .setPositiveButton(R.string.accept) { dialog, _ ->
                viewModel.acceptMission()
                dialog.dismiss()
                pendingMissionDialog = null
            }
            .setCancelable(true)
            .setOnCancelListener {
                viewModel.declineMission()
                pendingMissionDialog = null
            }
            .show()
    }

    private fun requestLocationPermissionAndGoOnline() {
        when {
            hasLocationPermission() -> viewModel.toggleOnline(true)
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
        pendingMissionDialog?.dismiss()
        pendingMissionDialog = null
        lastShownPendingRequestId = null
        switchOnline = null
        tvOnlineStatus = null
        progressToggle = null
        layoutActiveMission = null
        tvMissionStatus = null
        tvMissionTimer = null
        tvMissionDistance = null
        tvOwnerName = null
        tvOwnerInitial = null
        tvPetNames = null
        btnOpenMaps = null
        btnStartWalk = null
        btnMarkReturning = null
        btnCompleteMission = null
        viewMissionStatusBackground = null
        ivMissionStatusIcon = null
        layoutWaiting = null
        layoutOffline = null
        walkTrackingMapView?.onDestroy()
        walkTrackingMapView = null
        mapContainer = null
    }
}
