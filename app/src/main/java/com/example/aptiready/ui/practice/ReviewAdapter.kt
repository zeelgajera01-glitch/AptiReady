package com.example.aptiready.ui.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.R
import com.example.aptiready.data.model.SessionSnapshot
import com.example.aptiready.databinding.ItemReviewQuestionBinding

class ReviewAdapter(
    private val onBookmarkToggle: (SessionSnapshot) -> Unit
) : ListAdapter<SessionSnapshot, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(private val binding: ItemReviewQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: SessionSnapshot) {
            val context = binding.root.context
            binding.tvReviewQuestionText.text = "${snapshot.position + 1}. ${snapshot.questionText}"

            // Status badge
            when {
                snapshot.isCorrect() -> {
                    binding.tvReviewStatusBadge.text = "CORRECT"
                    binding.tvReviewStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_100))
                    binding.tvReviewStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.teal_800))
                }
                snapshot.isIncorrect() -> {
                    binding.tvReviewStatusBadge.text = "INCORRECT"
                    binding.tvReviewStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.indigo_100))
                    binding.tvReviewStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.indigo_900))
                }
                else -> {
                    binding.tvReviewStatusBadge.text = "SKIPPED"
                    binding.tvReviewStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.slate_200))
                    binding.tvReviewStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.slate_800))
                }
            }

            // User Selected Answer
            val userOpt = snapshot.options.find { it.id == snapshot.selectedOptionId }
            if (userOpt != null) {
                binding.tvUserSelectedAnswer.text = "Your Answer: Option ${userOpt.id.uppercase()} (${userOpt.text})"
                binding.tvUserSelectedAnswer.visibility = View.VISIBLE
            } else {
                binding.tvUserSelectedAnswer.text = "Your Answer: None (Skipped)"
                binding.tvUserSelectedAnswer.visibility = View.VISIBLE
            }

            // Correct Answer
            val correctOpt = snapshot.options.find { it.id == snapshot.correctOptionId }
            if (correctOpt != null) {
                binding.tvCorrectAnswerText.text = "Correct Answer: Option ${correctOpt.id.uppercase()} (${correctOpt.text})"
            }

            binding.tvReviewExplanation.text = "Explanation:\n${snapshot.explanation}"

            // Bookmark icon
            val starTint = if (snapshot.isBookmarked) R.color.primary else R.color.slate_400
            binding.btnReviewBookmark.setColorFilter(ContextCompat.getColor(context, starTint))

            binding.btnReviewBookmark.setOnClickListener {
                onBookmarkToggle(snapshot)
            }
        }
    }

    private object ReviewDiffCallback : DiffUtil.ItemCallback<SessionSnapshot>() {
        override fun areItemsTheSame(oldItem: SessionSnapshot, newItem: SessionSnapshot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SessionSnapshot, newItem: SessionSnapshot): Boolean {
            return oldItem == newItem
        }
    }
}