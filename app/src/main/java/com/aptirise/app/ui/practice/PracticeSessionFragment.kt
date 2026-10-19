package com.aptirise.app.ui.practice

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
import com.aptirise.app.data.model.FreeHintStatus
import com.aptirise.app.data.model.SessionSnapshot
import com.aptirise.app.databinding.FragmentPracticeSessionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PracticeSessionFragment : Fragment() {

    private var _binding: FragmentPracticeSessionBinding? = null
    private val binding get() = _binding!!

    private val sessionId: String by lazy {
        arguments?.getString("sessionId") ?: ""
    }

    internal val viewModel: PracticeSessionViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        PracticeSessionViewModel.Factory(
            sessionId,
            app.appContainer.practiceRepository,
            app.appContainer.learningOwner,
            app.appContainer.freeHintRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childFragmentManager.setFragmentResultListener("QuestionGridResult", viewLifecycleOwner) { _, result ->
            if (result.getBoolean("finish")) handleFinishRequest()
            else viewModel.navigateToPosition(result.getInt("position", -1))
        }

        observeActionError(viewModel.actionError)
        setupBackHandling()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupBackHandling() {
        binding.btnExitSession.setOnClickListener {
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
            .setTitle("Exit Practice Session?")
            .setMessage("What would you like to do with your current practice progress?")
            .setPositiveButton("Save & Exit") { _, _ ->
                viewModel.saveAndExit { popBackStackSafely(R.id.homeFragment, false) }
            }
            .setNegativeButton("Discard Session") { _, _ ->
                viewModel.discardSession {
                    popBackStackSafely(R.id.homeFragment, false)
                }
            }
            .setNeutralButton("Continue", null)
            .showOnce(this)
    }

    private fun setupClickListeners() {
        binding.btnBookmarkToggle.setOnClickListener {
            viewModel.toggleCurrentBookmark()
        }

        binding.btnToggleHint.setOnClickListener {
            val status = viewModel.freeHintStatus.value
            if (status is FreeHintStatus.Unavailable) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hint unavailable")
                    .setMessage(status.reason)
                    .setPositiveButton("OK", null)
                    .showOnce(this)
            } else {
                viewModel.handleFreeHintClick()
            }
        }

        binding.cardOptionA.setOnClickListener { viewModel.selectOption("a") }
        binding.cardOptionB.setOnClickListener { viewModel.selectOption("b") }
        binding.cardOptionC.setOnClickListener { viewModel.selectOption("c") }
        binding.cardOptionD.setOnClickListener { viewModel.selectOption("d") }

        binding.btnSubmitAnswer.setOnClickListener {
            viewModel.submitCurrentAnswer()
        }

        binding.btnPrevQuestion.setOnClickListener {
            val currentPos = viewModel.currentPosition.value
            if (currentPos > 0) {
                viewModel.navigateToPosition(currentPos - 1)
            }
        }

        binding.btnNextQuestion.setOnClickListener {
            val snapshots = viewModel.snapshots.value
            val currentPos = viewModel.currentPosition.value
            if (currentPos < snapshots.size - 1) {
                viewModel.navigateToPosition(currentPos + 1)
            } else {
                handleFinishRequest()
            }
        }

        binding.btnOpenNavigator.setOnClickListener {
            val snapshots = viewModel.snapshots.value
            val currentPos = viewModel.currentPosition.value
            if (snapshots.isEmpty() || childFragmentManager.isStateSaved ||
                childFragmentManager.findFragmentByTag("QuestionGrid") != null) return@setOnClickListener
            QuestionGridDialogFragment().showNow(childFragmentManager, "QuestionGrid")
        }
    }

    private fun handleFinishRequest() {
        if (viewModel.isFinishing.value || viewModel.snapshots.value.isEmpty()) return
        val snapshots = viewModel.snapshots.value
        val unanswered = snapshots.count { !it.isSubmitted || it.selectedOptionId == null }

        if (unanswered > 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Finish Practice Session?")
                .setMessage("You have $unanswered unanswered or unsubmitted questions. Unanswered questions will be counted as skipped.\n\nDo you want to submit and complete the session now?")
                .setPositiveButton("Finish & View Results") { _, _ ->
                    viewModel.finalizeSession { finalizedSession ->
                        val bundle = Bundle().apply {
                            putString("sessionId", finalizedSession.id)
                        }
                        navigateSafely(R.id.action_practiceSession_to_practiceResult, bundle)
                    }
                }
                .setNegativeButton("Keep Practicing", null)
                .showOnce(this)
        } else {
            viewModel.finalizeSession { finalizedSession ->
                val bundle = Bundle().apply {
                    putString("sessionId", finalizedSession.id)
                }
                navigateSafely(R.id.action_practiceSession_to_practiceResult, bundle)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.session.collectLatest { session ->
                        session?.let {
                            if (it.status == "COMPLETED") {
                                navigateSafely(R.id.action_practiceSession_to_practiceResult,
                                    Bundle().apply { putString("sessionId", it.id) })
                                return@collectLatest
                            }
                            binding.tvSessionTopicTitle.text = it.topicTitle
                            binding.tvSessionDifficulty.text = "${it.difficultyFilter.uppercase()} DIFFICULTY"
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

                launch {
                    combine(
                        viewModel.freeHintStatus,
                        viewModel.isClaimingFreeHint,
                        viewModel.currentPosition
                    ) { status, isClaiming, pos ->
                        Triple(status, isClaiming, pos)
                    }.collectLatest { (status, isClaiming, pos) ->
                        val snapshots = viewModel.snapshots.value
                        if (pos in snapshots.indices) {
                            renderFreeHintUI(status, isClaiming, snapshots[pos])
                        }
                    }
                }
            }
        }
    }

    private fun displaySnapshot(snapshot: SessionSnapshot, totalQuestions: Int, position: Int) {
        val context = requireContext()

        binding.tvQuestionCounter.visibility = View.GONE
        binding.sessionProgressBar.progress = (((position + 1).toFloat() / totalQuestions.toFloat()) * 100).toInt()

        binding.tvQuestionText.text = QuestionText.displayText(snapshot.questionText)

        renderFreeHintUI(viewModel.freeHintStatus.value, viewModel.isClaimingFreeHint.value, snapshot)
        renderExtraHint(snapshot, position)

        val starTint = if (snapshot.isBookmarked) R.color.primary else R.color.slate_400
        binding.btnBookmarkToggle.setColorFilter(ContextCompat.getColor(context, starTint))

        val opts = snapshot.options
        binding.tvOptionAText.text = opts.find { it.id == "a" }?.text ?: ""
        binding.tvOptionBText.text = opts.find { it.id == "b" }?.text ?: ""
        binding.tvOptionCText.text = opts.find { it.id == "c" }?.text ?: ""
        binding.tvOptionDText.text = opts.find { it.id == "d" }?.text ?: ""

        val isSubmitted = snapshot.isSubmitted
        val selected = snapshot.selectedOptionId

        if (isSubmitted) {
            binding.tvSubmittedStatus.visibility = View.VISIBLE
            binding.tvSubmittedStatus.text = if (snapshot.isCorrect()) "CORRECT ANSWER" else "INCORRECT ANSWER"
            binding.tvSubmittedStatus.setTextColor(
                ContextCompat.getColor(context, if (snapshot.isCorrect()) R.color.difficulty_easy else R.color.difficulty_hard)
            )

            binding.btnSubmitAnswer.isEnabled = false
            binding.cardExplanation.visibility = View.VISIBLE
            binding.tvExplanationText.text = snapshot.explanation

            highlightSubmittedOption(binding.cardOptionA, binding.tvBadgeA, binding.tvOptionAText, "a", snapshot.correctOptionId, selected)
            highlightSubmittedOption(binding.cardOptionB, binding.tvBadgeB, binding.tvOptionBText, "b", snapshot.correctOptionId, selected)
            highlightSubmittedOption(binding.cardOptionC, binding.tvBadgeC, binding.tvOptionCText, "c", snapshot.correctOptionId, selected)
            highlightSubmittedOption(binding.cardOptionD, binding.tvBadgeD, binding.tvOptionDText, "d", snapshot.correctOptionId, selected)
        } else {
            binding.tvSubmittedStatus.visibility = View.GONE
            binding.cardExplanation.visibility = View.GONE
            binding.btnSubmitAnswer.isEnabled = selected != null

            highlightUnsubmittedOption(binding.cardOptionA, binding.tvBadgeA, binding.tvOptionAText, "a", selected)
            highlightUnsubmittedOption(binding.cardOptionB, binding.tvBadgeB, binding.tvOptionBText, "b", selected)
            highlightUnsubmittedOption(binding.cardOptionC, binding.tvBadgeC, binding.tvOptionCText, "c", selected)
            highlightUnsubmittedOption(binding.cardOptionD, binding.tvBadgeD, binding.tvOptionDText, "d", selected)
        }

        binding.btnNextQuestion.text = if (position == totalQuestions - 1) "Finish" else "Next"
        binding.btnPrevQuestion.isEnabled = position > 0
    }

    private fun renderFreeHintUI(status: FreeHintStatus, isClaiming: Boolean, snapshot: SessionSnapshot) {
        val activeSnap = viewModel.snapshots.value.getOrNull(viewModel.currentPosition.value)
        if (activeSnap == null || activeSnap.questionId != snapshot.questionId) {
            binding.tvHintText.text = ""
            binding.layoutHintContainer.visibility = View.GONE
            return
        }

        if (isClaiming) {
            binding.btnToggleHint.text = "Checking Free Hint..."
            binding.btnToggleHint.isEnabled = false
            binding.tvFreeHintStatus.visibility = View.GONE
            binding.tvHintText.text = ""
            binding.layoutHintContainer.visibility = View.GONE
            return
        }

        val sessionOwnerId = viewModel.session.value?.ownerId
        val currentUid = viewModel.currentOwnerId

        when (status) {
            is FreeHintStatus.Loading -> {
                binding.btnToggleHint.text = "Checking Free Hint..."
                binding.btnToggleHint.isEnabled = false
                binding.tvFreeHintStatus.visibility = View.GONE
                binding.tvHintText.text = ""
                binding.layoutHintContainer.visibility = View.GONE
            }
            is FreeHintStatus.Available -> {
                binding.btnToggleHint.text = "Use Free Hint (1 total)"
                binding.btnToggleHint.isEnabled = true
                binding.tvFreeHintStatus.text = "You have 1 lifetime free hint on this device."
                binding.tvFreeHintStatus.visibility = View.VISIBLE
                binding.tvHintText.text = ""
                binding.layoutHintContainer.visibility = View.GONE
            }
            is FreeHintStatus.UnlockedForCurrent -> {
                val isValidReveal = sessionOwnerId == currentUid &&
                        status.ownerId == currentUid &&
                        status.sessionId == viewModel.sessionId &&
                        status.questionId == snapshot.questionId &&
                        status.isVisible

                binding.btnToggleHint.text = if (status.isVisible) "Hide Hint" else "Show Hint"
                binding.btnToggleHint.isEnabled = true
                binding.tvFreeHintStatus.visibility = View.GONE

                if (isValidReveal) {
                    binding.tvHintText.text = snapshot.hint
                    binding.layoutHintContainer.visibility = View.VISIBLE
                } else {
                    binding.tvHintText.text = ""
                    binding.layoutHintContainer.visibility = View.GONE
                }
            }
            is FreeHintStatus.Exhausted -> {
                binding.btnToggleHint.text = "Free hint used"
                binding.btnToggleHint.isEnabled = false
                binding.tvFreeHintStatus.text = "You have used your one free hint across all tests."
                binding.tvFreeHintStatus.visibility = View.VISIBLE
                binding.tvHintText.text = ""
                binding.layoutHintContainer.visibility = View.GONE
            }
            is FreeHintStatus.Unavailable -> {
                binding.btnToggleHint.text = "Use Free Hint (1 total)"
                binding.btnToggleHint.isEnabled = true
                binding.tvFreeHintStatus.text = status.reason
                binding.tvFreeHintStatus.visibility = View.VISIBLE
                binding.tvHintText.text = ""
                binding.layoutHintContainer.visibility = View.GONE
            }
            is FreeHintStatus.Error -> {
                binding.btnToggleHint.text = "Retry Free Hint Check"
                binding.btnToggleHint.isEnabled = true
                binding.tvFreeHintStatus.text = status.message
                binding.tvFreeHintStatus.visibility = View.VISIBLE
                binding.tvHintText.text = ""
                binding.layoutHintContainer.visibility = View.GONE
            }
        }
    }

    private fun renderExtraHint(snapshot: SessionSnapshot, position: Int) {
        val session = viewModel.session.value
        val ownerId = viewModel.currentOwnerId
        val current = viewModel.snapshots.value.getOrNull(viewModel.currentPosition.value)
        val canRead = session?.ownerId == ownerId && session.id == sessionId &&
            current?.questionId == snapshot.questionId && position == viewModel.currentPosition.value &&
            snapshot.isExtraHintUnlocked
        val text = if (canRead) snapshot.hint.ifBlank { snapshot.extraHint } else ""
        binding.tvExtraHintText.text = text
        binding.layoutExtraHintContainer.visibility = if (text.isNotBlank()) View.VISIBLE else View.GONE
    }

    private fun highlightUnsubmittedOption(
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

    private fun highlightSubmittedOption(
        card: com.google.android.material.card.MaterialCardView,
        badge: android.widget.TextView,
        optionTextView: android.widget.TextView,
        optionId: String,
        correctId: String,
        selectedId: String?
    ) {
        com.aptirise.app.ui.common.AtelierUi.option(
            card, badge, optionTextView, optionId, optionId == selectedId,
            submitted = true, correct = optionId == correctId
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
