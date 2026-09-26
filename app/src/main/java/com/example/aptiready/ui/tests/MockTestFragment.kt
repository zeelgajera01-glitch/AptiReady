package com.example.aptiready.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.R
import com.example.aptiready.data.model.MockQuestionSnapshot
import com.example.aptiready.databinding.FragmentMockTestBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MockTestFragment : Fragment() {

    private var _binding: FragmentMockTestBinding? = null
    private val binding get() = _binding!!

    private val attemptId: String by lazy {
        arguments?.getString("attemptId") ?: ""
    }

    private val viewModel: MockTestViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        MockTestViewModel.Factory(
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
        _binding = FragmentMockTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBackHandling()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupBackHandling() {
        binding.btnExitMock.setOnClickListener {
            showExitConfirmationDialog()
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showExitConfirmationDialog()
                }
            }
        )
    }

    private fun showExitConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Leave Mock Test?")
            .setMessage("Your timer will continue running in the background. You can resume this test from the Tests tab or Home screen.")
            .setPositiveButton("Save & Leave") { _, _ ->
                findNavController().popBackStack(R.id.testCatalogFragment, false)
            }
            .setNegativeButton("Submit Now") { _, _ ->
                handleManualSubmitRequest()
            }
            .setNeutralButton("Continue Test", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnMockMarkReview.setOnClickListener {
            viewModel.toggleMarkForReview()
        }

        binding.cardMockOptionA.setOnClickListener { viewModel.selectOption("a") }
        binding.cardMockOptionB.setOnClickListener { viewModel.selectOption("b") }
        binding.cardMockOptionC.setOnClickListener { viewModel.selectOption("c") }
        binding.cardMockOptionD.setOnClickListener { viewModel.selectOption("d") }

        binding.btnClearSelection.setOnClickListener {
            viewModel.selectOption(null)
        }

        binding.btnPrevMock.setOnClickListener {
            val pos = viewModel.currentPosition.value
            if (pos > 0) {
                viewModel.navigateToPosition(pos - 1)
            }
        }

        binding.btnNextMock.setOnClickListener {
            val snapshots = viewModel.snapshots.value
            val pos = viewModel.currentPosition.value
            if (pos < snapshots.size - 1) {
                viewModel.navigateToPosition(pos + 1)
            } else {
                handleManualSubmitRequest()
            }
        }

        binding.btnSubmitMockTest.setOnClickListener {
            handleManualSubmitRequest()
        }

        binding.btnMockNavigator.setOnClickListener {
            val snapshots = viewModel.snapshots.value
            val pos = viewModel.currentPosition.value
            val dialog = MockQuestionGridDialogFragment(
                snapshots = snapshots,
                currentPosition = pos,
                onSelectPosition = { selectedPos ->
                    viewModel.navigateToPosition(selectedPos)
                },
                onSubmitClick = {
                    handleManualSubmitRequest()
                }
            )
            dialog.show(childFragmentManager, "MockQuestionGrid")
        }
    }

    private fun handleManualSubmitRequest() {
        val snapshots = viewModel.snapshots.value
        val answered = snapshots.count { it.selectedOptionId != null }
        val unanswered = snapshots.count { it.selectedOptionId == null }
        val marked = snapshots.count { it.isMarkedForReview }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Submit Mock Test?")
            .setMessage("Summary:\n• Answered: $answered\n• Unanswered: $unanswered\n• Marked for Review: $marked\n\nQuestions marked for review with an answer WILL be graded.\n\nAre you sure you want to submit your test now?")
            .setPositiveButton("Confirm Submission") { _, _ ->
                viewModel.finalizeAttempt("MANUAL_SUBMISSION") { finalizedAttempt ->
                    navigateToResultScreen(finalizedAttempt.id)
                }
            }
            .setNegativeButton("Keep Working", null)
            .show()
    }

    private fun navigateToResultScreen(attemptId: String) {
        val bundle = Bundle().apply {
            putString("attemptId", attemptId)
        }
        findNavController().navigate(R.id.action_mockTest_to_mockTestResult, bundle)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.attempt.collectLatest { attempt ->
                        attempt?.let {
                            binding.tvMockTestTitle.text = it.testTitle
                        }
                    }
                }

                launch {
                    viewModel.remainingSeconds.collectLatest { seconds ->
                        val mins = seconds / 60
                        val secs = seconds % 60
                        binding.tvMockTimer.text = String.format(Locale.US, "%02d:%02d", mins, secs)

                        if (seconds <= 0 && viewModel.attempt.value?.status == "IN_PROGRESS") {
                            viewModel.finalizeAttempt("TIME_EXPIRED") { finalized ->
                                navigateToResultScreen(finalized.id)
                            }
                        }
                    }
                }

                launch {
                    viewModel.isExpired.collectLatest { expired ->
                        if (expired) {
                            val id = viewModel.attempt.value?.id ?: attemptId
                            navigateToResultScreen(id)
                        }
                    }
                }

                launch {
                    viewModel.snapshots.collectLatest { snapshots ->
                        val pos = viewModel.currentPosition.value
                        if (pos in snapshots.indices) {
                            displaySnapshot(snapshots[pos], snapshots.size, pos)
                        }
                    }
                }

                launch {
                    viewModel.currentPosition.collectLatest { pos ->
                        val snapshots = viewModel.snapshots.value
                        if (pos in snapshots.indices) {
                            displaySnapshot(snapshots[pos], snapshots.size, pos)
                        }
                    }
                }
            }
        }
    }

    private fun displaySnapshot(snapshot: MockQuestionSnapshot, totalQuestions: Int, position: Int) {
        val context = requireContext()
        binding.tvMockQuestionCounter.text = "Question ${position + 1} of $totalQuestions"
        binding.mockProgressBar.progress = (((position + 1).toFloat() / totalQuestions.toFloat()) * 100).toInt()

        binding.tvMockQuestionText.text = snapshot.questionText

        if (snapshot.isMarkedForReview) {
            binding.tvMockReviewStatus.visibility = View.VISIBLE
            binding.btnMockMarkReview.setColorFilter(ContextCompat.getColor(context, R.color.primary))
        } else {
            binding.tvMockReviewStatus.visibility = View.GONE
            binding.btnMockMarkReview.setColorFilter(ContextCompat.getColor(context, R.color.slate_400))
        }

        val opts = snapshot.options
        binding.tvMockOptionAText.text = opts.find { it.id == "a" }?.text ?: ""
        binding.tvMockOptionBText.text = opts.find { it.id == "b" }?.text ?: ""
        binding.tvMockOptionCText.text = opts.find { it.id == "c" }?.text ?: ""
        binding.tvMockOptionDText.text = opts.find { it.id == "d" }?.text ?: ""

        val selected = snapshot.selectedOptionId
        highlightOptionCard(binding.cardMockOptionA, binding.tvMockBadgeA, binding.tvMockOptionAText, "a", selected)
        highlightOptionCard(binding.cardMockOptionB, binding.tvMockBadgeB, binding.tvMockOptionBText, "b", selected)
        highlightOptionCard(binding.cardMockOptionC, binding.tvMockBadgeC, binding.tvMockOptionCText, "c", selected)
        highlightOptionCard(binding.cardMockOptionD, binding.tvMockBadgeD, binding.tvMockOptionDText, "d", selected)

        binding.btnPrevMock.isEnabled = position > 0
    }

    private fun highlightOptionCard(
        card: com.google.android.material.card.MaterialCardView,
        badge: android.widget.TextView,
        optionTextView: android.widget.TextView,
        optionId: String,
        selectedId: String?
    ) {
        val isSelected = optionId == selectedId

        card.isSelected = isSelected
        badge.background = null

        if (isSelected) {
            val bg = com.google.android.material.color.MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimaryContainer)
            val stroke = com.google.android.material.color.MaterialColors.getColor(card, com.google.android.material.R.attr.colorPrimary)
            val textColor = com.google.android.material.color.MaterialColors.getColor(optionTextView, com.google.android.material.R.attr.colorOnPrimaryContainer)

            card.setCardBackgroundColor(bg)
            card.strokeColor = stroke
            card.strokeWidth = 4
            badge.setTextColor(textColor)
            optionTextView.setTextColor(textColor)
        } else {
            val bg = com.google.android.material.color.MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurface)
            val stroke = com.google.android.material.color.MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutline, ContextCompat.getColor(requireContext(), R.color.card_stroke_light))
            val textColor = com.google.android.material.color.MaterialColors.getColor(optionTextView, com.google.android.material.R.attr.colorOnSurface)

            card.setCardBackgroundColor(bg)
            card.strokeColor = stroke
            card.strokeWidth = 1
            badge.setTextColor(textColor)
            optionTextView.setTextColor(textColor)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}