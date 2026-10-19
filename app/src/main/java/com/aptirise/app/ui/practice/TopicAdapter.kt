package com.aptirise.app.ui.practice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aptirise.app.R
import com.aptirise.app.data.model.Difficulty
import com.aptirise.app.data.model.Topic
import com.aptirise.app.databinding.ItemTopicBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class TopicAdapter(private val onTopicClick: (Topic) -> Unit) :
    ListAdapter<Topic, TopicAdapter.TopicViewHolder>(object : DiffUtil.ItemCallback<Topic>() {
        override fun areItemsTheSame(old: Topic, new: Topic) = old.id == new.id
        override fun areContentsTheSame(old: Topic, new: Topic) = old == new
    }) {
    fun submitTopicList(items: List<Topic>) = submitList(items.toList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TopicViewHolder(ItemTopicBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) = holder.bind(getItem(position))

    inner class TopicViewHolder(private val binding: ItemTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(topic: Topic) {
            val context = binding.root.context
            binding.tvTopicTitle.text = topic.title
            binding.tvTopicCategory.text = topic.categoryName.uppercase()
            binding.tvTopicDesc.text = topic.description
            binding.tvTopicCount.text = context.getString(R.string.questions_count_format, topic.sampleQuestionCount)

            binding.tvTopicDifficulty.text = topic.difficulty.getDisplayName().uppercase()
            val diffColor = when (topic.difficulty) {
                Difficulty.EASY -> ContextCompat.getColor(context, R.color.difficulty_easy)
                Difficulty.MEDIUM -> ContextCompat.getColor(context, R.color.difficulty_medium)
                Difficulty.HARD -> ContextCompat.getColor(context, R.color.difficulty_hard)
            }
            binding.tvTopicDifficulty.setTextColor(diffColor)

            binding.cardTopic.setOnClickListener {
                onTopicClick(topic)
            }
        }
    }
}
