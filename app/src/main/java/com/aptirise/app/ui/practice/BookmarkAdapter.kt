package com.aptirise.app.ui.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aptirise.app.data.model.BookmarkItem
import com.aptirise.app.data.model.QuestionText
import com.aptirise.app.databinding.ItemBookmarkBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class BookmarkAdapter(private val onRemoveBookmark: (BookmarkItem) -> Unit) :
    ListAdapter<BookmarkItem, BookmarkAdapter.BookmarkViewHolder>(object : DiffUtil.ItemCallback<BookmarkItem>() {
        override fun areItemsTheSame(old: BookmarkItem, new: BookmarkItem) = old.id == new.id
        override fun areContentsTheSame(old: BookmarkItem, new: BookmarkItem) = old == new
    }) {
    fun submitBookmarkList(items: List<BookmarkItem>) = submitList(items.toList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        BookmarkViewHolder(ItemBookmarkBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) = holder.bind(getItem(position))

    inner class BookmarkViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookmarkItem) {
            binding.tvBookmarkTopic.text = item.topicTitle.uppercase()
            binding.tvBookmarkQuestionText.text = QuestionText.displayText(item.questionText)

            val correctOpt = item.options.find { it.id == item.correctOptionId }
            if (correctOpt != null) {
                binding.tvCorrectOptionLabel.text = "Correct Answer: Option ${correctOpt.id.uppercase()} (${correctOpt.text})"
            }
            binding.tvBookmarkExplanation.text = "Explanation:\n${item.explanation}"

            if (item.isRevealed) {
                binding.layoutSolutionContainer.visibility = View.VISIBLE
                binding.btnRevealSolution.text = "Hide Solution"
            } else {
                binding.layoutSolutionContainer.visibility = View.GONE
                binding.btnRevealSolution.text = "Reveal Solution & Explanation"
            }

            binding.btnRevealSolution.setOnClickListener {
                item.isRevealed = !item.isRevealed
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos)
                }
            }

            binding.btnRemoveBookmark.setOnClickListener {
                onRemoveBookmark(item)
            }
        }
    }
}
