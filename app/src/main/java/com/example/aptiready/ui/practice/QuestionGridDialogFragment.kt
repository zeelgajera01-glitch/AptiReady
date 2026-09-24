package com.example.aptiready.ui.practice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.aptiready.data.model.SessionSnapshot
import com.example.aptiready.databinding.DialogQuestionGridBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class QuestionGridDialogFragment(
    private val snapshots: List<SessionSnapshot>,
    private val currentPosition: Int,
    private val onSelectPosition: (Int) -> Unit,
    private val onFinishClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogQuestionGridBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQuestionGridBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCloseGrid.setOnClickListener {
            dismiss()
        }

        val adapter = QuestionGridAdapter(currentPosition) { selectedPos ->
            onSelectPosition(selectedPos)
            dismiss()
        }

        binding.rvQuestionGrid.adapter = adapter
        adapter.submitList(snapshots)

        binding.btnFinishPractice.setOnClickListener {
            dismiss()
            onFinishClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}