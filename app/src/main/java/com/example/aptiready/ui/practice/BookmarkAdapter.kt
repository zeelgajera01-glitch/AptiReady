package com.example.aptiready.ui.practice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.model.BookmarkItem
import com.example.aptiready.databinding.ItemBookmarkBinding
import com.example.aptiready.databinding.NativeAdViewBinding
import com.example.aptiready.ui.ads.AdEligibilityPolicy
import com.example.aptiready.ui.ads.NativeAdLoader
import com.google.android.gms.ads.nativead.NativeAd

class BookmarkAdapter(
    private val onRemoveBookmark: (BookmarkItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rawBookmarks: List<BookmarkItem> = emptyList()
    private var nativeAd: NativeAd? = null
    private var presentationItems: List<BookmarkAdapterItem> = emptyList()

    fun submitBookmarkList(list: List<BookmarkItem>) {
        this.rawBookmarks = list
        rebuildPresentationList()
    }

    fun setNativeAd(ad: NativeAd?) {
        if (this.nativeAd === ad) return
        this.nativeAd = ad
        rebuildPresentationList()
        if (ad != null) {
            val index = presentationItems.indexOf(BookmarkAdapterItem.AdItem)
            if (index >= 0) notifyItemChanged(index)
        }
    }

    private fun rebuildPresentationList() {
        val items = mutableListOf<BookmarkAdapterItem>()
        val isEligible = AdEligibilityPolicy.isNativeAdListEligible(AdPlacement.BOOKMARKS_NATIVE, rawBookmarks.size)

        items.addAll(com.example.aptiready.ui.ads.insertAdRow(
            rawBookmarks.map { BookmarkAdapterItem.DataItem(it) as BookmarkAdapterItem },
            BookmarkAdapterItem.AdItem, 5, isEligible && nativeAd != null))

        val previous = presentationItems
        val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = items.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = previous[oldItemPosition]
                val new = items[newItemPosition]
                return if (old is BookmarkAdapterItem.DataItem && new is BookmarkAdapterItem.DataItem)
                    old.bookmark.id == new.bookmark.id
                else old === BookmarkAdapterItem.AdItem && new === BookmarkAdapterItem.AdItem
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
            is BookmarkAdapterItem.DataItem -> TYPE_BOOKMARK
            is BookmarkAdapterItem.AdItem -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_AD) {
            val binding = NativeAdViewBinding.inflate(inflater, parent, false)
            NativeAdViewHolder(binding)
        } else {
            val binding = ItemBookmarkBinding.inflate(inflater, parent, false)
            BookmarkViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = presentationItems[position]) {
            is BookmarkAdapterItem.DataItem -> (holder as BookmarkViewHolder).bind(item.bookmark)
            is BookmarkAdapterItem.AdItem -> (holder as NativeAdViewHolder).bind(nativeAd, parentContext = holder.itemView.context)
        }
    }

    inner class BookmarkViewHolder(private val binding: ItemBookmarkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookmarkItem) {
            binding.tvBookmarkTopic.text = item.topicTitle.uppercase()
            binding.tvBookmarkQuestionText.text = item.questionText

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

    class NativeAdViewHolder(private val binding: NativeAdViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ad: NativeAd?, parentContext: android.content.Context) {
            if (ad != null) {
                val loader = NativeAdLoader(parentContext) { true }
                loader.populateNativeAdView(ad, binding)
                binding.root.visibility = View.VISIBLE
            } else {
                binding.root.visibility = View.GONE
            }
        }
    }

    sealed class BookmarkAdapterItem {
        data class DataItem(val bookmark: BookmarkItem) : BookmarkAdapterItem()
        object AdItem : BookmarkAdapterItem()
    }

    companion object {
        private const val TYPE_BOOKMARK = 0
        private const val TYPE_AD = 1
    }
}
