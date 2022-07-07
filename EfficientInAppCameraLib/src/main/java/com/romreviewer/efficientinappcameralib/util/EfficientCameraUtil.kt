package com.romreviewer.efficientinappcameralib.util

import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.fragment.findNavController
import java.io.File

object EfficientCameraUtil {
    fun cameraImageCapture(
        fragment: Fragment,
        onImageReceived: (file: File) -> Unit
    ) {
        val newContext = fragment.context ?: return
        fragment.findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("img_uri")
            ?.observe(fragment) { uri ->
                if (uri != null) {
                    val file = Uri.parse(uri).getCompressedFile(newContext)
                    onImageReceived.invoke(file)
                }
            }
    }
    fun openCamera(fragment: Fragment){
        val request = NavDeepLinkRequest.Builder
            .fromUri("android-app://com.romreviewer.efficientinappcameralib.ui/FragmentCamera".toUri())
            .build()
        fragment.findNavController().navigate(request)
    }
}