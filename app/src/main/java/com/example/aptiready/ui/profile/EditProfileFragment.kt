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
import com.example.aptiready.data.model.ProfileState
import com.example.aptiready.databinding.FragmentEditProfileBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        EditProfileViewModel.Factory(
            app.appContainer.authRepository,
            app.appContainer.profileRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvProfileEmail.text = viewModel.currentUserEmail
        binding.tvVerificationBadge.text = if (viewModel.isVerified) "VERIFIED" else "UNVERIFIED"

        binding.btnSave.setOnClickListener {
            val name = binding.etDisplayName.text?.toString() ?: ""
            val goal = binding.etDailyGoal.text?.toString() ?: "10"
            viewModel.saveProfile(name, goal) {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        viewModel.loadProfile()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileState.collectLatest { state ->
                        if (state is ProfileState.Success) {
                            if (binding.etDisplayName.text.isNull_or_empty()) {
                                binding.etDisplayName.setText(state.profile.displayName)
                                binding.etDailyGoal.setText(state.profile.dailyGoal.toString())
                            }
                        }
                    }
                }

                launch {
                    viewModel.isSaving.collectLatest { isSaving ->
                        binding.progressIndicator.visibility = if (isSaving) View.VISIBLE else View.INVISIBLE
                        binding.btnSave.isEnabled = !isSaving
                    }
                }

                launch {
                    viewModel.saveError.collectLatest { err ->
                        if (err != null) {
                            Toast.makeText(requireContext(), err, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun CharSequence?.isNull_or_empty(): Boolean {
        return this == null || this.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}