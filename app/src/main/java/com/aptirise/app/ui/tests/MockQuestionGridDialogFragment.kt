package com.aptirise.app.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aptirise.app.databinding.DialogMockQuestionGridBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** No constructor data or callbacks: Android can restore this dialog after process death. */
class MockQuestionGridDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogMockQuestionGridBinding? = null
    private var resultSent = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogMockQuestionGridBinding.inflate(inflater, container, false)
        return requireNotNull(_binding).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = requireNotNull(_binding)
        val model = (parentFragment as? MockTestFragment)?.viewModel ?: return
        binding.btnCloseMockGrid.setOnClickListener { dismiss() }
        binding.btnSubmitMockFromGrid.setOnClickListener { sendResult(Bundle().apply { putBoolean("finish", true) }) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(model.snapshots, model.currentPosition) { rows, position -> rows to position }
                    .collect { (rows, position) ->
                        val adapter = MockQuestionGridAdapter(position) { selected ->
                            sendResult(Bundle().apply { putInt("position", selected) })
                        }
                        binding.rvMockQuestionGrid.adapter = adapter
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
        parentFragmentManager.setFragmentResult("MockQuestionGridResult", result)
    }

    override fun onDestroyView() {
        _binding?.rvMockQuestionGrid?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
