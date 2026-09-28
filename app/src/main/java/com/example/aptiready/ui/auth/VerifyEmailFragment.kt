package com.example.aptiready.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.databinding.FragmentVerifyEmailBinding

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
        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}