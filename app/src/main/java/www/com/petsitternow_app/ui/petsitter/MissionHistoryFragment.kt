package www.com.petsitternow_app.ui.petsitter

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.navigation.RouteProtectionManager
import www.com.petsitternow_app.domain.navigation.RouteProtectionResult
import www.com.petsitternow_app.view.adapter.MissionHistoryAdapter
import javax.inject.Inject

/**
 * Fragment displaying mission history for petsitters.
 */
@AndroidEntryPoint
class MissionHistoryFragment : Fragment(R.layout.fragment_mission_history) {

    @Inject
    lateinit var routeProtectionManager: RouteProtectionManager

    private val viewModel: MissionHistoryViewModel by viewModels()

    private var recyclerView: RecyclerView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var progressBar: ProgressBar? = null
    private var layoutEmpty: View? = null
    private var layoutError: View? = null
    private var tvError: TextView? = null
    private var btnRetry: View? = null
    private var tvTotalMissions: TextView? = null

    private var missionHistoryAdapter: MissionHistoryAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            when (routeProtectionManager.protectPetsitterRoute()) {
                RouteProtectionResult.Allowed -> {
                    initViews(view)
                    setupRecyclerView()
                    setupSwipeRefresh()
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
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)
        layoutError = view.findViewById(R.id.layoutError)
        tvError = view.findViewById(R.id.tvError)
        btnRetry = view.findViewById(R.id.btnRetry)
        tvTotalMissions = view.findViewById(R.id.tvTotalMissions)

        btnRetry?.setOnClickListener { viewModel.refresh() }
    }

    private fun setupRecyclerView() {
        missionHistoryAdapter = MissionHistoryAdapter(emptyList()) { mission ->
            // Handle mission item click if needed
        }
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = missionHistoryAdapter
    }

    private fun setupSwipeRefresh() {
        swipeRefresh?.setColorSchemeResources(R.color.primary)
        swipeRefresh?.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    swipeRefresh?.isRefreshing = false

                    when {
                        state.isLoading && state.missions.isEmpty() -> {
                            progressBar?.visibility = View.VISIBLE
                            recyclerView?.visibility = View.GONE
                            layoutEmpty?.visibility = View.GONE
                            layoutError?.visibility = View.GONE
                        }
                        state.error != null && state.missions.isEmpty() -> {
                            progressBar?.visibility = View.GONE
                            recyclerView?.visibility = View.GONE
                            layoutEmpty?.visibility = View.GONE
                            layoutError?.visibility = View.VISIBLE
                            tvError?.text = state.error
                        }
                        state.missions.isEmpty() -> {
                            progressBar?.visibility = View.GONE
                            recyclerView?.visibility = View.GONE
                            layoutEmpty?.visibility = View.VISIBLE
                            layoutError?.visibility = View.GONE
                        }
                        else -> {
                            progressBar?.visibility = View.GONE
                            recyclerView?.visibility = View.VISIBLE
                            layoutEmpty?.visibility = View.GONE
                            layoutError?.visibility = View.GONE
                            missionHistoryAdapter?.updateMissions(state.missions)

                            // Update stats
                            tvTotalMissions?.text = state.missions.size.toString()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        swipeRefresh = null
        progressBar = null
        layoutEmpty = null
        layoutError = null
        tvError = null
        btnRetry = null
        tvTotalMissions = null
        missionHistoryAdapter = null
    }
}
