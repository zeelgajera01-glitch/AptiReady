package com.example.aptiready.ui.tests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.R
import com.example.aptiready.data.model.MockQuestionSnapshot
import com.example.aptiready.databinding.ItemMockQuestionGridBinding

class MockQuestionGridAdapter(
    private val currentPosition: Int,
    private val onPositionClick: (Int) -> Unit
) : ListAdapter<MockQuestionSnapshot, MockQuestionGridAdapter.GridViewHolder>(GridDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemMockQuestionGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GridViewHolder(private val binding: ItemMockQuestionGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(snapshot: MockQuestionSnapshot, position: Int) {
            val context = binding.root.context
            binding.tvMockGridNumber.text = (position + 1).toString()

            val isCurrent = position == currentPosition

            val (bgColor, textColor, strokeColor) = when {
                snapshot.selectedOptionId != null -> {
                    Triple(
                        ContextCompat.getColor(context, R.color.teal_100),
                        ContextCompat.getColor(context, R.color.teal_800),
                        ContextCompat.getColor(context, R.color.teal_700)
                    )
                }
                snapshot.isVisited -> {
                    Triple(
                        ContextCompat.getColor(context, R.color.indigo_50),
                        ContextCompat.getColor(context, R.color.indigo_900),
                        ContextCompat.getColor(context, R.color.indigo_500)
                    )
                }
                else -> {
                    Triple(
                        ContextCompat.getColor(context, R.color.slate_100),
                        ContextCompat.getColor(context, R.color.slate_800),
                        ContextCompat.getColor(context, R.color.slate_300)
                    )
                }
            }

            binding.cardMockGridItem.setCardBackgroundColor(bgColor)
            binding.tvMockGridNumber.setTextColor(textColor)

            if (snapshot.isMarkedForReview) {
                binding.ivGridReviewFlag.visibility = View.VISIBLE
            } else {
                binding.ivGridReviewFlag.visibility = View.GONE
            }

            if (isCurrent) {
                binding.cardMockGridItem.strokeWidth = 4
                binding.cardMockGridItem.strokeColor = ContextCompat.getColor(context, R.color.primary)
            } else {
                binding.cardMockGridItem.strokeWidth = 1
                binding.cardMockGridItem.strokeColor = strokeColor
            }

            binding.cardMockGridItem.setOnClickListener {
                onPositionClick(position)
            }
        }
    }

    private object GridDiffCallback : DiffUtil.ItemCallback<MockQuestionSnapshot>() {
        override fun areItemsTheSame(oldItem: MockQuestionSnapshot, newItem: MockQuestionSnapshot): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MockQuestionSnapshot, newItem: MockQuestionSnapshot): Boolean {
            return oldItem == newItem
        }
    }
}