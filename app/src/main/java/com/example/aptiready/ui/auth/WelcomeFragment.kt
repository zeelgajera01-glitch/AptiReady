package com.example.aptiready.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.R
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.example.aptiready.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WelcomeViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        WelcomeViewModel.Factory(app.appContainer.authRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkFirebaseConfig()

        binding.btnCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_register)
        }

        binding.btnSignIn.setOnClickListener {
            findNavController().navigate(R.id.action_welcome_to_login)
        }

        observeAuthState()
    }

    private fun checkFirebaseConfig() {
        if (!viewModel.isFirebaseConfigured) {
            binding.cardFirebaseWarning.visibility = View.VISIBLE
            binding.tvConfigWarningMsg.text = FirebaseConfigManager.getConfigurationMessage()
        } else {
            binding.cardFirebaseWarning.visibility = View.GONE
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.authState.collectLatest { state ->
                    when (state) {
                        is AuthState.SignedInVerified,
                        is AuthState.SignedInUnverified -> {
                            if (findNavController().currentDestination?.id == R.id.welcomeFragment) {
                                findNavController().navigate(R.id.action_welcome_to_home)
                            }
                        }
                        else -> {}
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
