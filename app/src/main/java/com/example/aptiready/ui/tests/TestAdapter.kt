package com.example.aptiready.ui.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.data.model.MockTest
import com.example.aptiready.databinding.ItemTestBinding
import com.example.aptiready.databinding.NativeAdViewBinding
import com.example.aptiready.ui.ads.AdEligibilityPolicy
import com.example.aptiready.ui.ads.NativeAdLoader
import com.google.android.gms.ads.nativead.NativeAd

class TestAdapter(
    private val onTestClick: (MockTest) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rawTests: List<MockTest> = emptyList()
    private var nativeAd: NativeAd? = null
    private var presentationItems: List<TestAdapterItem> = emptyList()

    fun submitMockTestList(list: List<MockTest>) {
        this.rawTests = list
        rebuildPresentationList()
    }

    fun setNativeAd(ad: NativeAd?) {
        if (this.nativeAd === ad) return
        this.nativeAd = ad
        rebuildPresentationList()
        if (ad != null) {
            val index = presentationItems.indexOf(TestAdapterItem.AdItem)
            if (index >= 0) notifyItemChanged(index)
        }
    }

    private fun rebuildPresentationList() {
        val items = mutableListOf<TestAdapterItem>()
        val isEligible = AdEligibilityPolicy.isNativeAdListEligible(AdPlacement.TEST_CATALOG_NATIVE, rawTests.size)

        items.addAll(com.example.aptiready.ui.ads.insertAdRow(
            rawTests.map { TestAdapterItem.DataItem(it) as TestAdapterItem },
            TestAdapterItem.AdItem, 3, isEligible && nativeAd != null))

        val previous = presentationItems
        val diff = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = items.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val old = previous[oldItemPosition]
                val new = items[newItemPosition]
                return if (old is TestAdapterItem.DataItem && new is TestAdapterItem.DataItem)
                    old.test.id == new.test.id
                else old === TestAdapterItem.AdItem && new === TestAdapterItem.AdItem
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
            is TestAdapterItem.DataItem -> TYPE_TEST
            is TestAdapterItem.AdItem -> TYPE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_AD) {
            val binding = NativeAdViewBinding.inflate(inflater, parent, false)
            NativeAdViewHolder(binding)
        } else {
            val binding = ItemTestBinding.inflate(inflater, parent, false)
            TestViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = presentationItems[position]) {
            is TestAdapterItem.DataItem -> (holder as TestViewHolder).bind(item.test)
            is TestAdapterItem.AdItem -> (holder as NativeAdViewHolder).bind(nativeAd, parentContext = holder.itemView.context)
        }
    }

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

    sealed class TestAdapterItem {
        data class DataItem(val test: MockTest) : TestAdapterItem()
        object AdItem : TestAdapterItem()
    }

    companion object {
        private const val TYPE_TEST = 0
        private const val TYPE_AD = 1
    }
}
