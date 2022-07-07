package com.romreviewer.efficientinappcameralib.ui

import android.Manifest
import android.animation.Animator
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.common.util.concurrent.ListenableFuture
import com.permissionx.guolindev.PermissionX
import com.romreviewer.efficientinappcameralib.R
import com.romreviewer.efficientinappcameralib.databinding.FragmentCameraBinding
import com.romreviewer.efficientinappcameralib.util.toastS
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FragmentCamera :
    BottomSheetDialogFragment() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var flashEnabled = false
    private var hasFrontCamera = false
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var binding: FragmentCameraBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCameraBinding.inflate(inflater, container, false)
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("img_uri")
            ?.observe(viewLifecycleOwner) { uri ->
                if (uri != null) {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(
                        "img_uri",
                        uri
                    )
                    findNavController().navigateUp()
                }
            }
        return binding.root
    }

    private fun flipCamera() {
        if (lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA) lensFacing =
            CameraSelector.DEFAULT_BACK_CAMERA else if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA && hasFrontCamera
        ) lensFacing =
            CameraSelector.DEFAULT_FRONT_CAMERA
        startCamera()
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            if (this.isRemoving || this.activity == null || this.isDetached || !this.isAdded || this.view == null) {
                return@addListener
            }
            hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            cameraProvider.unbindAll()
            val preview: Preview = Preview.Builder()
                .build()
            imageCapture = ImageCapture.Builder()
                .build()
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(720, 1280))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            /*val analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer {
                imageAnalysis.clearAnalyzer()
                cameraProvider.unbindAll()
                Util.logTag(it)
            }
            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)*/
            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            val camera = cameraProvider.bindToLifecycle(
                this,
                lensFacing,
                imageAnalysis,
                preview,
                imageCapture
            )
            if (camera.cameraInfo.hasFlashUnit()) {
                binding.toolbarContainer.flashButton.visibility = View.VISIBLE
                binding.toolbarContainer.flashButton.setOnClickListener {
                    camera.cameraControl.enableTorch(!flashEnabled)
                }
                camera.cameraInfo.torchState.observe(viewLifecycleOwner) {
                    it?.let { torchState ->
                        flashEnabled = torchState == TorchState.ON
                        binding.toolbarContainer.flashButton.let { imageView ->
                            imageView.isSelected = torchState == TorchState.ON
                        }
                    }
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val newContext = context ?: return
        val name = SimpleDateFormat("dd/M/yyyy hh:mm:ss", Locale.ENGLISH)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }
        val photoFile = createFile(getOutputDirectory(newContext), FILENAME, PHOTO_EXTENSION)
        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = lensFacing == CameraSelector.DEFAULT_FRONT_CAMERA
        }

        // Create output options object which contains file + metadata
        val outputOptions2 = ImageCapture.OutputFileOptions.Builder(photoFile)
            .setMetadata(metadata)
            .build()
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                newContext.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()
        imageCapture?.takePicture(
            outputOptions2,
            ContextCompat.getMainExecutor(newContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    if (context != null)
                        Toast.makeText(
                            context,
                            "Photo capture failed: ${exc.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    binding.cvCamera.isEnabled = true
                    binding.cvCamera.frame = 11
                    binding.cvCamera.pauseAnimation()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    val bundle = bundleOf("imgUri" to savedUri.toString())
                    binding.cvCamera.isEnabled = true
                    findNavController().navigate(
                        R.id.action_go_to_captured_image_preview_fragment,
                        bundle
                    )
                    binding.cvCamera.frame = 11
                    binding.cvCamera.pauseAnimation()

                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.cvCamera.pauseAnimation()
        binding.cvCamera.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
            }

            override fun onAnimationEnd(p0: Animator?) {
                binding.cvCamera.pauseAnimation()
                binding.cvCamera.setMinAndMaxFrame(24, 50)
                binding.cvCamera.resumeAnimation()

            }

            override fun onAnimationCancel(p0: Animator?) {
            }

            override fun onAnimationRepeat(p0: Animator?) {
            }

        })
        binding.cvCamera.setOnClickListener {
            binding.cvCamera.frame = 12
            binding.cvCamera.setMaxFrame(50)
            binding.cvCamera.resumeAnimation()
            binding.cvCamera.isEnabled = false
            takePhoto()
        }
        binding.cvCameraSwitch.setOnClickListener {
            flipCamera()
        }
        binding.toolbarContainer.closeButton.setOnClickListener {
            dialog?.dismiss()
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        PermissionX.init(this)
            .permissions(Manifest.permission.CAMERA)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Core fundamental are based on these permissions",
                    "OK",
                    "Cancel"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow necessary permissions in Settings manually to continue",
                    "OK",
                    "Exit"
                )
            }
            .request { allGranted, _, _ ->
                if (allGranted) {
                    startCamera()
                } else {
                    findNavController().navigateUp()
                    context?.toastS(getString(R.string.please_grant_permission_to_continue))
                }
            }
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext())
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

    companion object {
        private const val TAG = "EfficientCamera"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalCacheDir?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}