package com.example.aptiready.ui.tests

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
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.databinding.FragmentMockTestResultBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativeAdLoader
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import com.example.aptiready.ui.ads.ResultExitState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MockTestResultFragment : Fragment() {

    private val exitState: ResultExitState by viewModels()

    private var _binding: FragmentMockTestResultBinding? = null
    private val binding get() = _binding!!

    private val attemptId: String by lazy {
        arguments?.getString("attemptId") ?: ""
    }

    private val viewModel: MockTestResultViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        MockTestResultViewModel.Factory(
            attemptId,
            app.appContainer.mockTestRepository,
            app.appContainer.authRepository
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMockTestResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnReviewMockAnswers.setOnClickListener {
            val bundle = Bundle().apply {
                putString("sessionId", attemptId)
            }
            findNavController().navigate(R.id.action_mockTestResult_to_practiceReview, bundle)
        }

        binding.btnRetakeTest.setOnClickListener {
            viewModel.retakeTest { newAttempt ->
                val bundle = Bundle().apply {
                    putString("attemptId", newAttempt.id)
                }
                findNavController().navigate(R.id.action_mockTestResult_to_mockTest, bundle)
            }
        }

        val app = requireActivity().application as AptiRiseApplication
        val entry = findNavController().currentBackStackEntry
        binding.btnReturnTestCatalog.setOnClickListener {
            if (!exitState.begin()) return@setOnClickListener
            val record = viewModel.attempt.value
            if (record?.status != "COMPLETED" || record.ownerId !=
                (app.appContainer.authRepository.currentUserId ?: "guest")) {
                exitState.complete()
            } else {
                app.appContainer.adProvider.showExitInterstitialIfEligible(
                    requireActivity(), AdPlacement.MOCK_EXIT_INTERSTITIAL, attemptId, exitState::complete)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                exitState.state.collect { state ->
                    binding.btnReturnTestCatalog.isEnabled = state == "IDLE"
                    if (state == "READY" && findNavController().currentBackStackEntry === entry &&
                        !parentFragmentManager.isStateSaved) {
                        exitState.consumed()
                        findNavController().popBackStack(R.id.testCatalogFragment, false)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(app.appContainer.consentManager.adState, viewModel.attempt,
                viewLifecycleOwner.lifecycle.currentStateFlow) { state, record, lifecycle ->
                state.ready && record?.status == "COMPLETED" && lifecycle.isAtLeast(Lifecycle.State.STARTED)
            }.collect { eligible ->
                if (eligible) app.appContainer.adProvider.prepareExitAd(AdPlacement.MOCK_EXIT_INTERSTITIAL)
            }
        }

        observeViewModel()
        setupNativeAd()
    }

    private fun setupNativeAd() {
        val app = requireActivity().application as AptiRiseApplication
        val adBinding = binding.resultNativeAd
        val renderer = NativeAdLoader(requireContext()) { false }
        NativePlacementBinding(requireContext(), viewLifecycleOwner,
            app.appContainer.consentManager, AdPlacement.RESULT_SUMMARY_NATIVE, { ad ->
                if (ad != null) renderer.populateNativeAdView(ad, adBinding)
                adBinding.root.visibility = if (ad == null) View.GONE else View.VISIBLE
            }, viewModel.attempt.map { if (it?.status == "COMPLETED") 1 else 0 })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.attempt.collectLatest { attempt ->
                    attempt?.let { a ->
                        val reasonText = when (a.finishReason) {
                            "TIME_EXPIRED" -> "Time Expired"
                            "MANUAL_SUBMISSION" -> "Submitted Manually"
                            "SYSTEM" -> "Completed by System"
                            else -> "Completion Reason Unknown"
                        }

                        val elapsedText = if (
                            a.finishReason == "Cloud Restore" ||
                            a.elapsedSeconds < 0
                        ) {
                            "Time spent: Unknown"
                        } else {
                            val minutes = a.elapsedSeconds / 60
                            val seconds = a.elapsedSeconds % 60
                            "Time spent: ${minutes}m ${seconds}s"
                        }

                        binding.tvMockResultTitleSub.text =
                            "${a.testTitle} • $reasonText\n$elapsedText"

                        binding.tvEarnedMarksSummary.text = "${a.earnedMarks} / ${a.maxMarks}"
                        binding.tvMockAccuracy.text = if (a.answeredCount() > 0) "${a.accuracyPercentage.toInt()}%" else "—"

                        binding.tvMockCorrectCount.text = a.correctCount().toString()
                        binding.tvMockIncorrectCount.text = a.incorrectCount().toString()
                        binding.tvMockUnansweredCount.text = a.unansweredCount().toString()
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
