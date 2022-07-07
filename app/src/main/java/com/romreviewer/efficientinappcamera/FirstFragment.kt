package com.romreviewer.efficientinappcamera

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import com.romreviewer.efficientinappcamera.databinding.FragmentFirstBinding
import com.romreviewer.efficientinappcameralib.util.EfficientCameraUtil

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EfficientCameraUtil.cameraImageCapture(this) {
            Log.d("LogTag", "onViewCreated: ${it.toUri()}")
        }
        binding.buttonFirst.setOnClickListener {
            EfficientCameraUtil.openCamera(this)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}