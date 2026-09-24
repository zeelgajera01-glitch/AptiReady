package com.example.aptiready.ui.practice

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
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.model.FreeHintStatus
import com.example.aptiready.data.model.SessionSnapshot
import com.example.aptiready.databinding.FragmentPracticeSessionBinding
import com.example.aptiready.ui.ads.RewardContext
import com.example.aptiready.ui.ads.RewardedState
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

    private val viewModel: PracticeSessionViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        PracticeSessionViewModel.Factory(
            sessionId,
            app.appContainer.practiceRepository,
            app.appContainer.authRepository,
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
                findNavController().popBackStack(R.id.homeFragment, false)
            }
            .setNegativeButton("Discard Session") { _, _ ->
                viewModel.discardSession {
                    findNavController().popBackStack(R.id.homeFragment, false)
                }
            }
            .setNeutralButton("Continue", null)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnBookmarkToggle.setOnClickListener {
            viewModel.toggleCurrentBookmark()
        }

        binding.btnToggleHint.setOnClickListener {
            val status = viewModel.freeHintStatus.value
            if (status is FreeHintStatus.Unverified) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Verified Account Required")
                    .setMessage(status.reason)
                    .setPositiveButton("OK", null)
                    .show()
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
            val dialog = QuestionGridDialogFragment(
                snapshots = snapshots,
                currentPosition = currentPos,
                onSelectPosition = { selectedPos ->
                    viewModel.navigateToPosition(selectedPos)
                },
                onFinishClick = {
                    handleFinishRequest()
                }
            )
            dialog.show(childFragmentManager, "QuestionGrid")
        }
    }

    private fun handleFinishRequest() {
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
                        findNavController().navigate(R.id.action_practiceSession_to_practiceResult, bundle)
                    }
                }
                .setNegativeButton("Keep Practicing", null)
                .show()
        } else {
            viewModel.finalizeSession { finalizedSession ->
                val bundle = Bundle().apply {
                    putString("sessionId", finalizedSession.id)
                }
                findNavController().navigate(R.id.action_practiceSession_to_practiceResult, bundle)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    val app = requireActivity().application as AptiRiseApplication

                    combine(
                        app.appContainer.consentManager.adState,
                        app.appContainer.adProvider.rewardedHintController.state,
                        app.appContainer.authRepository.authState
                    ) { _, _, _ -> Unit }.collectLatest {
                        val position = viewModel.currentPosition.value
                        val snapshot = viewModel.snapshots.value.getOrNull(position)

                        if (snapshot == null) {
                            binding.tvHintText.text = ""
                            binding.layoutHintContainer.visibility = View.GONE

                            binding.tvExtraHintText.text = ""
                            binding.layoutExtraHintContainer.visibility = View.GONE

                            binding.btnToggleHint.isEnabled = false
                            binding.btnUnlockExtraHint.visibility = View.GONE
                            binding.btnUnlockExtraHint.setOnClickListener(null)
                        } else {
                            renderFreeHintUI(
                                viewModel.freeHintStatus.value,
                                viewModel.isClaimingFreeHint.value,
                                snapshot
                            )
                            renderExtraHint(snapshot, position)
                        }
                    }
                }
                launch {
                    viewModel.freeHintStatus.collectLatest {
                        val position = viewModel.currentPosition.value
                        viewModel.snapshots.value.getOrNull(position)?.let { snapshot ->
                            renderExtraHint(snapshot, position)
                        }
                    }
                }
                launch {
                    viewModel.session.collectLatest { session ->
                        session?.let {
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

        binding.tvQuestionCounter.text = "Question ${position + 1} of $totalQuestions"
        binding.sessionProgressBar.progress = (((position + 1).toFloat() / totalQuestions.toFloat()) * 100).toInt()

        binding.tvQuestionText.text = snapshot.questionText

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
        val selected = viewModel.selectedOptionId.value ?: snapshot.selectedOptionId

        if (isSubmitted) {
            binding.tvSubmittedStatus.visibility = View.VISIBLE
            binding.tvSubmittedStatus.text = if (snapshot.isCorrect()) "CORRECT ANSWER" else "INCORRECT ANSWER"
            binding.tvSubmittedStatus.setTextColor(
                ContextCompat.getColor(context, if (snapshot.isCorrect()) R.color.difficulty_easy else R.color.difficulty_hard)
            )

            binding.btnSubmitAnswer.isEnabled = false
            binding.cardExplanation.visibility = View.VISIBLE
            binding.tvExplanationText.text = snapshot.explanation

            highlightSubmittedOption(binding.cardOptionA, binding.tvBadgeA, "a", snapshot.correctOptionId, selected)
            highlightSubmittedOption(binding.cardOptionB, binding.tvBadgeB, "b", snapshot.correctOptionId, selected)
            highlightSubmittedOption(binding.cardOptionC, binding.tvBadgeC, "c", snapshot.correctOptionId, selected)
            highlightSubmittedOption(binding.cardOptionD, binding.tvBadgeD, "d", snapshot.correctOptionId, selected)
        } else {
            binding.tvSubmittedStatus.visibility = View.GONE
            binding.cardExplanation.visibility = View.GONE
            binding.btnSubmitAnswer.isEnabled = selected != null

            highlightUnsubmittedOption(binding.cardOptionA, binding.tvBadgeA, "a", selected)
            highlightUnsubmittedOption(binding.cardOptionB, binding.tvBadgeB, "b", selected)
            highlightUnsubmittedOption(binding.cardOptionC, binding.tvBadgeC, "c", selected)
            highlightUnsubmittedOption(binding.cardOptionD, binding.tvBadgeD, "d", selected)
        }

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
                binding.tvFreeHintStatus.text = "You have 1 lifetime free hint for your account."
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
            is FreeHintStatus.Unverified -> {
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
        val app = requireActivity().application as AptiRiseApplication
        val container = app.appContainer
        val provider = container.adProvider
        val controller = provider.rewardedHintController
        val session = viewModel.session.value
        val ownerId = viewModel.currentOwnerId

        binding.btnUnlockExtraHint.setOnClickListener(null)
        binding.btnUnlockExtraHint.visibility = View.GONE
        binding.layoutExtraHintContainer.visibility = View.GONE
        binding.tvExtraHintText.text = ""

        val activeSnapshot = viewModel.snapshots.value
            .getOrNull(viewModel.currentPosition.value)

        if (
            ownerId.isBlank() ||
            ownerId == "guest" ||
            container.authRepository.currentUserId != ownerId ||
            session?.ownerId != ownerId ||
            session.id != sessionId ||
            snapshot.sessionId != sessionId ||
            activeSnapshot?.questionId != snapshot.questionId ||
            viewModel.currentPosition.value != position
        ) {
            return
        }

        // Prefer the normal hint. Existing extra-hint-only questions also work.
        val rewardText = snapshot.hint.takeIf { it.isNotBlank() }
            ?: snapshot.extraHint.takeIf { it.isNotBlank() }
            ?: return

        // Previously earned hints remain available without watching another ad.
        if (snapshot.isExtraHintUnlocked) {
            binding.tvExtraHintText.text = rewardText
            binding.layoutExtraHintContainer.visibility = View.VISIBLE
            return
        }

        if (
            session.status != "IN_PROGRESS" ||
            snapshot.isSubmitted ||
            viewModel.freeHintStatus.value !is FreeHintStatus.Exhausted
        ) {
            return
        }

        binding.btnUnlockExtraHint.visibility = View.VISIBLE

        if (!container.consentManager.canLoad(AdPlacement.OPTIONAL_HINT_REWARDED)) {
            binding.btnUnlockExtraHint.text = "Rewarded hint unavailable"
            binding.btnUnlockExtraHint.isEnabled = false
            return
        }

        val ready = controller.isAdLoadedAndReady()
        val state = controller.state.value

        binding.btnUnlockExtraHint.text = when {
            state == RewardedState.SHOWING -> "Ad in progress…"
            state == RewardedState.LOADING -> "Loading rewarded ad…"
            ready -> "Watch Ad to Unlock Hint"
            state == RewardedState.UNAVAILABLE -> "Retry loading hint ad"
            else -> "Load Hint Ad"
        }

        binding.btnUnlockExtraHint.isEnabled =
            state != RewardedState.LOADING &&
            state != RewardedState.SHOWING

        binding.btnUnlockExtraHint.setOnClickListener {
            val current = viewModel.snapshots.value
                .getOrNull(viewModel.currentPosition.value)

            // Recheck identity and question at click time.
            if (
                container.authRepository.currentUserId != ownerId ||
                viewModel.currentOwnerId != ownerId ||
                viewModel.session.value?.ownerId != ownerId ||
                viewModel.session.value?.status != "IN_PROGRESS" ||
                viewModel.currentPosition.value != position ||
                current?.questionId != snapshot.questionId ||
                current.isSubmitted ||
                current.isExtraHintUnlocked ||
                viewModel.freeHintStatus.value !is FreeHintStatus.Exhausted
            ) {
                return@setOnClickListener
            }

            if (!container.consentManager.canLoad(AdPlacement.OPTIONAL_HINT_REWARDED)) {
                return@setOnClickListener
            }

            if (!controller.isAdLoadedAndReady()) {
                provider.prepareRewardedHint()

                // Loading never automatically opens an ad.
                // The user taps again once it is ready.
                renderExtraHint(current, position)
                return@setOnClickListener
            }

            val rewardContext = RewardContext(
                ownerId = ownerId,
                sessionId = sessionId,
                questionId = current.questionId,
                position = position
            )

            // Capture application-owned delivery, not the Fragment binding.
            val delivery = container.rewardDelivery

            provider.showRewardedHintAd(
                requireActivity(),
                rewardContext,
                current.questionText,
                delivery::deliver
            )
        }
    }

    private fun highlightUnsubmittedOption(
        card: com.google.android.material.card.MaterialCardView,
        badge: android.widget.TextView,
        optionId: String,
        selectedId: String?
    ) {
        val context = requireContext()
        val isSelected = optionId == selectedId

        card.isSelected = isSelected
        badge.background = null

        if (isSelected) {
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.indigo_50))
            card.strokeColor = ContextCompat.getColor(context, R.color.primary)
            card.strokeWidth = 4
            badge.setTextColor(ContextCompat.getColor(context, R.color.primary))
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_light))
            card.strokeColor = ContextCompat.getColor(context, R.color.card_stroke_light)
            card.strokeWidth = 1
            badge.setTextColor(com.google.android.material.color.MaterialColors.getColor(badge, com.google.android.material.R.attr.colorOnSurface))
        }
    }

    private fun highlightSubmittedOption(
        card: com.google.android.material.card.MaterialCardView,
        badge: android.widget.TextView,
        optionId: String,
        correctId: String,
        selectedId: String?
    ) {
        val context = requireContext()
        val isCorrect = optionId == correctId
        val isUserSelection = optionId == selectedId

        card.isSelected = isUserSelection || isCorrect
        badge.background = null

        when {
            isCorrect -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.teal_100))
                card.strokeColor = ContextCompat.getColor(context, R.color.teal_700)
                card.strokeWidth = 4
                badge.setTextColor(ContextCompat.getColor(context, R.color.teal_800))
            }
            isUserSelection && !isCorrect -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.indigo_50))
                card.strokeColor = ContextCompat.getColor(context, R.color.difficulty_hard)
                card.strokeWidth = 4
                badge.setTextColor(ContextCompat.getColor(context, R.color.difficulty_hard))
            }
            else -> {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surface_light))
                card.strokeColor = ContextCompat.getColor(context, R.color.card_stroke_light)
                card.strokeWidth = 1
                badge.setTextColor(com.google.android.material.color.MaterialColors.getColor(badge, com.google.android.material.R.attr.colorOnSurface))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
