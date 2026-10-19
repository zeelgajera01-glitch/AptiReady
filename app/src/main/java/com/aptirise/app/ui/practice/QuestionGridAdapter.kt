package com.aptirise.app.ui.practice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aptirise.app.R
import com.aptirise.app.data.model.SessionSnapshot
import com.aptirise.app.databinding.ItemQuestionGridBinding

class QuestionGridAdapter(
    private val currentPosition: Int,
    private val onPositionClick: (Int) -> Unit
) : ListAdapter<SessionSnapshot, QuestionGridAdapter.GridViewHolder>(GridDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemQuestionGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GridViewHolder(private val binding: ItemQuestionGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: SessionSnapshot, position: Int) {
            val context = binding.root.context
            binding.tvGridNumber.text = (position + 1).toString()

            val isCurrent = position == currentPosition

            val (bgColor, textColor) = when {
                snapshot.isSubmitted -> {
                    if (snapshot.isCorrect()) {
                        ContextCompat.getColor(context, R.color.atelier_success_bg) to ContextCompat.getColor(context, R.color.atelier_success)
                    } else {
                        ContextCompat.getColor(context, R.color.atelier_error_bg) to ContextCompat.getColor(context, R.color.atelier_on_error_bg)
                    }
                }
                snapshot.selectedOptionId != null -> {
                    ContextCompat.getColor(context, R.color.primary_container) to ContextCompat.getColor(context, R.color.on_primary_container)
                }
                else -> {
                    ContextCompat.getColor(context, R.color.slate_100) to ContextCompat.getColor(context, R.color.slate_800)
                }
            }

            binding.cardGridItem.setCardBackgroundColor(bgColor)
            binding.tvGridNumber.setTextColor(textColor)

            if (isCurrent) {
                binding.cardGridItem.strokeWidth = 4
                binding.cardGridItem.strokeColor = ContextCompat.getColor(context, R.color.primary)
            } else {
                binding.cardGridItem.strokeWidth = 1
                binding.cardGridItem.strokeColor = ContextCompat.getColor(context, R.color.slate_200)
            }

            binding.cardGridItem.contentDescription = "Question ${position + 1}, " + when {
                snapshot.isSubmitted && snapshot.isCorrect() -> "correct"
                snapshot.isSubmitted -> "incorrect"
                snapshot.selectedOptionId != null -> "selected, not submitted"
                else -> "unanswered"
            } + if (isCurrent) ", current question" else ""
            binding.cardGridItem.setOnClickListener {
                onPositionClick(position)
            }
        }
    }

    private object GridDiffCallback : DiffUtil.ItemCallback<SessionSnapshot>() {
        override fun areItemsTheSame(oldItem: SessionSnapshot, newItem: SessionSnapshot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionSnapshot, newItem: SessionSnapshot): Boolean {
            return oldItem == newItem
        }
    }
}