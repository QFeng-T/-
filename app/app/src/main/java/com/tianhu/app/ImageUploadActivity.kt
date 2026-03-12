package com.tianhu.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageUploadActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var helpButton: Button
    private lateinit var cameraButton: Button
    private lateinit var galleryButton: Button
    private lateinit var deleteButton: Button
    private lateinit var recognizeButton: Button
    private lateinit var imageView: ImageView
    private lateinit var imagePreview: android.widget.RelativeLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tipText: TextView

    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var currentPhotoPath: String? = null
    private var tempImageFile: File? = null

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "需要存储权限才能访问相册", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            tempImageFile?.let { file ->
                if (file.exists()) {
                    selectedBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    selectedBitmap?.let { bitmap ->
                        imageView.setImageBitmap(bitmap)
                        imagePreview.visibility = android.view.View.VISIBLE
                        recognizeButton.visibility = android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                lifecycleScope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        MediaStore.Images.Media.getBitmap(this@ImageUploadActivity.contentResolver, uri)
                    }
                    selectedBitmap = bitmap
                    imageView.setImageBitmap(bitmap)
                    imagePreview.visibility = android.view.View.VISIBLE
                    recognizeButton.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_upload)

        initViews()
        setClickListeners()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        helpButton = findViewById(R.id.helpButton)
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        deleteButton = findViewById(R.id.deleteButton)
        recognizeButton = findViewById(R.id.recognizeButton)
        imageView = findViewById(R.id.imageView)
        imagePreview = findViewById(R.id.imagePreview)
        progressBar = findViewById(R.id.progressBar)
        tipText = findViewById(R.id.tipText)
    }

    private fun setClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        helpButton.setOnClickListener {
            showHelpDialog()
        }

        cameraButton.setOnClickListener {
            checkCameraPermissionAndOpen()
        }

        galleryButton.setOnClickListener {
            checkGalleryPermissionAndOpen()
        }

        deleteButton.setOnClickListener {
            clearImage()
        }

        recognizeButton.setOnClickListener {
            if (selectedBitmap != null) {
                recognizeImage()
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun checkGalleryPermissionAndOpen() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            galleryPermissionLauncher.launch(permission)
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            tempImageFile = photoFile
            val photoURI = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            }
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法启动相机: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun clearImage() {
        selectedImageUri = null
        selectedBitmap = null
        currentPhotoPath = null
        tempImageFile?.delete()
        tempImageFile = null
        imageView.setImageURI(null)
        imagePreview.visibility = android.view.View.GONE
        recognizeButton.visibility = android.view.View.GONE
    }

    private fun recognizeImage() {
        val bitmap = selectedBitmap ?: return
        
        val validationResult = ImageValidator.validateImage(bitmap)
        if (validationResult is ImageValidator.ValidationResult.Failure) {
            showValidationError(validationResult.message)
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        tipText.text = "正在识别中，请稍候..."

        lifecycleScope.launch {
            val compressedBitmap = withContext(Dispatchers.IO) {
                ImageUtil.compressImage(bitmap, 800, 800, 80)
            }

            Thread.sleep(1500)

            val mockFruitName = "番茄"
            val mockConfidence = 0.95f
            val mockNutritionData = """{
                "热量": "18kcal",
                "维生素C": "19mg",
                "维生素A": "833IU",
                "钾": "237mg",
                "膳食纤维": "1.2g",
                "蛋白质": "0.9g",
                "碳水化合物": "3.9g"
            }"""

            val recordId = RecognitionRecordService.saveRecognitionRecord(
                context = this@ImageUploadActivity,
                fruitVegName = mockFruitName,
                confidence = mockConfidence,
                imageBitmap = compressedBitmap ?: bitmap,
                nutritionData = mockNutritionData
            )

            runOnUiThread {
                progressBar.visibility = android.view.View.GONE
                tipText.text = "支持识别常见果蔬，确保图片清晰、光线充足"
                
                val intent = Intent(this@ImageUploadActivity, ResultActivity::class.java)
                intent.putExtra("record_id", recordId)
                startActivity(intent)
            }
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(this)
            .setTitle("使用帮助")
            .setMessage(
                """1. 拍照识别：使用相机拍摄果蔬图片进行识别
                |
                |2. 从相册选择：从手机相册中选择图片进行识别
                |
                |3. 识别建议：
                |   - 确保图片清晰、光线充足
                |   - 尽量让果蔬占据画面主要部分
                |   - 避免背景过于复杂
                |   - 单个果蔬识别效果更佳
                |
                |4. 目前支持常见的水果和蔬菜识别""".trimMargin()
            )
            .setPositiveButton("知道了") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showValidationError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("图片校验失败")
            .setMessage(message)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempImageFile?.delete()
    }
}
