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
import com.example.aptiready.BuildConfig
import com.example.aptiready.R
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.model.ProfileState
import com.example.aptiready.data.model.SyncStatus
import com.example.aptiready.data.model.ThemeMode
import com.example.aptiready.databinding.FragmentProfileBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        ProfileViewModel.Factory(
            app.appContainer.themeRepository,
            app.appContainer.authRepository,
            app.appContainer.profileRepository,
            app.appContainer.cloudSyncRepository,
            app.appContainer.adProvider
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeRadioGroup()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupThemeRadioGroup() {
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = when (checkedId) {
                R.id.rb_theme_light -> ThemeMode.LIGHT
                R.id.rb_theme_dark -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            viewModel.selectTheme(selectedMode)
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }

        binding.cardBookmarksShortcut.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_bookmarks)
        }

        binding.btnOpenAdDiagnostics.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE
        binding.btnOpenAdDiagnostics.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_adDiagnostics)
        }

        binding.switchCloudBackup.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setBackupEnabled(isChecked)
        }

        binding.btnSyncNow.setOnClickListener {
            if (viewModel.syncStatus.value == SyncStatus.ERROR) {
                viewModel.retryPermanentFailures()
                Toast.makeText(requireContext(), "Retrying failed sync items...", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.triggerSyncNow()
                Toast.makeText(requireContext(), "Cloud sync triggered...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cardPrivacyOptions.setOnClickListener {
            val app = requireActivity().application as AptiRiseApplication
            app.appContainer.adProvider.showPrivacyOptionsForm(requireActivity()) {
                Toast.makeText(requireContext(), "Privacy choices updated", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnDeleteAccount.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_accountDeletion)
        }

        binding.btnSignOut.setOnClickListener {
            viewModel.signOut()
            findNavController().navigate(R.id.action_profile_to_welcome)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val app = requireActivity().application as AptiRiseApplication
                    app.appContainer.themeRepository.themeMode.collectLatest { mode ->
                        when (mode) {
                            ThemeMode.LIGHT -> binding.rbThemeLight.isChecked = true
                            ThemeMode.DARK -> binding.rbThemeDark.isChecked = true
                            ThemeMode.SYSTEM -> binding.rbThemeSystem.isChecked = true
                        }
                    }
                }

                launch {
                    viewModel.authState.collectLatest { authState ->
                        when (authState) {
                            is AuthState.SignedInVerified -> {
                                binding.cardAuthProfile.visibility = View.VISIBLE
                                binding.cardCloudSync.visibility = View.VISIBLE
                                binding.btnSignOut.visibility = View.VISIBLE
                                binding.tvAuthEmail.text = authState.email
                                binding.tvVerifiedBadge.text = "VERIFIED"
                                viewModel.loadProfile()
                            }
                            is AuthState.SignedInUnverified -> {
                                binding.cardAuthProfile.visibility = View.VISIBLE
                                binding.cardCloudSync.visibility = View.VISIBLE
                                binding.btnSignOut.visibility = View.VISIBLE
                                binding.tvAuthEmail.text = authState.email
                                binding.tvVerifiedBadge.text = "UNVERIFIED"
                            }
                            else -> {
                                binding.cardAuthProfile.visibility = View.GONE
                                binding.cardCloudSync.visibility = View.GONE
                                binding.btnSignOut.visibility = View.GONE
                            }
                        }
                    }
                }

                launch {
                    viewModel.profileState.collectLatest { profileState ->
                        if (profileState is ProfileState.Success) {
                            binding.tvDisplayName.text = profileState.profile.displayName
                            binding.tvDailyGoalSummary.text = "Daily Target: ${profileState.profile.dailyGoal} Questions / day"
                        }
                    }
                }

                launch {
                    viewModel.isBackupEnabled.collectLatest { enabled ->
                        binding.switchCloudBackup.isChecked = enabled
                        binding.btnSyncNow.isEnabled = enabled
                    }
                }

                launch {
                    viewModel.syncStatus.collectLatest { status ->
                        val pending = viewModel.pendingOutboxCount.value
                        binding.tvSyncStatusSummary.text = "Status: ${status.getDisplayName()} (${pending} pending)"
                        if (status == SyncStatus.ERROR) {
                            binding.btnSyncNow.text = "Retry Failed Sync"
                        } else {
                            binding.btnSyncNow.text = "Sync Now"
                        }
                    }
                }

                launch {
                    viewModel.isPrivacyOptionsRequired.collectLatest { required ->
                        binding.cardPrivacyOptions.visibility = if (required) View.VISIBLE else View.GONE
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