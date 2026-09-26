package com.example.aptiready.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.R
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.example.aptiready.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        LoginViewModel.Factory(app.appContainer.authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        checkFirebaseConfig()

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""

            viewModel.login(
                email = email,
                password = password,
                onVerified = {
                    findNavController().navigate(
                        R.id.action_login_to_home
                    )
                },
                onNeedsVerification = {
                    findNavController().navigate(
                        R.id.action_login_to_verifyEmail
                    )
                }
            )
        }

        binding.btnForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPassword)
        }

        binding.btnLinkRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        observeViewModel()
    }

    private fun checkFirebaseConfig() {
        binding.cardFirebaseWarning.visibility = View.GONE
        binding.btnLogin.isEnabled = true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
                        binding.btnLogin.isEnabled = !isLoading && viewModel.isFirebaseConfigured
                    }
                }

                launch {
                    viewModel.loginError.collectLatest { err ->
                        if (err != null) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}