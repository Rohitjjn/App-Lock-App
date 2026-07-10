package com.example.util

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IntruderCameraHelper {
    private const val TAG = "IntruderCameraHelper"

    private var imageCapture: ImageCapture? = null
    private var isCameraReady = false
    private var currentCameraProvider: ProcessCameraProvider? = null

    fun prepareCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        val hasCameraPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCameraPerm) {
            Log.e(TAG, "Camera permission not granted")
            return
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                currentCameraProvider = cameraProvider

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageCapture!!
                )
                isCameraReady = true
                Log.d(TAG, "Camera prepared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Camera preparation failed: ${e.message}", e)
                isCameraReady = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takeSelfieAndClose(context: Context) {
        if (!isCameraReady || imageCapture == null) {
            Log.e(TAG, "Camera not ready for fast capture")
            return
        }

        val storageDir = File(context.filesDir, "intruder_selfies")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val entryTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val selfieFile = File(storageDir, "IMG_$entryTime.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(selfieFile).build()

        imageCapture!!.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.d(TAG, "Background selfie saved successfully at: ${selfieFile.absolutePath}")
                    releaseCamera()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Background selfie save failed: ${exception.message}", exception)
                    releaseCamera()
                }
            }
        )
    }

    fun releaseCamera() {
        try {
            currentCameraProvider?.unbindAll()
            imageCapture = null
            isCameraReady = false
            currentCameraProvider = null
            Log.d(TAG, "Camera released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera", e)
        }
    }
    
    // Legacy support to just capture instantly (slow)
    fun captureSelfie(context: Context, lifecycleOwner: LifecycleOwner) {
        prepareCamera(context, lifecycleOwner)
        // Wait a bit to ensure it is bound before taking
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            takeSelfieAndClose(context)
        }, 1200)
    }
}
