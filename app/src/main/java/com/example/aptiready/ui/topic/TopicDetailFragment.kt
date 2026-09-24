package com.example.aptiready.ui.topic

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
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.databinding.FragmentTopicDetailBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.BannerAdWrapper
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
            app.appContainer.authRepository
        )
    }

    private var bannerAdWrapper: BannerAdWrapper? = null

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

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupChips()
        setupStartButton()
        observeViewModel()
        setupBannerAd()
    }

    private fun setupBannerAd() {
        val app = requireActivity().application as AptiRiseApplication
        val consent = app.appContainer.consentManager
        val container = binding.bannerAdContainer
        bannerAdWrapper = BannerAdWrapper(requireContext()) {
            consent.canLoad(AdPlacement.FORMULA_LIBRARY_BANNER)
        }
        var epoch = -1L
        viewLifecycleOwner.lifecycleScope.launch {
            combine(consent.adState, viewModel.topic, viewLifecycleOwner.lifecycle.currentStateFlow) {
                state, topic, lifecycle -> Triple(state, topic, lifecycle)
            }.collect { (state, topic, lifecycle) ->
                if (epoch != state.generation || topic?.formulaPreview.isNullOrBlank() ||
                    !consent.canLoad(AdPlacement.FORMULA_LIBRARY_BANNER)) {
                    bannerAdWrapper?.destroy()
                    epoch = state.generation
                }
                if (!topic?.formulaPreview.isNullOrBlank() && consent.canLoad(AdPlacement.FORMULA_LIBRARY_BANNER) &&
                    lifecycle.isAtLeast(Lifecycle.State.STARTED)) {
                    bannerAdWrapper?.loadAnchoredAdaptiveBanner(
                        AdPlacement.FORMULA_LIBRARY_BANNER, container) {}
                }
            }
        }
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
                    .show()
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
                        .show()
                } else {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("All Questions Completed")
                        .setMessage("You have completed all $total question(s) in this filter! This session will select $requested questions for revision.")
                        .setPositiveButton("Start Revision") { _, _ ->
                            launchPractice(requested)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } else {
                launchPractice(requested)
            }
        }
    }

    private fun launchPractice(countToRequest: Int) {
        viewModel.startPracticeSession(countToRequest) { session ->
            val bundle = Bundle().apply {
                putString("sessionId", session.id)
            }
            findNavController().navigate(R.id.action_topicDetail_to_practiceSession, bundle)
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
                    viewModel.availableCount.collectLatest { total ->
                        val unseen = viewModel.unseenCount.value
                        val requested = viewModel.selectedLength.value
                        val diffText = viewModel.selectedDifficulty.value?.name ?: "ALL"

                        if (total > 0) {
                            binding.btnStartPractice.isEnabled = true
                            if (unseen >= requested) {
                                binding.btnStartPractice.text = "Start Practice ($requested Unseen Questions)"
                            } else if (unseen > 0) {
                                binding.btnStartPractice.text = "Start Practice ($unseen Unseen, ${requested - unseen} Revision)"
                            } else {
                                binding.btnStartPractice.text = "Start Revision ($requested Questions)"
                            }
                        } else {
                            binding.btnStartPractice.text = "No Questions Available ($diffText)"
                            binding.btnStartPractice.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        bannerAdWrapper?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        bannerAdWrapper?.resume()
    }

    override fun onDestroyView() {
        bannerAdWrapper?.destroy()
        bannerAdWrapper = null
        super.onDestroyView()
        _binding = null
    }
}
