package com.aptirise.app.ui.common

import com.aptirise.app.ui.common.navigateUpSafely
import com.aptirise.app.ui.common.popBackStackSafely
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aptirise.app.R
import com.aptirise.app.databinding.FragmentUpcomingFeatureBinding

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
            navigateUpSafely()
        }

        binding.btnReturnHome.setOnClickListener {
            popBackStackSafely(R.id.homeFragment, false)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}