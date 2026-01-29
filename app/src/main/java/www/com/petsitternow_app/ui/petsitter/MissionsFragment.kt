package www.com.petsitternow_app.ui.petsitter

import android.Manifest
import android.content.pm.PackageManager
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkStatus
import www.com.petsitternow_app.ui.walk.WalkTrackingViewModel
import www.com.petsitternow_app.view.map.WalkTrackingMapView

/**
 * Fragment displaying missions for petsitters.
 */
@AndroidEntryPoint
class MissionsFragment : Fragment(R.layout.fragment_missions) {

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
        initViews(view)
        setupOnlineToggle()
        setupMissionButtons()
        observeState()
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

        // Mission notification is not inline in this simple version - we'll handle via dialog
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
        btnStartWalk?.setOnClickListener { viewModel.startWalk() }
        btnMarkReturning?.setOnClickListener { viewModel.markReturning() }
        btnCompleteMission?.setOnClickListener { viewModel.completeMission() }
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

                    // Handle pending mission - show dialog if needed
                    state.pendingMission?.let { mission ->
                        // TODO: Show mission notification dialog
                    }

                    // Handle active mission
                    state.activeMission?.let { mission ->
                        layoutActiveMission?.visibility = View.VISIBLE
                        layoutWaiting?.visibility = View.GONE
                        layoutOffline?.visibility = View.GONE

                        // Setup walk tracking for map
                        walkTrackingViewModel.setRequestId(mission.id)

                        // Update mission status UI
                        when (mission.status) {
                            WalkStatus.ASSIGNED -> {
                                tvMissionStatus?.text = "Mission assignée"
                                viewMissionStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_assigned)
                                ivMissionStatusIcon?.setImageResource(R.drawable.ic_check_circle)
                                btnStartWalk?.visibility = View.VISIBLE
                                btnMarkReturning?.visibility = View.GONE
                                btnCompleteMission?.visibility = View.GONE
                            }
                            WalkStatus.WALKING -> {
                                tvMissionStatus?.text = "Promenade en cours"
                                viewMissionStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_walking)
                                ivMissionStatusIcon?.setImageResource(R.drawable.ic_walk)
                                // Timer text is updated by walkTrackingViewModel observer
                                tvMissionTimer?.visibility = View.VISIBLE
                                btnStartWalk?.visibility = View.GONE
                                btnMarkReturning?.visibility = View.VISIBLE
                                btnCompleteMission?.visibility = View.GONE
                            }
                            WalkStatus.RETURNING -> {
                                tvMissionStatus?.text = "Retour en cours"
                                viewMissionStatusBackground?.setBackgroundResource(R.drawable.bg_walk_status_returning)
                                ivMissionStatusIcon?.setImageResource(R.drawable.ic_home)
                                // Timer text is updated by walkTrackingViewModel observer
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

                        // Update owner info
                        tvOwnerName?.text = "Propriétaire"
                        tvOwnerInitial?.text = "P"
                        tvPetNames?.text = mission.petIds.joinToString(", ")
                    } ?: run {
                        layoutActiveMission?.visibility = View.GONE

                        // Show waiting or offline state
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
