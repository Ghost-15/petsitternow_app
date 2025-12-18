package www.com.petsitternow_app.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import www.com.petsitternow_app.R

class Step3Fragment : Fragment(R.layout.fragment_onboarding_step3) {

    private lateinit var etAddress: EditText
    private lateinit var etCity: EditText
    private lateinit var etCodePostal: EditText
    private lateinit var btnPrevious: Button
    private lateinit var btnFinish: Button
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        etAddress = view.findViewById(R.id.etAddress)
        etCity = view.findViewById(R.id.etCity)
        etCodePostal = view.findViewById(R.id.etCodePostal)
        btnPrevious = view.findViewById(R.id.btnPrevious)
        btnFinish = view.findViewById(R.id.btnFinish)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupClickListeners() {
        btnPrevious.setOnClickListener {
            findNavController().popBackStack()
        }

        btnFinish.setOnClickListener {
            val address = etAddress.text.toString().trim()
            val city = etCity.text.toString().trim()
            val codePostal = etCodePostal.text.toString().trim()

            val error = when {
                address.length < 5 -> "L'adresse doit contenir au moins 5 caractères"
                city.length < 2 -> "La ville doit contenir au moins 2 caractères"
                !codePostal.matches(Regex("^[0-9]{5}$")) -> "Le code postal doit contenir 5 chiffres"
                else -> null
            }

            if (error != null) {
                tvError.text = error
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
                Toast.makeText(requireContext(), "Onboarding terminé ✓", Toast.LENGTH_LONG).show()
                // TODO: Sauvegarder les données et rediriger vers le dashboard
            }
        }
    }
}

