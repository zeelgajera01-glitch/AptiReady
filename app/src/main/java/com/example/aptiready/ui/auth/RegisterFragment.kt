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
import com.example.aptiready.databinding.FragmentRegisterBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        RegisterViewModel.Factory(app.appContainer.authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        checkFirebaseConfig()

        binding.btnRegister.setOnClickListener {
            val name = binding.etDisplayName.text?.toString() ?: ""
            val email = binding.etEmail.text?.toString() ?: ""
            val password = binding.etPassword.text?.toString() ?: ""
            val confirm = binding.etConfirmPassword.text?.toString() ?: ""

            viewModel.register(name, email, password, confirm) {
                findNavController().navigate(R.id.action_register_to_verifyEmail)
            }
        }

        binding.btnLinkLogin.setOnClickListener {
            findNavController().navigate(R.id.action_register_to_login)
        }

        observeViewModel()
    }

    private fun checkFirebaseConfig() {
        binding.cardFirebaseWarning.visibility = View.GONE
        binding.btnRegister.isEnabled = true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
                        binding.btnRegister.isEnabled = !isLoading && viewModel.isFirebaseConfigured
                    }
                }

                launch {
                    viewModel.registerError.collectLatest { err ->
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
