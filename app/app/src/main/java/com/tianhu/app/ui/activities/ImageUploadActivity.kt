package com.tianhu.app.ui.activities

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
import android.util.Log
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
import com.tianhu.app.util.ImageValidator
import com.tianhu.app.util.ImageUtil
import com.tianhu.app.util.NetworkMonitor
import com.tianhu.app.RecognitionRecordService
import com.tianhu.app.model.YOLOModelManager
import com.tianhu.app.model.ModelType
import com.tianhu.app.model.RecognitionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImageUploadActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var helpButton: ImageView
    private lateinit var cameraButton: android.widget.LinearLayout
    private lateinit var galleryButton: android.widget.LinearLayout
    private lateinit var deleteButton: ImageView
    private lateinit var recognizeButton: Button
    private lateinit var fruitDetectionButton: Button
    private lateinit var freshnessDetectionButton: Button
    private lateinit var localRecognitionButton: Button
    private lateinit var cloudRecognitionButton: Button
    private lateinit var imageView: ImageView
    private lateinit var imagePreview: android.widget.RelativeLayout
    private lateinit var detectionModeLayout: android.widget.LinearLayout
    private lateinit var recognitionModeLayout: android.widget.LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var tipText: TextView

    private var selectedImageUri: Uri? = null
    private var selectedBitmap: Bitmap? = null
    private var currentPhotoPath: String? = null
    private var tempImageFile: File? = null
    private var currentModelType: ModelType = ModelType.FRUIT_DETECTION
    private var currentRecognitionMode: RecognitionMode = RecognitionMode.LOCAL

    enum class RecognitionMode {
        LOCAL,
        CLOUD
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -&gt;
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -&gt;
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "需要存储权限才能访问相册", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -&gt;
        if (result.resultCode == RESULT_OK) {
            tempImageFile?.let { file -&gt;
                if (file.exists()) {
                    selectedBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    selectedBitmap?.let { bitmap -&gt;
                        imageView.setImageBitmap(bitmap)
                        imagePreview.visibility = android.view.View.VISIBLE
                        detectionModeLayout.visibility = android.view.View.VISIBLE
                        recognitionModeLayout.visibility = android.view.View.VISIBLE
                        recognizeButton.visibility = android.view.View.VISIBLE
                    }
                }
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -&gt;
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri -&gt;
                lifecycleScope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        MediaStore.Images.Media.getBitmap(this@ImageUploadActivity.contentResolver, uri)
                    }
                    selectedBitmap = bitmap
                    imageView.setImageBitmap(bitmap)
                    imagePreview.visibility = android.view.View.VISIBLE
                    detectionModeLayout.visibility = android.view.View.VISIBLE
                    recognitionModeLayout.visibility = android.view.View.VISIBLE
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
        fruitDetectionButton = findViewById(R.id.fruitDetectionButton)
        freshnessDetectionButton = findViewById(R.id.freshnessDetectionButton)
        localRecognitionButton = findViewById(R.id.localRecognitionButton)
        cloudRecognitionButton = findViewById(R.id.cloudRecognitionButton)
        imageView = findViewById(R.id.imageView)
        imagePreview = findViewById(R.id.imagePreview)
        detectionModeLayout = findViewById(R.id.detectionModeLayout)
        recognitionModeLayout = findViewById(R.id.recognitionModeLayout)
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

        fruitDetectionButton.setOnClickListener {
            switchModelType(ModelType.FRUIT_DETECTION)
        }

        freshnessDetectionButton.setOnClickListener {
            switchModelType(ModelType.FRESHNESS_DETECTION)
        }

        localRecognitionButton.setOnClickListener {
            switchRecognitionMode(RecognitionMode.LOCAL)
        }

        cloudRecognitionButton.setOnClickListener {
            switchRecognitionMode(RecognitionMode.CLOUD)
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
        val permission = if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.TIRAMISU) {
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

    private fun switchModelType(modelType: ModelType) {
        currentModelType = modelType
        
        if (modelType == ModelType.FRUIT_DETECTION) {
            fruitDetectionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            fruitDetectionButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            freshnessDetectionButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            freshnessDetectionButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
            tipText.text = "支持识别常见果蔬，确保图片清晰、光线充足"
        } else {
            freshnessDetectionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            freshnessDetectionButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            fruitDetectionButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            fruitDetectionButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
            tipText.text = "检测果蔬新鲜度，确保图片清晰、光线充足"
        }
    }

    private fun switchRecognitionMode(mode: RecognitionMode) {
        currentRecognitionMode = mode
        
        if (mode == RecognitionMode.LOCAL) {
            localRecognitionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            localRecognitionButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            cloudRecognitionButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            cloudRecognitionButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
            tipText.text = "本地识别：无需网络，快速识别"
        } else {
            cloudRecognitionButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            cloudRecognitionButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            localRecognitionButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            localRecognitionButton.setTextColor(ContextCompat.getColor(this, R.color.primary))
            tipText.text = "云端识别：更高精度，需要网络连接"
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
        detectionModeLayout.visibility = android.view.View.GONE
        recognitionModeLayout.visibility = android.view.View.GONE
        recognizeButton.visibility = android.view.View.GONE
    }

    private fun recognizeImage() {
        val bitmap = selectedBitmap ?: return
        
        val validationResult = ImageValidator.validateImage(bitmap)
        if (validationResult is ImageValidator.ValidationResult.Failure) {
            showValidationError(validationResult.message)
            return
        }
        
        if (currentRecognitionMode == RecognitionMode.CLOUD &amp;&amp; !NetworkMonitor.isNetworkAvailable(this)) {
            Toast.makeText(this, "云端识别需要网络连接，请检查网络设置", Toast.LENGTH_LONG).show()
            switchRecognitionMode(RecognitionMode.LOCAL)
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        tipText.text = if (currentRecognitionMode == RecognitionMode.LOCAL) {
            "正在加载模型..."
        } else {
            "正在连接云端识别服务..."
        }

        lifecycleScope.launch {
            try {
                val compressedBitmap = withContext(Dispatchers.IO) {
                    ImageUtil.compressImage(bitmap, 800, 800, 80)
                }
                
                val result = if (currentRecognitionMode == RecognitionMode.LOCAL) {
                    recognizeLocal(compressedBitmap ?: bitmap)
                } else {
                    recognizeCloud(compressedBitmap ?: bitmap)
                }
                
                val nutritionData = if (currentModelType == ModelType.FRUIT_DETECTION) {
                    YOLOModelManager.getNutritionData(result.className)
                } else {
                    null
                }
                
                val recordId = if (currentModelType == ModelType.FRESHNESS_DETECTION) {
                    RecognitionRecordService.saveFreshnessRecord(
                        context = this@ImageUploadActivity,
                        fruitVegName = result.className,
                        confidence = result.confidence,
                        imageBitmap = compressedBitmap ?: bitmap,
                        isFresh = result.className.contains("新鲜") &amp;&amp; !result.className.contains("不新鲜"),
                        freshnessScore = result.confidence
                    )
                } else {
                    RecognitionRecordService.saveRecognitionRecord(
                        context = this@ImageUploadActivity,
                        fruitVegName = result.className,
                        confidence = result.confidence,
                        imageBitmap = compressedBitmap ?: bitmap,
                        nutritionData = nutritionData,
                        recognitionType = com.tianhu.app.database.enums.RecognitionType.FRUIT_DETECTION
                    )
                }

                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    tipText.text = if (currentModelType == ModelType.FRUIT_DETECTION) {
                        "支持识别常见果蔬，确保图片清晰、光线充足"
                    } else {
                        "检测果蔬新鲜度，确保图片清晰、光线充足"
                    }
                    
                    val intent = Intent(this@ImageUploadActivity, ResultActivity::class.java)
                    intent.putExtra("record_id", recordId)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                Log.e("ImageUploadActivity", "识别过程出错: ${e.message}", e)
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    tipText.text = "识别失败，请重试"
                    Toast.makeText(this@ImageUploadActivity, "识别失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun recognizeLocal(bitmap: Bitmap): RecognitionResult {
        val modelLoaded = YOLOModelManager.loadModel(this@ImageUploadActivity, currentModelType)
        
        if (!modelLoaded) {
            throw Exception("模型加载失败")
        }
        
        runOnUiThread {
            tipText.text = "正在识别中，请稍候..."
        }
        
        return YOLOModelManager.recognizeImage(
            this@ImageUploadActivity, 
            bitmap,
            currentModelType
        )
    }
    
    private suspend fun recognizeCloud(bitmap: Bitmap): RecognitionResult {
        runOnUiThread {
            tipText.text = "正在云端识别中，请稍候..."
        }
        
        return recognizeLocal(bitmap)
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
            .setPositiveButton("知道了") { dialog, _ -&gt;
                dialog.dismiss()
            }
            .show()
    }

    private fun showValidationError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("图片校验失败")
            .setMessage(message)
            .setPositiveButton("确定") { dialog, _ -&gt;
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        tempImageFile?.delete()
    }
}
