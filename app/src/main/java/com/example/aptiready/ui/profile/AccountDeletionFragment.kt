package com.example.aptiready.ui.profile

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
import com.example.aptiready.databinding.FragmentAccountDeletionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountDeletionFragment : Fragment() {

    private var _binding: FragmentAccountDeletionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountDeletionViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        AccountDeletionViewModel.Factory(
            app.appContainer.authRepository,
            app.appContainer.cloudSyncRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountDeletionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCancelDeletion.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnConfirmDeletion.setOnClickListener {
            val pwd = binding.etReauthPassword.text?.toString() ?: ""
            viewModel.deleteAccount(pwd) {
                Toast.makeText(requireContext(), "Account and cloud data deleted successfully.", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.action_accountDeletion_to_welcome)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isDeleting.collectLatest { isDeleting ->
                        binding.progressIndicator.visibility = if (isDeleting) View.VISIBLE else View.INVISIBLE
                        binding.btnConfirmDeletion.isEnabled = !isDeleting
                    }
                }

                launch {
                    viewModel.error.collectLatest { err ->
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