package com.example.aptiready.ui.practice

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.databinding.FragmentPracticeBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PracticeFragment : Fragment() {

    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PracticeViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        PracticeViewModel.Factory(app.appContainer.questionRepository)
    }

    private lateinit var topicAdapter: TopicAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilter()
        observeViewModel()
        setupNativeAd()
    }

    private fun setupRecyclerView() {
        topicAdapter = TopicAdapter { topic ->
            val bundle = Bundle().apply {
                putString("topicId", topic.id)
            }
            findNavController().navigate(R.id.action_practice_to_topicDetail, bundle)
        }

        binding.rvTopics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topicAdapter
        }
    }

    private fun setupNativeAd() {
        val app = requireActivity().application as AptiRiseApplication
        NativePlacementBinding(requireContext(), viewLifecycleOwner,
            app.appContainer.consentManager, AdPlacement.TOPIC_LIST_NATIVE,
            { ad -> topicAdapter.setNativeAd(ad) }, viewModel.topics.map { it.size })
    }

    private fun setupSearchAndFilter() {
        binding.etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val categoryId = when (checkedIds.first()) {
                R.id.chip_quant -> "quant"
                R.id.chip_logical -> "logical"
                R.id.chip_verbal -> "verbal"
                else -> "all"
            }
            viewModel.setCategoryFilter(categoryId)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.topics.collectLatest { topicList ->
                    topicAdapter.submitTopicList(topicList)
                    if (topicList.isEmpty()) {
                        binding.rvTopics.visibility = View.GONE
                        binding.layoutEmptySearch.visibility = View.VISIBLE
                    } else {
                        binding.rvTopics.visibility = View.VISIBLE
                        binding.layoutEmptySearch.visibility = View.GONE
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
