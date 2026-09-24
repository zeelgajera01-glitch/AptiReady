package com.example.aptiready.ui.home

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
import com.example.aptiready.databinding.FragmentHomeBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativeAdLoader
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        HomeViewModel.Factory(
            app.appContainer.questionRepository,
            app.appContainer.progressRepository,
            app.appContainer.practiceRepository,
            app.appContainer.authRepository
        )
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()
        setupNativeAd()
    }

    private fun setupNativeAd() {
        val app = requireActivity().application as AptiRiseApplication
        val adBinding = binding.homeNativeAd
        val renderer = NativeAdLoader(requireContext()) { false }
        NativePlacementBinding(requireContext(), viewLifecycleOwner,
            app.appContainer.consentManager, AdPlacement.HOME_NATIVE, { ad ->
                if (ad != null) renderer.populateNativeAdView(ad, adBinding)
                adBinding.root.visibility = if (ad == null) View.GONE else View.VISIBLE
            }, kotlinx.coroutines.flow.flowOf(1))
    }

    private fun setupClickListeners() {
        binding.btnDailyChallenge.setOnClickListener {
            val bundle = Bundle().apply {
                putString("topicId", "percentages")
            }
            findNavController().navigate(R.id.action_home_to_topicDetail, bundle)
        }

        binding.cardCategoryQuant.setOnClickListener {
            navigateToPracticeCategory("quant")
        }

        binding.cardCategoryLogical.setOnClickListener {
            navigateToPracticeCategory("logical")
        }

        binding.cardCategoryVerbal.setOnClickListener {
            navigateToPracticeCategory("verbal")
        }

        binding.btnQuickQuiz.setOnClickListener {
            val bundle = Bundle().apply {
                putString("topicId", "percentages")
            }
            findNavController().navigate(R.id.action_home_to_topicDetail, bundle)
        }

        binding.btnContinueTopic.setOnClickListener {
            val topicId = viewModel.recentTopic.value?.id ?: "percentages"
            val bundle = Bundle().apply {
                putString("topicId", topicId)
            }
            findNavController().navigate(R.id.action_home_to_topicDetail, bundle)
        }
    }

    private fun navigateToPracticeCategory(categoryId: String) {
        findNavController().navigate(R.id.action_home_to_practice)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.inProgressSession.collectLatest { session ->
                        if (session != null) {
                            binding.cardResumePractice.visibility = View.VISIBLE
                            binding.tvResumeTopicTitle.text = session.topicTitle
                            val pos = session.currentQuestionIndex + 1
                            val total = session.requestedQuestionCount
                            binding.tvResumeProgressText.text = "Q$pos of $total"

                            binding.btnResumeSession.setOnClickListener {
                                val bundle = Bundle().apply {
                                    putString("sessionId", session.id)
                                }
                                findNavController().navigate(R.id.action_home_to_practiceSession, bundle)
                            }
                        } else {
                            binding.cardResumePractice.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.demoProgress.collectLatest { progress ->
                        progress?.let {
                            binding.tvDemoAccuracy.text = "${it.accuracyPercentage}%"
                            binding.tvDemoStreak.text = "${it.streakDays} Days"
                            binding.tvDemoTopics.text = "${it.topicsCompleted}"
                        }
                    }
                }

                launch {
                    viewModel.recentTopic.collectLatest { topic ->
                        if (topic != null) {
                            binding.layoutContinueLearning.visibility = View.VISIBLE
                            binding.tvRecentTopicTitle.text = topic.title
                            binding.tvRecentTopicSub.text = "${topic.categoryName} • ${topic.sampleQuestionCount} sample questions"
                        } else {
                            binding.layoutContinueLearning.visibility = View.GONE
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
