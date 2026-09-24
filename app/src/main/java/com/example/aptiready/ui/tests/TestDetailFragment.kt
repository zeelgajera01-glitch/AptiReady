package com.example.aptiready.ui.tests

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
import com.example.aptiready.data.model.MockTest
import com.example.aptiready.databinding.FragmentTestDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestDetailFragment : Fragment() {

    private var _binding: FragmentTestDetailBinding? = null
    private val binding get() = _binding!!

    private val testId: String by lazy {
        arguments?.getString("testId") ?: "quant_sprint_01"
    }

    private val viewModel: TestCatalogViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        TestCatalogViewModel.Factory(
            app.appContainer.mockTestRepository,
            app.appContainer.authRepository
        )
    }

    private var currentMockTest: MockTest? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnStartMockTest.setOnClickListener {
            handleStartOrResumeClick()
        }

        observeViewModel()
    }

    private fun handleStartOrResumeClick() {
        val app = requireActivity().application as AptiRiseApplication
        val repo = app.appContainer.mockTestRepository
        val ownerId = viewModel.currentOwnerId

        lifecycleScope.launch {
            val activeAttempt = repo.getInProgressAttempt(ownerId)
            if (activeAttempt != null && activeAttempt.testId != testId) {
                // Another test is active for this owner
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Active Mock Test in Progress")
                    .setMessage("You already have an active unfinished test: '${activeAttempt.testTitle}'. Only one active mock test is permitted per account at a time.\n\nWould you like to resume your active test or discard it?")
                    .setPositiveButton("Resume Active Test") { _, _ ->
                        val bundle = Bundle().apply {
                            putString("attemptId", activeAttempt.id)
                        }
                        findNavController().navigate(R.id.action_testDetail_to_mockTest, bundle)
                    }
                    .setNegativeButton("Discard & Start New") { _, _ ->
                        lifecycleScope.launch {
                            repo.discardAttempt(activeAttempt.id)
                            startNewAttempt(ownerId)
                        }
                    }
                    .setNeutralButton("Cancel", null)
                    .show()
            } else if (activeAttempt != null && activeAttempt.testId == testId) {
                // Resume same test
                val bundle = Bundle().apply {
                    putString("attemptId", activeAttempt.id)
                }
                findNavController().navigate(R.id.action_testDetail_to_mockTest, bundle)
            } else {
                startNewAttempt(ownerId)
            }
        }
    }

    private fun startNewAttempt(ownerId: String) {
        val app = requireActivity().application as AptiRiseApplication
        val repo = app.appContainer.mockTestRepository

        lifecycleScope.launch {
            binding.btnStartMockTest.isEnabled = false
            val result = repo.createMockAttempt(ownerId, testId)
            binding.btnStartMockTest.isEnabled = true

            result.onSuccess { attempt ->
                val bundle = Bundle().apply {
                    putString("attemptId", attempt.id)
                }
                findNavController().navigate(R.id.action_testDetail_to_mockTest, bundle)
            }.onFailure { err ->
                Toast.makeText(requireContext(), err.localizedMessage ?: "Failed to start mock test.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mockTests.collectLatest { list ->
                    val match = list.find { it.id == testId }
                    if (match != null) {
                        currentMockTest = match
                        binding.tvTestFullTitle.text = match.title
                        binding.tvTestFullDesc.text = match.description
                        binding.tvDetailDuration.text = "${match.durationSeconds / 60} Mins"
                        binding.tvDetailQuestions.text = "${match.questionIds.size} Qs"
                        binding.tvDetailDifficulty.text = "Medium"
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