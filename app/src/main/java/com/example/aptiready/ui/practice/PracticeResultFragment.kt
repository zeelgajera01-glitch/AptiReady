package com.example.aptiready.ui.practice

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
import com.example.aptiready.databinding.FragmentPracticeResultBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativeAdLoader
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import com.example.aptiready.ui.ads.ResultExitState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PracticeResultFragment : Fragment() {

    private val exitState: ResultExitState by viewModels()

    private var _binding: FragmentPracticeResultBinding? = null
    private val binding get() = _binding!!

    private val sessionId: String by lazy {
        arguments?.getString("sessionId") ?: ""
    }

    private val viewModel: PracticeResultViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        PracticeResultViewModel.Factory(
            sessionId,
            app.appContainer.practiceRepository,
            app.appContainer.authRepository
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnReviewAnswers.setOnClickListener {
            val bundle = Bundle().apply {
                putString("sessionId", sessionId)
            }
            findNavController().navigate(R.id.action_practiceResult_to_practiceReview, bundle)
        }

        binding.btnRetryIncorrect.setOnClickListener {
            viewModel.retryIncorrect { newSession ->
                val bundle = Bundle().apply {
                    putString("sessionId", newSession.id)
                }
                findNavController().navigate(R.id.action_practiceResult_to_practiceSession, bundle)
            }
        }

        val app = requireActivity().application as AptiRiseApplication
        val entry = findNavController().currentBackStackEntry
        binding.btnReturnHome.setOnClickListener {
            if (!exitState.begin()) return@setOnClickListener
            val record = viewModel.session.value
            if (record?.status != "COMPLETED" || record.ownerId !=
                (app.appContainer.authRepository.currentUserId ?: "guest")) {
                exitState.complete()
            } else {
                app.appContainer.adProvider.showExitInterstitialIfEligible(
                    requireActivity(), AdPlacement.PRACTICE_EXIT_INTERSTITIAL, sessionId, exitState::complete)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                exitState.state.collect { state ->
                    binding.btnReturnHome.isEnabled = state == "IDLE"
                    if (state == "READY" && findNavController().currentBackStackEntry === entry &&
                        !parentFragmentManager.isStateSaved) {
                        exitState.consumed()
                        findNavController().popBackStack(R.id.homeFragment, false)
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            combine(app.appContainer.consentManager.adState, viewModel.session,
                viewLifecycleOwner.lifecycle.currentStateFlow) { state, record, lifecycle ->
                state.ready && record?.status == "COMPLETED" && lifecycle.isAtLeast(Lifecycle.State.STARTED)
            }.collect { eligible ->
                if (eligible) app.appContainer.adProvider.prepareExitAd(AdPlacement.PRACTICE_EXIT_INTERSTITIAL)
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
            }, viewModel.session.map { if (it?.status == "COMPLETED") 1 else 0 })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.session.collectLatest { session ->
                    session?.let { s ->
                        binding.tvResultTopicSubtitle.text = "${s.topicTitle} • ${s.difficultyFilter.uppercase()} DIFFICULTY"
                        
                        val total = s.totalQuestions()
                        val correct = s.correctCount()
                        val incorrect = s.incorrectCount()
                        val unanswered = s.unansweredCount()
                        val submitted = s.submittedCount()

                        val scorePct = if (total > 0) ((correct.toFloat() / total.toFloat()) * 100).toInt() else 0
                        val accuracyPct = if (submitted > 0) ((correct.toFloat() / submitted.toFloat()) * 100).toInt() else 0

                        binding.tvScorePercentage.text = "$scorePct%"
                        binding.tvAccuracyPercentage.text = if (submitted > 0) "$accuracyPct%" else "—"

                        binding.tvCountCorrect.text = correct.toString()
                        binding.tvCountIncorrect.text = incorrect.toString()
                        binding.tvCountSkipped.text = unanswered.toString()

                        if (incorrect == 0) {
                            binding.btnRetryIncorrect.visibility = View.GONE
                        } else {
                            binding.btnRetryIncorrect.visibility = View.VISIBLE
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
