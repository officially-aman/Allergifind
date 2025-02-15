package com.aman.allergifind.Fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aman.allergifind.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color

class HomeFragment : Fragment() {

    private lateinit var progressDialog: ProgressDialog
    private lateinit var imageUri: Uri
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    private lateinit var ingredientsTable: TableLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startCropping(imageUri)
            } else {
                Toast.makeText(requireContext(), "Camera operation canceled", Toast.LENGTH_SHORT).show()
            }
        }

        // Initialize crop launcher
        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                if (resultUri != null) {
                    processCroppedImage(resultUri)
                } else {
                    Toast.makeText(requireContext(), "Error: Cropped image URI is null", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(requireContext(), "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Change status bar color
        requireActivity().window.apply {
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.dark)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize UI elements
        ingredientsTable = view.findViewById(R.id.ingredientsTable)
        val captureButton = view.findViewById<Button>(R.id.captureButton)

        progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Processing image...")
            setCancelable(false)
        }

        captureButton.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        return view
    }

    private fun openCamera() {
        val photoFile = createImageFile()
        imageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.provider", photoFile
        )
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        }

        cameraLauncher.launch(cameraIntent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun startCropping(sourceUri: Uri) {
        val destinationUri = Uri.fromFile(File(requireContext().cacheDir, "cropped_image.jpg"))
        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(UCrop.Options().apply {
                setFreeStyleCropEnabled(true)
            })
            .getIntent(requireContext())

        cropLauncher.launch(cropIntent)
    }

    private fun processCroppedImage(uri: Uri) {
        val croppedBitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        Log.d("CroppedImage", "Width: ${croppedBitmap.width}, Height: ${croppedBitmap.height}")

        performOCR(croppedBitmap)
    }

    private fun performOCR(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        progressDialog.show()
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                progressDialog.dismiss()
                if (visionText.text.isNotEmpty()) {
                    populateTableWithText(visionText.text)
                } else {
                    Toast.makeText(requireContext(), "No text detected in the image", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(requireContext(), "Error recognizing text: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("OCR", "Error recognizing text", e)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun populateTableWithText(detectedText: String) {
        ingredientsTable.removeAllViews() // Clear previous rows

        // Add a header row
        val headerRow = TableRow(requireContext())
        val serialHeader = TextView(requireContext()).apply {
            text = "S.No" // Header for serial number
            textSize = 18f
            setPadding(16, 16, 16, 16)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        }
        val ingredientHeader = TextView(requireContext()).apply {
            text = "Ingredient" // Header for ingredient
            textSize = 18f
            setPadding(16, 16, 16, 16)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
        }
        headerRow.addView(serialHeader)
        headerRow.addView(ingredientHeader)
        ingredientsTable.addView(headerRow)

        // Split the detected text into ingredients
        val ingredients = detectedText.split(",").map { it.trim() }.filter { it.isNotBlank() }

        // Add each ingredient in a separate row
        ingredients.forEachIndexed { index, ingredient ->
            val row = TableRow(requireContext())
            val serialCell = TextView(requireContext()).apply {
                text = (index + 1).toString() // Serial number
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.border) // Styling
                setTextColor(Color.BLACK)
                setTextAlignment(View.TEXT_ALIGNMENT_CENTER)
            }
            val ingredientCell = TextView(requireContext()).apply {
                text = ingredient // Ingredient text
                textSize = 16f
                setPadding(16, 16, 16, 16)
                setBackgroundResource(R.drawable.border) // Styling
                setTextColor(Color.BLACK)
            }
            row.addView(serialCell)
            row.addView(ingredientCell)
            ingredientsTable.addView(row)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission is required to use this feature.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}
