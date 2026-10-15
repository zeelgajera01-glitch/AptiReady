package com.example.aptiready.ui.auth

import com.example.aptiready.ui.common.navigateSafely
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.R
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.databinding.FragmentWelcomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private var signInPending = false

    private lateinit var googleSignInClient: GoogleSignInClient

    private val viewModel: WelcomeViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        WelcomeViewModel.Factory(app.appContainer.authRepository)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        signInPending = false
        _binding?.btnGoogleSignIn?.isEnabled = true
        if (_binding == null || !isAdded) return@registerForActivityResult
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrEmpty()) {
                viewModel.signInWithGoogle(
                    idToken = idToken,
                    onSuccess = {
                        if (!isAdded || _binding == null) return@signInWithGoogle
                        if (findNavController().currentDestination?.id == R.id.welcomeFragment) {
                            navigateSafely(R.id.action_welcome_to_home)
                        }
                    },
                    onError = { errorMsg ->
                        if (!isAdded || _binding == null) return@signInWithGoogle
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                Toast.makeText(requireContext(), "Google sign-in failed: ID Token missing.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(requireContext(), "Google sign-in canceled or failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
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

        setupGoogleClient()

        binding.btnGoogleSignIn.setOnClickListener {
            if (signInPending || viewModel.isLoading.value) return@setOnClickListener
            signInPending = true
            binding.btnGoogleSignIn.isEnabled = false
            googleSignInClient.signOut().addOnCompleteListener {
                if (!isAdded || _binding == null ||
                    !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    signInPending = false
                    return@addOnCompleteListener
                }
                try {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                } catch (failure: Exception) {
                    signInPending = false
                    binding.btnGoogleSignIn.isEnabled = true
                    Toast.makeText(requireContext(), "Could not open Google sign-in. Please try again.", Toast.LENGTH_LONG).show()
                }
            }
        }

        observeViewModel()
    }

    private fun setupGoogleClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collectLatest { isLoading ->
                        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
                        binding.btnGoogleSignIn.isEnabled = !isLoading && !signInPending
                    }
                }

                launch {
                    viewModel.authState.collectLatest { state ->
                        when (state) {
                            is AuthState.SignedInVerified,
                            is AuthState.SignedInUnverified -> {
                                if (findNavController().currentDestination?.id == R.id.welcomeFragment) {
                                    navigateSafely(R.id.action_welcome_to_home)
                                }
                            }
                            else -> {}
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
