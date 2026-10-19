package com.aptirise.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aptirise.app.AptiRiseApplication
import com.aptirise.app.R
import com.aptirise.app.databinding.FragmentHomeBinding
import com.aptirise.app.ui.common.navigateSafely
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels {
        val c = (requireActivity().application as AptiRiseApplication).appContainer
        HomeViewModel.Factory(c.questionRepository,c.progressRepository,c.practiceRepository,c.learningOwner)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding=FragmentHomeBinding.inflate(inflater,container,false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnDailyChallenge.setOnClickListener { navigateSafely(R.id.action_home_to_topicDetail,Bundle().apply { putString("topicId","percentages") }) }
        binding.btnMockCatalog.setOnClickListener { navigateSafely(R.id.action_home_to_testCatalog) }
        listOf(binding.cardCategoryQuant to "quant",binding.cardCategoryLogical to "logical",binding.cardCategoryVerbal to "verbal").forEach { (card,id) ->
            card.setOnClickListener { navigateSafely(R.id.action_home_to_practice,Bundle().apply { putString("categoryId",id) }) }
        }
        val c=(requireActivity().application as AptiRiseApplication).appContainer
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { c.questionRepository.getTopics().collectLatest { topics ->
                    listOf("quant" to binding.tvQuantCatalogCount,"logical" to binding.tvLogicalCatalogCount,"verbal" to binding.tvVerbalCatalogCount).forEach { (id,label) ->
                        val group=topics.filter { it.categoryId==id }
                        label.text=getString(R.string.catalog_summary,group.size,group.sumOf { it.sampleQuestionCount })
                    }
                } }
                launch { viewModel.demoProgress.collectLatest { p ->
                    binding.tvHomeProgress.text=if (p==null || p.totalQuestionsAnswered==0) getString(R.string.no_progress_yet) else getString(R.string.home_progress_summary,p.totalQuestionsAnswered,p.accuracyPercentage,p.streakDays)
                } }
                launch { viewModel.inProgressSession.collectLatest { session ->
                    binding.cardResumePractice.visibility=if(session==null) View.GONE else View.VISIBLE
                    if(session!=null) {
                        binding.tvResumeTopicTitle.text=session.topicTitle
                        binding.tvResumeProgressText.text=getString(R.string.resume_position,session.currentQuestionIndex+1,session.requestedQuestionCount)
                        binding.btnResumeSession.setOnClickListener { navigateSafely(R.id.action_home_to_practiceSession,Bundle().apply {putString("sessionId",session.id)}) }
                    }
                } }
            }
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _binding=null }
}
