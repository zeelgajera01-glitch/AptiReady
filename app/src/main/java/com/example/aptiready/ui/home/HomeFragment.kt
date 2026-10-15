package com.example.aptiready.ui.home

import com.example.aptiready.ui.common.navigateSafely
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

        val container = (requireActivity().application as AptiRiseApplication).appContainer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.authRepository.authState.collectLatest {
                    val name = if (container.authRepository.isFirebaseConfigured) {
                        try { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName }
                        catch (_: Exception) { null }
                    } else null
                    binding.tvHomeGreeting.text = name?.trim()?.takeIf { it.isNotBlank() }
                        ?.let { "Ready to rise, ${it.substringBefore(' ')}?" } ?: "Ready to rise?"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.questionRepository.getTopics().collectLatest { topics ->
                    fun summary(categoryId: String): String {
                        val entries = topics.filter { it.categoryId == categoryId }
                        return "${entries.size} topics · ${entries.sumOf { it.sampleQuestionCount }} questions"
                    }
                    binding.tvQuantCatalogCount.text = summary("quant")
                    binding.tvLogicalCatalogCount.text = summary("logical")
                    binding.tvVerbalCatalogCount.text = summary("verbal")
                }
            }
        }
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
            navigateSafely(R.id.action_home_to_topicDetail, bundle)
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

        binding.cardQuickQuiz.setOnClickListener {
            val bundle = Bundle().apply {
                putString("topicId", "percentages")
            }
            navigateSafely(R.id.action_home_to_topicDetail, bundle)
        }

        binding.btnQuickQuiz.setOnClickListener {
            val bundle = Bundle().apply {
                putString("topicId", "percentages")
            }
            navigateSafely(R.id.action_home_to_topicDetail, bundle)
        }

        binding.cardMockTests.setOnClickListener {
            navigateSafely(R.id.action_home_to_testCatalog)
        }

        binding.btnMockCatalog.setOnClickListener {
            navigateSafely(R.id.action_home_to_testCatalog)
        }

        binding.btnContinueTopic.setOnClickListener {
            val topicId = viewModel.recentTopic.value?.id ?: "percentages"
            val bundle = Bundle().apply {
                putString("topicId", topicId)
            }
            navigateSafely(R.id.action_home_to_topicDetail, bundle)
        }
    }

    private fun navigateToPracticeCategory(categoryId: String) {
        navigateSafely(R.id.action_home_to_practice, Bundle().apply {
            putString("categoryId", categoryId)
        })
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
                            binding.tvResumeProgressText.text = "Question $pos of $total"
                            binding.resumeProgress.progress = if (total > 0) ((pos.toFloat() / total) * 100).toInt() else 0

                            binding.btnResumeSession.setOnClickListener {
                                val bundle = Bundle().apply {
                                    putString("sessionId", session.id)
                                }
                                navigateSafely(R.id.action_home_to_practiceSession, bundle)
                            }
                        } else {
                            binding.cardResumePractice.visibility = View.GONE
                        }
                    }
                }



                launch {
                    viewModel.recentTopic.collectLatest { topic ->
                        if (topic != null) {
                            binding.layoutContinueLearning.visibility = View.VISIBLE
                            binding.tvRecentTopicTitle.text = topic.title
                            binding.tvRecentTopicSub.text = "${topic.categoryName} • ${topic.sampleQuestionCount} questions"
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
