package www.com.petsitternow_app.ui.walk

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.model.WalkRequest
import www.com.petsitternow_app.domain.navigation.RouteProtectionManager
import www.com.petsitternow_app.domain.navigation.RouteProtectionResult
import www.com.petsitternow_app.view.adapter.WalkHistoryAdapter
import www.com.petsitternow_app.view.fragment.RatingBottomSheetDialogFragment
import javax.inject.Inject

/**
 * Fragment displaying walk history for owners.
 */
@AndroidEntryPoint
class WalkHistoryFragment : Fragment(R.layout.fragment_walk_history) {

    @Inject
    lateinit var routeProtectionManager: RouteProtectionManager

    private val viewModel: WalkHistoryViewModel by viewModels()

    private var recyclerView: RecyclerView? = null
    private var swipeRefresh: SwipeRefreshLayout? = null
    private var progressBar: ProgressBar? = null
    private var layoutEmpty: View? = null
    private var layoutError: View? = null
    private var tvError: TextView? = null
    private var btnRetry: View? = null

    private var walkHistoryAdapter: WalkHistoryAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            when (routeProtectionManager.protectOwnerRoute()) {
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

        btnRetry?.setOnClickListener { viewModel.refresh() }
    }

    private fun setupRecyclerView() {
        walkHistoryAdapter = WalkHistoryAdapter(
            walks = emptyList(),
            onClick = { },
            onRatePetsitter = { walk -> showRatingSheet(walk, variant = "petsitter") }
        )
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = walkHistoryAdapter
    }

    private fun showRatingSheet(walk: WalkRequest, variant: String) {
        val sheet = RatingBottomSheetDialogFragment.newInstance(
            requestId = walk.id,
            variant = variant,
            targetName = if (variant == "petsitter") walk.petsitter?.name else walk.owner.name
        )
        sheet.onSubmit = { requestId, score, comment, sheetFragment ->
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val result = viewModel.submitWalkRating(requestId, score, comment).first()
                    sheetFragment.setSubmitFinished()
                    result.fold(
                        onSuccess = {
                            sheetFragment.dismiss()
                            view?.let { Snackbar.make(it, R.string.rating_success, Snackbar.LENGTH_SHORT).show() }
                            viewModel.refresh()
                        },
                        onFailure = { e ->
                            view?.let { Snackbar.make(it, e.message ?: getString(R.string.rating_error_send), Snackbar.LENGTH_LONG).show() }
                        }
                    )
                } catch (e: Exception) {
                    sheetFragment.setSubmitFinished()
                    view?.let { Snackbar.make(it, e.message ?: getString(R.string.rating_error_send), Snackbar.LENGTH_LONG).show() }
                }
            }
        }
        sheet.show(childFragmentManager, "RatingBottomSheet")
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
                        state.isLoading && state.walks.isEmpty() -> {
                            progressBar?.visibility = View.VISIBLE
                            recyclerView?.visibility = View.GONE
                            layoutEmpty?.visibility = View.GONE
                            layoutError?.visibility = View.GONE
                        }
                        state.error != null && state.walks.isEmpty() -> {
                            progressBar?.visibility = View.GONE
                            recyclerView?.visibility = View.GONE
                            layoutEmpty?.visibility = View.GONE
                            layoutError?.visibility = View.VISIBLE
                            tvError?.text = state.error
                        }
                        state.walks.isEmpty() -> {
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
                            walkHistoryAdapter?.updateWalks(state.walks)

                            // Stats are optional - layout may not have them
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
        walkHistoryAdapter = null
    }
}
