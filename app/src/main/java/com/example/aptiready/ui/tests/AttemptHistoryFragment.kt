package com.example.aptiready.ui.tests

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.databinding.FragmentAttemptHistoryBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AttemptHistoryFragment : Fragment() {

    private var _binding: FragmentAttemptHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AttemptHistoryViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        AttemptHistoryViewModel.Factory(
            app.appContainer.mockTestRepository,
            app.appContainer.authRepository
        )
    }

    private lateinit var historyAdapter: AttemptHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttemptHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()
        setupFilterChips()
        observeViewModel()
        setupNativeAd()
    }

    private fun setupRecyclerView() {
        historyAdapter = AttemptHistoryAdapter { item ->
            val bundle = Bundle()
            if (item.type == "PRACTICE") {
                bundle.putString("sessionId", item.id)
                findNavController().navigate(R.id.action_attemptHistory_to_practiceResult, bundle)
            } else {
                bundle.putString("attemptId", item.id)
                findNavController().navigate(R.id.action_attemptHistory_to_mockTestResult, bundle)
            }
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupNativeAd() {
        val app = requireActivity().application as AptiRiseApplication
        NativePlacementBinding(requireContext(), viewLifecycleOwner,
            app.appContainer.consentManager, AdPlacement.HISTORY_NATIVE,
            { ad -> historyAdapter.setNativeAd(ad) }, viewModel.filteredHistory.map { it.size })
    }

    private fun setupFilterChips() {
        binding.chipGroupHistoryFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val filter = when (checkedIds.first()) {
                R.id.chip_history_practice -> "practice"
                R.id.chip_history_mock -> "mock"
                else -> "all"
            }
            viewModel.filterHistory(filter)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredHistory.collectLatest { list ->
                    historyAdapter.submitHistoryList(list)
                    if (list.isEmpty()) {
                        binding.rvHistory.visibility = View.GONE
                        binding.layoutEmptyHistory.visibility = View.VISIBLE
                    } else {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.layoutEmptyHistory.visibility = View.GONE
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
