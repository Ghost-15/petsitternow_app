package www.com.petsitternow_app.view.fragment

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
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import www.com.petsitternow_app.R

@AndroidEntryPoint
class ChangePasswordFragment : Fragment(R.layout.fragment_change_password) {

    private val viewModel: ChangePasswordViewModel by viewModels()

    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        observeState()
    }

    private fun initViews(view: View) {
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
    }

    private fun setupClickListeners() {
        btnChangePassword.setOnClickListener {
            viewModel.changePassword(
                currentPassword = etCurrentPassword.text.toString(),
                newPassword = etNewPassword.text.toString(),
                confirmPassword = etConfirmPassword.text.toString()
            )
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    btnChangePassword.isEnabled = !state.isLoading

                    if (state.error != null) {
                        tvError.text = state.error
                        tvError.visibility = View.VISIBLE
                    } else {
                        tvError.visibility = View.GONE
                    }

                    if (state.isSuccess) {
                        Snackbar.make(requireView(), "Mot de passe modifié avec succès", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }
}
