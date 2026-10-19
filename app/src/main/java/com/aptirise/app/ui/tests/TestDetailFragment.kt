package com.aptirise.app.ui.tests

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
import com.aptirise.app.data.model.MockTest
import com.aptirise.app.databinding.FragmentTestDetailBinding
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
            app.appContainer.learningOwner
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
            navigateUpSafely()
        }

        binding.btnStartMockTest.setOnClickListener {
            handleStartOrResumeClick()
        }

        observeViewModel()
    }

    private var startPending = false

    private fun handleStartOrResumeClick() {
        if (startPending) return
        startPending = true
        binding.btnStartMockTest.isEnabled = false
        val repo = (requireActivity().application as AptiRiseApplication).appContainer.mockTestRepository
        val ownerId = viewModel.currentOwnerId
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val active = repo.getInProgressAttempt(ownerId)
                if (active != null && active.testId != testId) {
                    startPending = false
                    binding.btnStartMockTest.isEnabled = true
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Active Mock Test in Progress")
                        .setMessage("Resume '${active.testTitle}' or discard it and start this test?")
                        .setPositiveButton("Resume Active Test") { _, _ -> openAttempt(active.id) }
                        .setNegativeButton("Discard & Start New") { _, _ ->
                            startNewAttempt(ownerId, active.id)
                        }
                        .setNeutralButton("Cancel", null)
                        .showOnce(this@TestDetailFragment)
                } else if (active != null) {
                    openAttempt(active.id)
                } else {
                    createAndOpenAttempt(ownerId)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                showStartFailure()
            }
        }
    }

    private fun startNewAttempt(ownerId: String, discardId: String) {
        if (startPending) return
        startPending = true
        binding.btnStartMockTest.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val repo = (requireActivity().application as AptiRiseApplication).appContainer.mockTestRepository
                repo.discardAttempt(discardId)
                createAndOpenAttempt(ownerId)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                showStartFailure()
            }
        }
    }

    private suspend fun createAndOpenAttempt(ownerId: String) {
        val repo = (requireActivity().application as AptiRiseApplication).appContainer.mockTestRepository
        val attempt = repo.createMockAttempt(ownerId, testId).getOrThrow()
        openAttempt(attempt.id)
    }

    private fun openAttempt(id: String) {
        navigateSafely(R.id.action_testDetail_to_mockTest, Bundle().apply { putString("attemptId", id) })
    }

    private fun showStartFailure() {
        startPending = false
        _binding?.btnStartMockTest?.isEnabled = true
        context?.let { Toast.makeText(it, "Could not start this test. Please try again.", Toast.LENGTH_LONG).show() }
    }

    override fun onStart() {
        super.onStart()
        startPending = false
        binding.btnStartMockTest.isEnabled = true
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
                        binding.tvDetailDifficulty.text = "Timed aptitude test"
                        binding.tvMarkingScheme.text = "+${match.marksPerCorrect} per correct answer · No negative marking"
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