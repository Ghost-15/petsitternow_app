package www.com.petsitternow_app.view.fragment

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R
import www.com.petsitternow_app.domain.repository.Pet

@AndroidEntryPoint
class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private val viewModel: DashboardViewModel by viewModels()
    private var petAdapter: PetAdapter? = null
    private var isViewSetup = false

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