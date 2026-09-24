package com.example.aptiready.ui.tests

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
import com.example.aptiready.R
import com.example.aptiready.data.model.AdPlacement
import com.example.aptiready.databinding.FragmentTestCatalogBinding
import com.example.aptiready.ui.ads.AdConfig
import com.example.aptiready.ui.ads.NativePlacementBinding
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TestCatalogFragment : Fragment() {

    private var _binding: FragmentTestCatalogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TestCatalogViewModel by viewModels {
        val app = requireActivity().application as AptiRiseApplication
        TestCatalogViewModel.Factory(
            app.appContainer.mockTestRepository,
            app.appContainer.authRepository
        )
    }

    private lateinit var testAdapter: TestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTestCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        setupNativeAd()
    }

    private fun setupRecyclerView() {
        testAdapter = TestAdapter { test ->
            val bundle = Bundle().apply {
                putString("testId", test.id)
            }
            findNavController().navigate(R.id.action_testCatalog_to_testDetail, bundle)
        }

        binding.rvTests.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = testAdapter
        }
    }

    private fun setupNativeAd() {
        val app = requireActivity().application as AptiRiseApplication
        NativePlacementBinding(requireContext(), viewLifecycleOwner,
            app.appContainer.consentManager, AdPlacement.TEST_CATALOG_NATIVE,
            { ad -> testAdapter.setNativeAd(ad) }, viewModel.mockTests.map { it.size })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.mockTests.collectLatest { list ->
                    testAdapter.submitMockTestList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
