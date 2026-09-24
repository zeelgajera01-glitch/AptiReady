package com.example.aptiready.ui.practice

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
import com.example.aptiready.databinding.FragmentPracticeReviewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PracticeReviewFragment : Fragment() {

    private var _binding: FragmentPracticeReviewBinding? = null
    private val binding get() = _binding!!

    private val sessionId: String by lazy {
        arguments?.getString("sessionId") ?: ""
    }

    private val viewModel: PracticeReviewViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        PracticeReviewViewModel.Factory(
            sessionId,
            app.appContainer.practiceRepository,
            app.appContainer.authRepository
        )
    }

    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeReviewBinding.inflate(inflater, container, false)
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
    }

    private fun setupRecyclerView() {
        reviewAdapter = ReviewAdapter { snapshot ->
            viewModel.toggleBookmark(snapshot)
        }

        binding.rvReviewQuestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reviewAdapter
        }
    }

    private fun setupFilterChips() {
        binding.chipGroupReviewFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val filter = when (checkedIds.first()) {
                R.id.chip_filter_correct -> "correct"
                R.id.chip_filter_incorrect -> "incorrect"
                R.id.chip_filter_skipped -> "skipped"
                else -> "all"
            }
            viewModel.filterSnapshots(filter)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredSnapshots.collectLatest { snapshots ->
                    reviewAdapter.submitList(snapshots)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}