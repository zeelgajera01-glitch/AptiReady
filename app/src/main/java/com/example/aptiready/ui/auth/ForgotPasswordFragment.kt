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
import com.example.aptiready.databinding.FragmentForgotPasswordBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForgotPasswordViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        ForgotPasswordViewModel.Factory(app.appContainer.authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnBackLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text?.toString() ?: ""
            viewModel.sendPasswordReset(email)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
                        binding.btnSendReset.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.isSubmitted.collectLatest { isSubmitted ->
                        if (isSubmitted) {
                            binding.cardConfirmation.visibility = View.VISIBLE
                        } else {
                            binding.cardConfirmation.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collectLatest { err ->
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