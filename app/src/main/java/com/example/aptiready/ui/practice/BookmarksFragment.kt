package com.example.aptiready.ui.practice

import com.example.aptiready.ui.common.navigateUpSafely
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.aptiready.AptiRiseApplication
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.databinding.FragmentBookmarksBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookmarksViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        BookmarksViewModel.Factory(
            app.appContainer.practiceRepository,
            app.appContainer.authRepository
        )
    }

    private lateinit var bookmarkAdapter: BookmarkAdapter
    private var allBookmarks: List<com.example.aptiready.data.model.BookmarkItem> = emptyList()
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            navigateUpSafely()
        }

        setupRecyclerView()
        binding.etBookmarkSearch.setText(searchQuery)
        binding.etBookmarkSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim().orEmpty()
                renderBookmarks()
            }
        })
        observeViewModel()
        setupNativeAd()
    }

    private fun setupRecyclerView() {
        bookmarkAdapter = BookmarkAdapter { bookmarkItem ->
            viewModel.removeBookmark(bookmarkItem.questionId)
        }

        binding.rvBookmarks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookmarkAdapter
        }
    }

    private fun setupNativeAd() {
        val app = requireActivity().application as AptiRiseApplication
        NativePlacementBinding(requireContext(), viewLifecycleOwner,
            app.appContainer.consentManager, AdPlacement.BOOKMARKS_NATIVE,
            { ad -> bookmarkAdapter.setNativeAd(ad) }, viewModel.bookmarks.map { it.size })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookmarks.collectLatest { list ->
                    allBookmarks = list
                    renderBookmarks()
                }
            }
        }
    }

    private fun renderBookmarks() {
        val visible = allBookmarks.filter { item ->
            searchQuery.isBlank() || item.questionText.contains(searchQuery, ignoreCase = true) ||
                item.topicTitle.contains(searchQuery, ignoreCase = true) ||
                item.explanation.contains(searchQuery, ignoreCase = true)
        }
        binding.tvBookmarkCount.text = if (searchQuery.isBlank()) "${allBookmarks.size} saved"
            else "${visible.size} shown of ${allBookmarks.size} saved"
        bookmarkAdapter.submitBookmarkList(visible)
        binding.rvBookmarks.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        binding.layoutEmptyBookmarks.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        binding.tvBookmarkEmptyTitle.text = if (allBookmarks.isEmpty()) "Your study codex starts here" else "No matching bookmarks"
        binding.tvBookmarkEmptyDescription.text = if (allBookmarks.isEmpty())
            "Tap the bookmark icon during practice or answer review to save a question."
            else "Try another topic or keyword."
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
