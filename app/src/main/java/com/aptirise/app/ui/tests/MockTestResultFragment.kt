package com.aptirise.app.ui.tests

import com.aptirise.app.ui.common.observeActionError
import com.aptirise.app.ui.common.navigateSafely
import com.aptirise.app.ui.common.popBackStackSafely
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
import com.aptirise.app.AptiRiseApplication
import com.aptirise.app.R
import com.aptirise.app.databinding.FragmentMockTestResultBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class MockTestResultFragment : Fragment() {

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
            app.appContainer.learningOwner
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
        observeActionError(viewModel.creationError)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.creationBusy.collectLatest { busy ->
                binding.btnRetakeTest.isEnabled = !busy
                binding.btnRetakeTest.text = if (busy) "Loading…" else "Retake Test"
            }
        }

        binding.btnReviewMockAnswers.setOnClickListener {
            val bundle = Bundle().apply {
                putString("sessionId", attemptId)
            }
            navigateSafely(R.id.action_mockTestResult_to_practiceReview, bundle)
        }

        binding.btnRetakeTest.setOnClickListener {
            viewModel.retakeTest { newAttempt ->
                val bundle = Bundle().apply {
                    putString("attemptId", newAttempt.id)
                }
                navigateSafely(R.id.action_mockTestResult_to_mockTest, bundle)
            }
        }

        binding.btnReturnTestCatalog.setOnClickListener {
            popBackStackSafely(R.id.testCatalogFragment, false)
        }

        observeViewModel()
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

                        binding.mockAccuracyBar.progress = a.accuracyPercentage.toInt().coerceIn(0, 100)
                        binding.tvMockCorrectCount.text = a.correctCount().toString()
                        binding.tvMockIncorrectCount.text = a.incorrectCount().toString()
                        binding.tvMockUnansweredCount.text = a.unansweredCount().toString()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.allowNewCreation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
