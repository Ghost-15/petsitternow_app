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
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.view.adapter.MissionHistoryAdapter
import www.com.petsitternow_app.view.fragment.RatingSheetHelper

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
    private var tvStatLabel: TextView? = null
    private var tvSubtitle: TextView? = null
    private var tvEmptyTitle: TextView? = null
    private var tvEmptySubtitle: TextView? = null

    private var missionHistoryAdapter: MissionHistoryAdapter? = null
    private var lastAdapterRole: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        RatingSheetHelper.setupRatingResultListener(this, { viewModel.refresh() }) { r, s, c, v ->
            if (v == "petsitter") viewModel.submitWalkRating(r, s, c) else viewModel.submitOwnerRating(r, s, c)
        }
        initViews(view)
        setupRecyclerView(viewModel.uiState.value.userRole)
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
        tvStatLabel = view.findViewById(R.id.tvStatLabel)
        tvSubtitle = view.findViewById(R.id.tvSubtitle)
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle)
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle)

        btnRetry?.setOnClickListener { viewModel.refresh() }
    }

    private fun setupRecyclerView(userRole: String?) {
        missionHistoryAdapter = MissionHistoryAdapter(
            missions = emptyList(),
            userRole = userRole,
            onClick = { },
            onRatePetsitter = { walk -> showRatingSheet(walk, variant = "petsitter") },
            onRateOwner = { walk -> showRatingSheet(walk, variant = "owner") }
        )
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = missionHistoryAdapter
    }

    private fun showRatingSheet(walk: WalkRequest, variant: String) {
        RatingSheetHelper.showRatingSheet(this, walk, variant)
    }

    private fun updateLabelsForRole(userRole: String?) {
        val isOwner = userRole == "owner"
        tvStatLabel?.text = getString(if (isOwner) R.string.stat_walks else R.string.stat_missions)
        tvSubtitle?.text = getString(if (isOwner) R.string.subtitle_history_owner else R.string.subtitle_history)
        tvEmptyTitle?.text = getString(if (isOwner) R.string.empty_history_title_owner else R.string.empty_history_title)
        tvEmptySubtitle?.text = getString(if (isOwner) R.string.empty_history_subtitle_owner else R.string.empty_history_subtitle)
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

                    // Update labels and adapter when role is known
                    state.userRole?.let { role ->
                        updateLabelsForRole(role)
                        if (lastAdapterRole != role) {
                            setupRecyclerView(role)
                            lastAdapterRole = role
                        }
                    }

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
        tvStatLabel = null
        tvSubtitle = null
        tvEmptyTitle = null
        tvEmptySubtitle = null
        missionHistoryAdapter = null
        lastAdapterRole = null
    }
}
