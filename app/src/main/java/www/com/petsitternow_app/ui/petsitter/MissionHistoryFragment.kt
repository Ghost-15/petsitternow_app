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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.view.adapter.MissionHistoryAdapter

/**
 * Fragment displaying mission history for petsitters.
 */
@AndroidEntryPoint
class MissionHistoryFragment : Fragment(R.layout.fragment_mission_history) {

    private val viewModel: MissionHistoryViewModel by viewModels()

    private var recyclerView: RecyclerView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var progressBar: ProgressBar? = null
    private var layoutEmpty: View? = null
    private var layoutError: View? = null
    private var tvError: TextView? = null
    private var btnRetry: View? = null
    private var tvTotalMissions: TextView? = null
    private var tvTotalEarnings: TextView? = null
    private var tvTotalDistance: TextView? = null

    private var missionHistoryAdapter: MissionHistoryAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupRecyclerView()
        setupSwipeRefresh()
        observeState()
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
        tvTotalEarnings = view.findViewById(R.id.tvTotalEarnings)
        tvTotalDistance = view.findViewById(R.id.tvTotalDistance)

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

                            // Calculate total earnings (assuming 10€ per 30min)
                            val totalEarnings = state.missions.sumOf { mission ->
                                ((mission.duration.toIntOrNull() ?: 30) / 30) * 10
                            }
                            tvTotalEarnings?.text = "€$totalEarnings"

                            // Calculate total distance (placeholder)
                            val totalDistance = state.missions.size * 2.5 // Rough estimate
                            tvTotalDistance?.text = String.format("%.1f km", totalDistance)
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
        tvTotalEarnings = null
        tvTotalDistance = null
        missionHistoryAdapter = null
    }
}
