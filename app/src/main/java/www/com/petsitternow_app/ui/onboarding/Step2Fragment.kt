package www.com.petsitternow_app.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import www.com.petsitternow_app.R

class Step2Fragment : Fragment(R.layout.fragment_onboarding_step2) {

    private lateinit var optionOwner: LinearLayout
    private lateinit var optionPetsitter: LinearLayout
    private lateinit var radioOwner: RadioButton
    private lateinit var radioPetsitter: RadioButton
    private lateinit var btnPrevious: Button
    private lateinit var btnNext: Button
    private lateinit var tvError: TextView

    private var selectedUserType: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        optionOwner = view.findViewById(R.id.optionOwner)
        optionPetsitter = view.findViewById(R.id.optionPetsitter)
        radioOwner = view.findViewById(R.id.radioOwner)
        radioPetsitter = view.findViewById(R.id.radioPetsitter)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnNext = view.findViewById(R.id.btnNext)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupClickListeners() {
        // Sélection Owner
        val selectOwner = {
            selectedUserType = "owner"
            radioOwner.isChecked = true
            radioPetsitter.isChecked = false
            optionOwner.isSelected = true
            optionPetsitter.isSelected = false
            tvError.visibility = View.GONE
        }

        // Sélection Petsitter
        val selectPetsitter = {
            selectedUserType = "petsitter"
            radioOwner.isChecked = false
            radioPetsitter.isChecked = true
            optionOwner.isSelected = false
            optionPetsitter.isSelected = true
            tvError.visibility = View.GONE
        }

        optionOwner.setOnClickListener { selectOwner() }
        radioOwner.setOnClickListener { selectOwner() }
        optionPetsitter.setOnClickListener { selectPetsitter() }
        radioPetsitter.setOnClickListener { selectPetsitter() }

        // Navigation
        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        btnNext.setOnClickListener {
            if (selectedUserType == null) {
                tvError.text = "Veuillez sélectionner un type d'utilisateur"
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
                findNavController().navigate(R.id.action_step2_to_step3)
            }
        }
    }
}

