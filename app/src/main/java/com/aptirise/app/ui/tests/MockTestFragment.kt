package com.aptirise.app.ui.tests

import com.aptirise.app.ui.common.observeActionError
import com.aptirise.app.ui.common.showOnce
import com.aptirise.app.ui.common.navigateSafely
import com.aptirise.app.ui.common.popBackStackSafely
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
import com.aptirise.app.AptiRiseApplication
import com.aptirise.app.R
import com.aptirise.app.data.model.QuestionText
import com.aptirise.app.data.model.MockQuestionSnapshot
import com.aptirise.app.databinding.FragmentMockTestBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale

class MockTestFragment : Fragment() {

    private var _binding: FragmentMockTestBinding? = null
    private var submitSheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null
    private var submitSheetBinding: com.aptirise.app.databinding.SheetMockSubmitBinding? = null
    private var submissionPending = false
    private var resultNavigated = false
    private val binding get() = _binding!!

    private val attemptId: String by lazy {
        arguments?.getString("attemptId") ?: ""
    }

    internal val viewModel: MockTestViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        MockTestViewModel.Factory(
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
        _binding = FragmentMockTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.setFragmentResultListener("MockQuestionGridResult", viewLifecycleOwner) { _, result ->
            if (result.getBoolean("finish")) handleManualSubmitRequest()
            else viewModel.navigateToPosition(result.getInt("position", -1))
        }

        observeActionError(viewModel.actionError)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isFinishing.collectLatest { busy ->
                if (!busy && viewModel.attempt.value?.status != "COMPLETED") submissionPending = false
            }
        }
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
                viewModel.saveAndLeave { popBackStackSafely(R.id.testCatalogFragment, false) }
            }
            .setNegativeButton("Submit Now") { _, _ ->
                handleManualSubmitRequest()
            }
            .setNeutralButton("Continue Test", null)
            .showOnce(this)
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
            if (snapshots.isEmpty() || childFragmentManager.isStateSaved ||
                childFragmentManager.findFragmentByTag("MockQuestionGrid") != null) return@setOnClickListener
            MockQuestionGridDialogFragment().showNow(childFragmentManager, "MockQuestionGrid")
        }
    }

    private fun handleManualSubmitRequest() {
        if (submissionPending || resultNavigated || submitSheet?.isShowing == true || viewModel.snapshots.value.isEmpty()) return
        val sheetBinding = com.aptirise.app.databinding.SheetMockSubmitBinding.inflate(layoutInflater)
        val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        submitSheet = sheet
        submitSheetBinding = sheetBinding
        sheet.setContentView(sheetBinding.root)
        sheet.setOnDismissListener {
            if (submitSheet === sheet) {
                submitSheet = null
                submitSheetBinding = null
            }
        }
        sheetBinding.btnKeepWorking.setOnClickListener { sheet.dismiss() }
        sheetBinding.btnConfirmSubmit.setOnClickListener {
            if (submissionPending || resultNavigated) return@setOnClickListener
            submissionPending = true
            sheetBinding.btnConfirmSubmit.isEnabled = false
            sheet.dismiss()
            viewModel.finalizeAttempt("MANUAL_SUBMISSION") { finalizedAttempt ->
                navigateToResultScreen(finalizedAttempt.id)
            }
        }
        updateSubmitSheet()
        sheet.show()
    }

    private fun updateSubmitSheet() {
        val sheet = submitSheetBinding ?: return
        val snapshots = viewModel.snapshots.value
        val answered = snapshots.count { it.selectedOptionId != null }
        val total = snapshots.size
        val seconds = viewModel.remainingSeconds.value.coerceAtLeast(0)
        sheet.tvSubmitTime.text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
        sheet.tvSubmitAnswered.text = answered.toString()
        sheet.tvSubmitUnanswered.text = (total - answered).toString()
        sheet.tvSubmitReview.text = snapshots.count { it.isMarkedForReview }.toString()
        sheet.tvSubmitProgress.text = "$answered of $total answered"
        sheet.submitProgress.progress = if (total > 0) answered * 100 / total else 0
    }

    private fun navigateToResultScreen(attemptId: String) {
        if (_binding == null || resultNavigated || findNavController().currentDestination?.id != R.id.mockTestFragment) return
        resultNavigated = true
        submitSheet?.dismiss()
        val bundle = Bundle().apply { putString("attemptId", attemptId) }
        navigateSafely(R.id.action_mockTest_to_mockTestResult, bundle)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.attempt.collectLatest { attempt ->
                        attempt?.let {
                            if (it.status == "COMPLETED") {
                                navigateToResultScreen(it.id)
                                return@collectLatest
                            }
                            binding.tvMockTestTitle.text = it.testTitle
                            binding.tvMockTestSubtitle.text = "+${it.marksPerCorrect} per correct answer · No negative marking"
                        }
                    }
                }

                launch {
                    viewModel.remainingSeconds.collectLatest { seconds ->
                        val mins = seconds / 60
                        val secs = seconds % 60
                        binding.tvMockTimer.text = String.format(Locale.US, "%02d:%02d", mins.coerceAtLeast(0), secs.coerceAtLeast(0))
                        binding.tvMockTimer.setTextColor(ContextCompat.getColor(requireContext(),
                            if (seconds in 1..60) R.color.atelier_error else R.color.atelier_text))
                        updateSubmitSheet()

                    }
                }

                launch {
                    viewModel.snapshots.collectLatest { snapshots ->
                        updateSubmitSheet()
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
        binding.tvMockQuestionCounter.visibility = View.GONE
        binding.mockProgressBar.progress = (((position + 1).toFloat() / totalQuestions.toFloat()) * 100).toInt()

        binding.tvMockQuestionText.text = QuestionText.displayText(snapshot.questionText)

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

        binding.btnNextMock.text = if (position == totalQuestions - 1) "Finish" else "Next"
        binding.btnPrevMock.isEnabled = position > 0
    }

    private fun highlightOptionCard(
        card: com.google.android.material.card.MaterialCardView,
        badge: android.widget.TextView,
        optionTextView: android.widget.TextView,
        optionId: String,
        selectedId: String?
    ) {
        com.aptirise.app.ui.common.AtelierUi.option(
            card, badge, optionTextView, optionId, optionId == selectedId
        )
    }

    override fun onDestroyView() {
        submitSheet?.dismiss()
        submitSheet = null
        submitSheetBinding = null
        super.onDestroyView()
        _binding = null
    }
}