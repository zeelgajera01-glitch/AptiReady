package com.example.aptiready.ui.auth

import com.example.aptiready.ui.common.navigateUpSafely
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.databinding.FragmentForgotPasswordBinding

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

        binding.btnBackLogin.setOnClickListener {
            navigateUpSafely()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}