package com.romreviewer.efficientinappcameralib.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.romreviewer.efficientinappcameralib.databinding.FragmentCapturedImagePreviewBinding

class CapturedImagePreviewFragment :
    BottomSheetDialogFragment() {
    private lateinit var binding: FragmentCapturedImagePreviewBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCapturedImagePreviewBinding.inflate(inflater, container, false)
        val imageUri = arguments?.getString("imgUri")
        if (imageUri?.isNotEmpty() == true) {
            context?.let { Glide.with(it).load(imageUri).into(binding.ivImagePreview) }
        }
        binding.btnAccept.setOnClickListener {
            findNavController().previousBackStackEntry?.savedStateHandle?.set(
                "img_uri",
                imageUri
            )
            findNavController().navigateUp()
        }
        binding.btnReject.setOnClickListener {
            findNavController().navigateUp()
        }
        return binding.root
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {

            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it2 ->
                val behaviour = BottomSheetBehavior.from(it2)
                behaviour.addBottomSheetCallback(object :
                    BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                            behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {}
                })
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
                val layoutParams = it2.layoutParams
                layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
                it2.layoutParams = layoutParams
            }
        }
        return dialog
    }

}