package com.aptirise.app.ui.topic

import com.aptirise.app.ui.common.observeActionError
import com.aptirise.app.ui.common.showOnce
import com.aptirise.app.ui.common.navigateSafely
import com.aptirise.app.ui.common.navigateUpSafely
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
import com.aptirise.app.AptiRiseApplication
import com.aptirise.app.R
import com.aptirise.app.data.model.Difficulty
import com.aptirise.app.databinding.FragmentTopicDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TopicDetailFragment : Fragment() {

    private var _binding: FragmentTopicDetailBinding? = null
    private val binding get() = _binding!!

    private val topicId: String by lazy {
        arguments?.getString("topicId") ?: "percentages"
    }

    private val viewModel: TopicDetailViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        TopicDetailViewModel.Factory(
            topicId,
            app.appContainer.questionRepository,
            app.appContainer.practiceRepository,
            app.appContainer.learningOwner
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTopicDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeActionError(viewModel.creationError)

        binding.btnBack.setOnClickListener {
            navigateUpSafely()
        }

        setupChips()
        setupStartButton()
        observeViewModel()
    }

    private fun setupChips() {
        binding.chipGroupDifficulty.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val difficulty = when (checkedIds.first()) {
                R.id.chip_diff_easy -> Difficulty.EASY
                R.id.chip_diff_medium -> Difficulty.MEDIUM
                R.id.chip_diff_hard -> Difficulty.HARD
                else -> null
            }
            viewModel.setDifficulty(difficulty)
        }

        binding.chipGroupLength.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val length = when (checkedIds.first()) {
                R.id.chip_len_5 -> 5
                R.id.chip_len_15 -> 15
                R.id.chip_len_20 -> 20
                else -> 10
            }
            viewModel.setPracticeLength(length)
        }
    }

    private fun setupStartButton() {
        binding.btnStartPractice.setOnClickListener {
            val total = viewModel.availableCount.value
            val unseen = viewModel.unseenCount.value
            val requested = viewModel.selectedLength.value

            if (total <= 0) {
                Toast.makeText(requireContext(), "No questions available for selected difficulty filter.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (requested > total) {
                val filterName = viewModel.selectedDifficulty.value?.name ?: "All Difficulties"
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Question Pool Notice")
                    .setMessage("You requested $requested questions, but only $total matching question(s) exist in the bank for $filterName.\n\nWould you like to practice all $total available questions?")
                    .setPositiveButton("Practice $total Questions") { _, _ ->
                        launchPractice(total)
                    }
                    .setNegativeButton("Switch to All Difficulties") { _, _ ->
                        binding.chipDiffAll.isChecked = true
                        viewModel.setDifficulty(null)
                    }
                    .setNeutralButton("Cancel", null)
                    .showOnce(this)
            } else if (unseen < requested) {
                val filterName = viewModel.selectedDifficulty.value?.name ?: "All Difficulties"
                if (unseen > 0) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Revision Questions Included")
                        .setMessage("Only $unseen unseen question(s) remain for $filterName. Your session will include $unseen new question(s) and ${requested - unseen} revision question(s).")
                        .setPositiveButton("Start Session") { _, _ ->
                            launchPractice(requested)
                        }
                        .setNegativeButton("Cancel", null)
                        .showOnce(this)
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("All Questions Completed")
                        .setMessage("You have completed all $total question(s) in this filter! This session will select $requested questions for revision.")
                        .setPositiveButton("Start Revision") { _, _ ->
                            launchPractice(requested)
                        }
                        .setNegativeButton("Cancel", null)
                        .showOnce(this)
                }
            } else {
                launchPractice(requested)
            }
        }
    }

    private fun launchPractice(countToRequest: Int) {
        binding.btnStartPractice.isEnabled = false
        binding.btnStartPractice.text = "Preparing Session…"
        viewModel.startPracticeSession(countToRequest) { session ->
            val bundle = Bundle().apply {
                putString("sessionId", session.id)
            }
            navigateSafely(R.id.action_topicDetail_to_practiceSession, bundle)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.topic.collectLatest { topic ->
                        topic?.let {
                            binding.tvHeaderTitle.text = it.title
                            binding.tvTopicFullTitle.text = it.title
                            binding.tvCategoryName.text = it.categoryName.uppercase()
                            binding.tvTopicDescription.text = it.description
                            binding.tvFormulaContent.text = it.formulaPreview
                        }
                    }
                }

                launch {
                    combine(viewModel.availableCount, viewModel.unseenCount,
                        viewModel.selectedLength, viewModel.selectedDifficulty, viewModel.creationBusy
                    ) { total, unseen, requested, difficulty, busy ->
                        val label = when {
                            busy -> "Loading…"
                            total <= 0 -> "No Questions Available (${difficulty?.name ?: "ALL"})"
                            unseen >= requested -> "Start Practice ($requested Unseen Questions)"
                            unseen > 0 -> "Start Practice ($unseen Unseen, ${requested - unseen} Revision)"
                            else -> "Start Revision ($requested Questions)"
                        }
                        (total > 0 && !busy) to label
                    }.collectLatest { (enabled, label) ->
                        binding.btnStartPractice.isEnabled = enabled
                        binding.btnStartPractice.text = label
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
