package com.example.aptiready.ui.progress

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
import com.example.aptiready.data.model.UserProgress
import com.example.aptiready.databinding.FragmentProgressBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProgressFragment : Fragment() {

    private var _binding: FragmentProgressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProgressViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        ProgressViewModel.Factory(
            app.appContainer.progressRepository,
            app.appContainer.authRepository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchSampleData.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showSampleData()
            } else {
                updateUI(viewModel.realProgress.value)
            }
        }

        binding.btnViewAttemptHistory.setOnClickListener {
            findNavController().navigate(R.id.action_progress_to_attemptHistory)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.realProgress.collectLatest { real ->
                    if (!binding.switchSampleData.isChecked) {
                        updateUI(real)
                    }
                }
            }
        }
    }

    private fun updateUI(progress: UserProgress?) {
        val hasData = progress != null && (progress.totalQuestionsAnswered > 0 || progress.topicsCompleted > 0)

        if (hasData) {
            binding.layoutZeroData.visibility = View.GONE
            binding.scrollSamplePreview.visibility = View.VISIBLE
            bindProgressData(progress!!)
        } else {
            binding.layoutZeroData.visibility = View.VISIBLE
            binding.scrollSamplePreview.visibility = View.GONE
        }
    }

    private fun bindProgressData(p: UserProgress) {
        // Main metrics
        binding.tvRealAccuracy.text = "${p.accuracyPercentage}%"
        binding.tvRealStreak.text = if (p.streakDays == 1) "1 Day" else "${p.streakDays} Days"

        // Mastery cards
        binding.tvQuantMasteryLabel.text = "${p.quantMastery}% (${getMasteryLevel(p.quantMastery)})"
        binding.progressQuantMastery.progress = p.quantMastery

        binding.tvLogicalMasteryLabel.text = "${p.logicalMastery}% (${getMasteryLevel(p.logicalMastery)})"
        binding.progressLogicalMastery.progress = p.logicalMastery

        binding.tvVerbalMasteryLabel.text = "${p.verbalMastery}% (${getMasteryLevel(p.verbalMastery)})"
        binding.progressVerbalMastery.progress = p.verbalMastery
    }

    private fun getMasteryLevel(pct: Int): String {
        return when {
            pct >= 90 -> "Mastered"
            pct >= 70 -> "Proficient"
            pct >= 40 -> "Good"
            else -> "Newbie"
        }
    }

    private fun showSampleData() {
        binding.layoutZeroData.visibility = View.GONE
        binding.scrollSamplePreview.visibility = View.VISIBLE
        
        // Reset to sample values (hardcoded as per original XML intent)
        binding.tvRealAccuracy.text = "82%"
        binding.tvRealStreak.text = "5 Days"
        binding.tvQuantMasteryLabel.text = "80% (Proficient)"
        binding.progressQuantMastery.progress = 80
        binding.tvLogicalMasteryLabel.text = "75% (Good)"
        binding.progressLogicalMastery.progress = 75
        binding.tvVerbalMasteryLabel.text = "90% (Mastered)"
        binding.progressVerbalMastery.progress = 90
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}