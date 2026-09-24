package com.example.aptiready.ui.ads

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
import com.example.aptiready.databinding.FragmentAdDiagnosticsBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AdDiagnosticsFragment : Fragment() {

    private var _binding: FragmentAdDiagnosticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdDiagnosticsViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        AdDiagnosticsViewModel.Factory(
            app.appContainer.practiceRepository,
            app.appContainer.authRepository,
            app.appContainer.adSessionManager,
            app.appContainer.consentManager
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdDiagnosticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!BuildConfig.DEBUG) {
            Toast.makeText(requireContext(), "Diagnostics available in debug builds only.", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnGenerateHistoryFixtures.visibility = View.GONE
        binding.btnGenerateBookmarkFixtures.visibility = View.GONE

        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        if (BuildConfig.DEBUG && _binding != null) {
            binding.tvAdEnvSummary.text = viewModel.getDiagnosticSummary()
            binding.tvPlacementsChecklist.text = viewModel.getPlacementsChecklist()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.canRequestAds.collectLatest {
                        refreshStatus()
                    }
                }
                launch {
                    viewModel.isPrivacyOptionsRequired.collectLatest {
                        refreshStatus()
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
