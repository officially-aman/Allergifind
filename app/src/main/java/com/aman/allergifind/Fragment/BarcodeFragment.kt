package com.aman.allergifind.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import com.aman.allergifind.R
import com.bumptech.glide.Glide
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.bumptech.glide.request.RequestOptions
import com.aman.allergifind.databinding.FragmentBarcodeBinding // If using ViewBinding

class BarcodeFragment : Fragment() {

    private lateinit var barcodeText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: androidx.camera.view.PreviewView

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }

    @SuppressLint("MissingSuperCall")
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        // Change status bar color to a light color (e.g., white or light gray)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requireActivity().window.apply {
                // Set light status bar icons (dark icons on a light background)
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                // Inside your fragment or activity
                requireActivity().window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.dark)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_barcode, container, false)

        barcodeText = rootView.findViewById(R.id.barcodeText)
        viewFinder = rootView.findViewById(R.id.viewFinder)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check camera permission
        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        return rootView
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()


            val preview = Preview.Builder()
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .build()

// ImageAnalysis for Barcode Scanning
            imageAnalysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer({ barcode ->
                requireActivity().runOnUiThread {
                    // Call the API fetch function with the scanned barcode
                    fetchProductDetailsFromAPI(barcode)
                }
            }, requireContext())) // Pass the context here



            // Set up the camera selector to use the back camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                // Unbind any previously bound use cases
                cameraProvider.unbindAll()

                // Bind the preview and image analysis use cases
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )

                // Set up the preview surface provider (PreviewView)
                preview.setSurfaceProvider(viewFinder.surfaceProvider)

            } catch (e: Exception) {
                Log.e("BarcodeFragment", "Error binding camera use cases", e)
                Toast.makeText(requireContext(), "Camera binding failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }


    private fun fetchProductDetailsFromAPI(barcode: String?) {
        if (barcode.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Invalid barcode value", Toast.LENGTH_SHORT).show()
            return
        }

        val apiUrl = "https://world.openfoodfacts.org/api/v0/product/$barcode.json"

        // Use OkHttp to query the API
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(apiUrl)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "API request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val jsonData = responseBody.string()
                        val jsonObject = JSONObject(jsonData)
                        val product = jsonObject.optJSONObject("product")

                        requireActivity().runOnUiThread {
                            // Display details in the TextView
                            barcodeText.text = buildString {
                                append("Barcode Number: $barcode\n\n")
                                append("Name: ${product?.optString("product_name") ?: "N/A"}\n")
                                append("Price: ${product?.optString("price") ?: "N/A"}\n")
                                append("Weight: ${product?.optString("quantity") ?: "N/A"}\n")
                                append("Manufacture Date: ${product?.optString("manufacture_date") ?: "N/A"}\n")
                                append("Expiry Date: ${product?.optString("expiration_date") ?: "N/A"}\n")
                                append("Manufacturer: ${product?.optString("brands") ?: "N/A"}\n\n")
                                append("Ingredients:\n")

                                // Fetch and display ingredients
                                val ingredients = product?.optJSONArray("ingredients")
                                if (ingredients != null) {
                                    for (i in 0 until ingredients.length()) {
                                        val ingredient = ingredients.optJSONObject(i)
                                        val ingredientText = ingredient?.optString("text") ?: "Unknown Ingredient"
                                        append("- $ingredientText\n")
                                    }
                                } else {
                                    append("Ingredients Not Found.\n")
                                }
                            }
                        }


                    }

                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to fetch product details", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


     override fun onDestroyView() {
            super.onDestroyView()
            cameraExecutor.shutdown()
        }

        class BarcodeAnalyzer(
            private val onBarcodeScanned: (String) -> Unit,
            private val context: Context // Add context to access system services
        ) : ImageAnalysis.Analyzer {

            private val scanner = BarcodeScanning.getClient()

            @SuppressLint("UnsafeOptInUsageError")
            @androidx.camera.core.ExperimentalGetImage
            @OptIn(ExperimentalGetImage::class)
            override fun analyze(imageProxy: ImageProxy) {
                val mediaImage = imageProxy.image
                if (mediaImage != null) {
                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty()) {
                                val barcode = barcodes.first().displayValue
                                if (barcode != null) {

    //                                playBeepSound() // Add this for audio feedback
                                    onBarcodeScanned(barcode)

                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("BarcodeAnalyzer", "Barcode analysis error", e)
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
            }

            private fun playBeepSound() {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200) // 200ms beep
            }

            private fun vibrateDevice() {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    // Vibrate for 500 milliseconds
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    // For devices below API 26, use the deprecated vibrate method
                    vibrator.vibrate(500)
                }
            }
        }
    }
