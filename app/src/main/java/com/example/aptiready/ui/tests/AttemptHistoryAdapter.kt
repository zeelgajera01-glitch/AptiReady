package com.example.aptiready.ui.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.model.AttemptHistoryItem
import com.example.aptiready.databinding.ItemAttemptHistoryBinding
import com.example.aptiready.databinding.NativeAdViewBinding
import com.example.aptiready.ui.ads.AdEligibilityPolicy
import com.example.aptiready.ui.ads.NativeAdLoader
import com.google.android.gms.ads.nativead.NativeAd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttemptHistoryAdapter(
    private val onItemClick: (AttemptHistoryItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rawHistory: List<AttemptHistoryItem> = emptyList()
    private var nativeAd: NativeAd? = null
    private var presentationItems: List<HistoryAdapterItem> = emptyList()

    fun submitHistoryList(list: List<AttemptHistoryItem>) {
        this.rawHistory = list
        rebuildPresentationList()
    }

    fun setNativeAd(ad: NativeAd?) {
        if (this.nativeAd === ad) return
        this.nativeAd = ad
        rebuildPresentationList()
        if (ad != null) {
            val index = presentationItems.indexOf(HistoryAdapterItem.AdItem)
            if (index >= 0) notifyItemChanged(index)
        }
    }

    private fun rebuildPresentationList() {
        val items = mutableListOf<HistoryAdapterItem>()
        val isEligible = AdEligibilityPolicy.isNativeAdListEligible(AdPlacement.HISTORY_NATIVE, rawHistory.size)

        items.addAll(com.example.aptiready.ui.ads.insertAdRow(
            rawHistory.map { HistoryAdapterItem.DataItem(it) as HistoryAdapterItem },
            HistoryAdapterItem.AdItem, 5, isEligible && nativeAd != null))

        val previous = presentationItems
        val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = items.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = previous[oldItemPosition]
                val new = items[newItemPosition]
                return if (old is HistoryAdapterItem.DataItem && new is HistoryAdapterItem.DataItem)
                    old.history.id == new.history.id && old.history.type == new.history.type
                else old === HistoryAdapterItem.AdItem && new === HistoryAdapterItem.AdItem
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
            is HistoryAdapterItem.DataItem -> TYPE_HISTORY
            is HistoryAdapterItem.AdItem -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_AD) {
            val binding = NativeAdViewBinding.inflate(inflater, parent, false)
            NativeAdViewHolder(binding)
        } else {
            val binding = ItemAttemptHistoryBinding.inflate(inflater, parent, false)
            HistoryViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = presentationItems[position]) {
            is HistoryAdapterItem.DataItem -> (holder as HistoryViewHolder).bind(item.history)
            is HistoryAdapterItem.AdItem -> (holder as NativeAdViewHolder).bind(nativeAd, parentContext = holder.itemView.context)
        }
    }

    inner class HistoryViewHolder(private val binding: ItemAttemptHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttemptHistoryItem) {
            val context = binding.root.context
            binding.tvHistoryTitle.text = item.title
            binding.tvHistorySubtitle.text = item.categoryOrSubtitle
            binding.tvHistoryScoreSummary.text = "${item.earnedSummary} (${item.scorePercentage.toInt()}%)"

            val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            binding.tvHistoryDate.text = dateFormat.format(Date(item.completedAt))

            if (item.type == "MOCK_TEST") {
                binding.tvHistoryBadge.text = "MOCK TEST"
                binding.tvHistoryBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.teal_100))
                binding.tvHistoryBadge.setTextColor(ContextCompat.getColor(context, R.color.teal_800))
            } else {
                binding.tvHistoryBadge.text = "PRACTICE"
                binding.tvHistoryBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.indigo_50))
                binding.tvHistoryBadge.setTextColor(ContextCompat.getColor(context, R.color.indigo_800))
            }

            binding.cardHistoryItem.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    class NativeAdViewHolder(private val binding: NativeAdViewBinding) :
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

    sealed class HistoryAdapterItem {
        data class DataItem(val history: AttemptHistoryItem) : HistoryAdapterItem()
        object AdItem : HistoryAdapterItem()
    }

    companion object {
        private const val TYPE_HISTORY = 0
        private const val TYPE_AD = 1
    }
}
