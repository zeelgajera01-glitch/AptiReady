package com.example.aptiready.ui.practice

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
            findNavController().navigateUp()
        }

        setupRecyclerView()
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
                    bookmarkAdapter.submitBookmarkList(list)
                    if (list.isEmpty()) {
                        binding.rvBookmarks.visibility = View.GONE
                        binding.layoutEmptyBookmarks.visibility = View.VISIBLE
                    } else {
                        binding.rvBookmarks.visibility = View.VISIBLE
                        binding.layoutEmptyBookmarks.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
