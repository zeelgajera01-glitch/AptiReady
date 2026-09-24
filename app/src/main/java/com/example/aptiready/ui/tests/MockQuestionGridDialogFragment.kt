package com.example.aptiready.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.aptiready.data.model.MockQuestionSnapshot
import com.example.aptiready.databinding.DialogMockQuestionGridBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MockQuestionGridDialogFragment(
    private val snapshots: List<MockQuestionSnapshot>,
    private val currentPosition: Int,
    private val onSelectPosition: (Int) -> Unit,
    private val onSubmitClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogMockQuestionGridBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMockQuestionGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseMockGrid.setOnClickListener {
            dismiss()
        }

        val adapter = MockQuestionGridAdapter(currentPosition) { selectedPos ->
            onSelectPosition(selectedPos)
            dismiss()
        }

        binding.rvMockQuestionGrid.adapter = adapter
        adapter.submitList(snapshots)

        binding.btnSubmitMockFromGrid.setOnClickListener {
            dismiss()
            onSubmitClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}