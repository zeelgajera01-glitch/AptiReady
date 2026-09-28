package com.example.aptiready.ui.auth

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

    private lateinit var googleSignInClient: GoogleSignInClient

    private val viewModel: WelcomeViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        WelcomeViewModel.Factory(app.appContainer.authRepository)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (!idToken.isNullOrEmpty()) {
                viewModel.signInWithGoogle(
                    idToken = idToken,
                    onSuccess = {
                        if (findNavController().currentDestination?.id == R.id.welcomeFragment) {
                            findNavController().navigate(R.id.action_welcome_to_home)
                        }
                    },
                    onError = { errorMsg ->
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
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
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
                        binding.btnGoogleSignIn.isEnabled = !isLoading
                    }
                }

                launch {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
