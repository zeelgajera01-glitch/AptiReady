package com.aptirise.app.ui.practice

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
import com.aptirise.app.databinding.FragmentPracticeResultBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class PracticeResultFragment : Fragment() {

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
            app.appContainer.learningOwner
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
        observeActionError(viewModel.creationError)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.creationBusy.collectLatest { busy ->
                binding.btnRetryIncorrect.isEnabled = !busy
                binding.btnRetryIncorrect.text = if (busy) "Loading…" else "Retry Incorrect"
            }
        }

        binding.btnReviewAnswers.setOnClickListener {
            val bundle = Bundle().apply {
                putString("sessionId", sessionId)
            }
            navigateSafely(R.id.action_practiceResult_to_practiceReview, bundle)
        }

        binding.btnRetryIncorrect.setOnClickListener {
            viewModel.retryIncorrect { newSession ->
                val bundle = Bundle().apply {
                    putString("sessionId", newSession.id)
                }
                navigateSafely(R.id.action_practiceResult_to_practiceSession, bundle)
            }
        }

        binding.btnReturnHome.setOnClickListener {
            popBackStackSafely(R.id.homeFragment, false)
        }

        observeViewModel()
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
                        binding.resultScoreRing.progress = scorePct
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

    override fun onStart() {
        super.onStart()
        viewModel.allowNewCreation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
