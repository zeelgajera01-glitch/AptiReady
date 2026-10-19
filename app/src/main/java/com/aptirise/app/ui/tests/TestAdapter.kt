package com.aptirise.app.ui.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aptirise.app.R
import com.aptirise.app.data.model.MockTest
import com.aptirise.app.databinding.ItemTestBinding
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil

class TestAdapter(private val onTestClick: (MockTest) -> Unit) :
    ListAdapter<MockTest, TestAdapter.TestViewHolder>(object : DiffUtil.ItemCallback<MockTest>() {
        override fun areItemsTheSame(old: MockTest, new: MockTest) = old.id == new.id
        override fun areContentsTheSame(old: MockTest, new: MockTest) = old == new
    }) {
    fun submitMockTestList(items: List<MockTest>) = submitList(items.toList())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TestViewHolder(ItemTestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) = holder.bind(getItem(position))

    inner class TestViewHolder(private val binding: ItemTestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(test: MockTest) {
            val context = binding.root.context
            binding.tvTestTitle.text = test.title
            binding.tvTestCategory.text = "TIMED MOCK TEST"
            binding.tvTestDesc.text = test.description
            binding.tvTestDuration.text = context.getString(R.string.duration_format, test.durationSeconds / 60)
            binding.tvTestQsCount.text = context.getString(R.string.questions_count_format, test.questionIds.size)

            binding.tvTestDifficulty.text = "MEDIUM"
            binding.tvTestDifficulty.setTextColor(ContextCompat.getColor(context, R.color.difficulty_medium))

            binding.cardTest.setOnClickListener {
                onTestClick(test)
            }
        }
    }
}
