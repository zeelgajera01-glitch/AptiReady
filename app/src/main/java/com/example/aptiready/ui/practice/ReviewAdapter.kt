package com.example.aptiready.ui.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.R
import com.example.aptiready.data.model.QuestionText
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
            binding.tvReviewQuestionText.text = QuestionText.displayText(snapshot.questionText)

            val (status, bg, fg) = when {
                snapshot.isCorrect() -> Triple("CORRECT", R.color.atelier_success_bg, R.color.atelier_success)
                snapshot.isIncorrect() -> Triple("INCORRECT", R.color.atelier_error_bg, R.color.atelier_on_error_bg)
                else -> Triple("SKIPPED", R.color.atelier_soft, R.color.atelier_muted)
            }
            binding.tvReviewStatusBadge.text = status
            com.example.aptiready.ui.common.AtelierUi.pill(binding.tvReviewStatusBadge, bg, fg)
            com.example.aptiready.ui.common.AtelierUi.pill(binding.tvUserSelectedAnswer, bg, fg)

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

            binding.tvReviewExplanation.text = snapshot.explanation

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