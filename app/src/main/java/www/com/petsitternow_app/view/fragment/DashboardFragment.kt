package www.com.petsitternow_app.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private var contentView: View? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when {
                        state.isLoading -> {
                            showLoading()
                        }
                        state.error != null -> {
                            showError(state.error)
                        }
                        state.userType != null -> {
                            updateView(state.userType)
                        }
                        else -> {
                            updateView("owner")
                        }
                    }
                }
            }
        }
    }

    private fun showLoading() {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        container.removeAllViews()
        
        val loadingView = layoutInflater.inflate(
            R.layout.fragment_dashboard_loading,
            container,
            false
        )
        container.addView(loadingView)
    }

    private fun showError(error: String) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return
        container.removeAllViews()
        
        val errorView = layoutInflater.inflate(
            R.layout.fragment_dashboard_error,
            container,
            false
        )
        errorView.findViewById<TextView>(R.id.tvError)?.text = error
        container.addView(errorView)
    }

    private fun updateView(userType: String?) {
        val container = view?.findViewById<FrameLayout>(R.id.container) ?: return

        contentView?.let { container.removeView(it) }

        val layoutId = when (userType) {
            "petsitter" -> R.layout.fragment_dashboard_petsitter
            "owner" -> R.layout.fragment_dashboard_owner
            else -> R.layout.fragment_dashboard_owner
        }

        contentView = layoutInflater.inflate(layoutId, container, false)
        container.addView(contentView)

        if (userType == "petsitter") {
            setupPetsitterView(contentView!!)
        } else if (userType == "owner") {
            setupOwnerView(contentView!!)
        }
    }

    private fun setupPetsitterView(view: View) {
        val btnMissions = view.findViewById<MaterialButton>(R.id.btnMissions)
        btnMissions?.setOnClickListener {
        }
    }

    private fun setupOwnerView(view: View) {
        val btnAddPet = view.findViewById<MaterialButton>(R.id.btnAddPet)
        btnAddPet?.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_addPet)
        }
    }
}