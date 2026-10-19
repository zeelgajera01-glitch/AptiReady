package com.aptirise.app.ui.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aptirise.app.databinding.DialogQuestionGridBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** No constructor data or callbacks: Android can restore this dialog after process death. */
class QuestionGridDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogQuestionGridBinding? = null
    private var resultSent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogQuestionGridBinding.inflate(inflater, container, false)
        return requireNotNull(_binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = requireNotNull(_binding)
        val model = (parentFragment as? PracticeSessionFragment)?.viewModel ?: return
        binding.btnCloseGrid.setOnClickListener { dismiss() }
        binding.btnFinishPractice.setOnClickListener { sendResult(Bundle().apply { putBoolean("finish", true) }) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(model.snapshots, model.currentPosition) { rows, position -> rows to position }
                    .collect { (rows, position) ->
                        val adapter = QuestionGridAdapter(position) { selected ->
                            sendResult(Bundle().apply { putInt("position", selected) })
                        }
                        binding.rvQuestionGrid.adapter = adapter
                        adapter.submitList(rows)
                    }
            }
        }
    }

    private fun sendResult(result: Bundle) {
        if (resultSent || parentFragmentManager.isStateSaved) return
        resultSent = true
        // Dismiss first so a finish confirmation cannot stack above the grid.
        dismiss()
        parentFragmentManager.setFragmentResult("QuestionGridResult", result)
    }

    override fun onDestroyView() {
        _binding?.rvQuestionGrid?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
