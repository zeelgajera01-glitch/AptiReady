package com.example.aptiready.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.aptiready.R
import com.example.aptiready.databinding.FragmentUpcomingFeatureBinding

class UpcomingFeatureFragment : Fragment() {

    private var _binding: FragmentUpcomingFeatureBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpcomingFeatureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString("featureTitle") ?: getString(R.string.upcoming_title)
        val description = arguments?.getString("featureDescription") ?: getString(R.string.upcoming_desc)

        binding.tvUpcomingTitle.text = title
        binding.tvUpcomingDesc.text = description

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnReturnHome.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}