package com.example.aptiready.ui.practice

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.model.Difficulty
import com.example.aptiready.data.model.Topic
import com.example.aptiready.databinding.ItemTopicBinding
import com.example.aptiready.databinding.NativeAdViewBinding
import com.example.aptiready.ui.ads.AdEligibilityPolicy
import com.example.aptiready.ui.ads.NativeAdLoader
import com.google.android.gms.ads.nativead.NativeAd

class TopicAdapter(
    private val onTopicClick: (Topic) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rawTopics: List<Topic> = emptyList()
    private var nativeAd: NativeAd? = null
    private var presentationItems: List<TopicAdapterItem> = emptyList()

    fun submitTopicList(topics: List<Topic>) {
        this.rawTopics = topics
        rebuildPresentationList()
    }

    fun setNativeAd(ad: NativeAd?) {
        if (this.nativeAd === ad) return
        this.nativeAd = ad
        rebuildPresentationList()
        if (ad != null) {
            val index = presentationItems.indexOf(TopicAdapterItem.AdItem)
            if (index >= 0) notifyItemChanged(index)
        }
    }

    private fun rebuildPresentationList() {
        val items = mutableListOf<TopicAdapterItem>()
        val isEligible = AdEligibilityPolicy.isNativeAdListEligible(AdPlacement.TOPIC_LIST_NATIVE, rawTopics.size)

        items.addAll(com.example.aptiready.ui.ads.insertAdRow(
            rawTopics.map { TopicAdapterItem.DataItem(it) as TopicAdapterItem },
            TopicAdapterItem.AdItem, 4, isEligible && nativeAd != null))

        val previous = presentationItems
        val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = items.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = previous[oldItemPosition]
                val new = items[newItemPosition]
                return if (old is TopicAdapterItem.DataItem && new is TopicAdapterItem.DataItem)
                    old.topic.id == new.topic.id
                else old === TopicAdapterItem.AdItem && new === TopicAdapterItem.AdItem
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                previous[oldItemPosition] == items[newItemPosition]
        })
        this.presentationItems = items
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = presentationItems.size

    override fun getItemViewType(position: Int): Int {
        return when (presentationItems[position]) {
            is TopicAdapterItem.DataItem -> TYPE_TOPIC
            is TopicAdapterItem.AdItem -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_AD) {
            val binding = NativeAdViewBinding.inflate(inflater, parent, false)
            NativeAdViewHolder(binding)
        } else {
            val binding = ItemTopicBinding.inflate(inflater, parent, false)
            TopicViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = presentationItems[position]) {
            is TopicAdapterItem.DataItem -> (holder as TopicViewHolder).bind(item.topic)
            is TopicAdapterItem.AdItem -> (holder as NativeAdViewHolder).bind(nativeAd, parentContext = holder.itemView.context)
        }
    }

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

    inner class NativeAdViewHolder(private val binding: NativeAdViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ad: NativeAd?, parentContext: android.content.Context) {
            if (ad != null) {
                val loader = NativeAdLoader(parentContext) { true }
                loader.populateNativeAdView(ad, binding)
                binding.root.visibility = android.view.View.VISIBLE
            } else {
                binding.root.visibility = android.view.View.GONE
            }
        }
    }

    sealed class TopicAdapterItem {
        data class DataItem(val topic: Topic) : TopicAdapterItem()
        object AdItem : TopicAdapterItem()
    }

    companion object {
        private const val TYPE_TOPIC = 0
        private const val TYPE_AD = 1
    }
}
