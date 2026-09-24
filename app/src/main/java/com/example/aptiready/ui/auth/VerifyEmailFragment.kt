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
import com.example.aptiready.databinding.FragmentVerifyEmailBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VerifyEmailFragment : Fragment() {

    private var _binding: FragmentVerifyEmailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VerifyEmailViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        VerifyEmailViewModel.Factory(
            app.appContainer.authRepository,
            app.appContainer.profileRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerifyEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvUserEmail.text = viewModel.currentUserEmail

        binding.btnIHaveVerified.setOnClickListener {
            viewModel.checkVerification(
                onVerified = {
                    Toast.makeText(requireContext(), "Email verified! Welcome to AptiRise.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_verifyEmail_to_home)
                },
                onUnverified = { msg ->
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            )
        }

        binding.btnResendEmail.setOnClickListener {
            viewModel.resendVerification()
        }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            findNavController().navigate(R.id.action_verifyEmail_to_welcome)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
                        binding.btnIHaveVerified.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.cooldownSeconds.collectLatest { seconds ->
                        if (seconds > 0) {
                            binding.btnResendEmail.isEnabled = false
                            binding.tvCooldownStatus.visibility = View.VISIBLE
                            binding.tvCooldownStatus.text = "Resend available in ${seconds}s"
                        } else {
                            binding.btnResendEmail.isEnabled = true
                            binding.tvCooldownStatus.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.statusMessage.collectLatest { msg ->
                        if (msg != null) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
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