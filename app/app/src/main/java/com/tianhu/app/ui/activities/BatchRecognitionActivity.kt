package com.tianhu.app.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tianhu.app.service.RecognitionRecordService
import com.tianhu.app.util.ImageUtil
import com.tianhu.app.model.YOLOModelManager
import com.tianhu.app.model.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BatchRecognitionActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var addImagesButton: Button
    private lateinit var startRecognitionButton: Button
    private lateinit var imageRecyclerView: RecyclerView
    private lateinit var progressText: TextView
    private lateinit var progressBar: ProgressBar

    private val selectedImages = mutableListOf&lt;BatchImageItem&gt;()
    private lateinit var adapter: BatchImageAdapter
    private var isRecognizing = false

    private val galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted -&gt;
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "需要存储权限才能访问相册", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris -&gt;
        if (uris.isNotEmpty()) {
            addImages(uris)
        }
    }

    data class BatchImageItem(
        val uri: Uri,
        var status: ImageStatus = ImageStatus.PENDING,
        var result: String? = null,
        var bitmap: Bitmap? = null
    )

    enum class ImageStatus {
        PENDING,
        RECOGNIZING,
        SUCCESS,
        FAILED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_batch_recognition)

        initViews()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        addImagesButton = findViewById(R.id.addImagesButton)
        startRecognitionButton = findViewById(R.id.startRecognitionButton)
        imageRecyclerView = findViewById(R.id.imageRecyclerView)
        progressText = findViewById(R.id.progressText)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupRecyclerView() {
        adapter = BatchImageAdapter(selectedImages) { position -&gt;
            removeImage(position)
        }
        imageRecyclerView.layoutManager = LinearLayoutManager(this)
        imageRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        addImagesButton.setOnClickListener {
            checkGalleryPermissionAndOpen()
        }

        startRecognitionButton.setOnClickListener {
            if (!isRecognizing &amp;&amp; selectedImages.isNotEmpty()) {
                startBatchRecognition()
            }
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

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun addImages(uris: List&lt;Uri&gt;) {
        lifecycleScope.launch {
            uris.forEach { uri -&gt;
                if (!selectedImages.any { it.uri == uri }) {
                    val bitmap = withContext(Dispatchers.IO) {
                        try {
                            MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    selectedImages.add(BatchImageItem(uri, bitmap = bitmap))
                }
            }
            adapter.notifyDataSetChanged()
            updateUI()
        }
    }

    private fun removeImage(position: Int) {
        selectedImages.removeAt(position)
        adapter.notifyItemRemoved(position)
        updateUI()
    }

    private fun updateUI() {
        val pendingCount = selectedImages.count { it.status == ImageStatus.PENDING }
        val successCount = selectedImages.count { it.status == ImageStatus.SUCCESS }
        val failedCount = selectedImages.count { it.status == ImageStatus.FAILED }

        progressText.text = when {
            isRecognizing -&gt; "识别中: $successCount/${selectedImages.size} 成功, $failedCount 失败"
            selectedImages.isEmpty() -&gt; "已选择 0 张图片"
            else -&gt; "已选择 ${selectedImages.size} 张图片"
        }

        startRecognitionButton.isEnabled = !isRecognizing &amp;&amp; selectedImages.any { it.status == ImageStatus.PENDING }
        startRecognitionButton.text = if (isRecognizing) "识别中..." else "开始识别"
    }

    private fun startBatchRecognition() {
        isRecognizing = true
        progressBar.visibility = android.view.View.VISIBLE
        updateUI()

        lifecycleScope.launch {
            try {
                val modelLoaded = YOLOModelManager.loadModel(this@BatchRecognitionActivity, ModelType.FRUIT_DETECTION)
                if (!modelLoaded) {
                    Toast.makeText(this@BatchRecognitionActivity, "模型加载失败", Toast.LENGTH_SHORT).show()
                    isRecognizing = false
                    progressBar.visibility = android.view.View.GONE
                    updateUI()
                    return@launch
                }

                selectedImages.forEachIndexed { index, item -&gt;
                    if (item.status == ImageStatus.PENDING) {
                        item.status = ImageStatus.RECOGNIZING
                        adapter.notifyItemChanged(index)
                        updateUI()

                        try {
                            val bitmap = item.bitmap ?: withContext(Dispatchers.IO) {
                                MediaStore.Images.Media.getBitmap(contentResolver, item.uri)
                            }

                            val compressedBitmap = withContext(Dispatchers.IO) {
                                ImageUtil.compressImage(bitmap, 800, 800, 80)
                            }

                            val result = YOLOModelManager.recognizeImage(
                                this@BatchRecognitionActivity,
                                compressedBitmap ?: bitmap,
                                ModelType.FRUIT_DETECTION
                            )

                            val nutritionData = YOLOModelManager.getNutritionData(result.className)

                            RecognitionRecordService.saveRecognitionRecord(
                                context = this@BatchRecognitionActivity,
                                fruitVegName = result.className,
                                confidence = result.confidence,
                                imageBitmap = compressedBitmap ?: bitmap,
                                nutritionData = nutritionData,
                                recognitionType = com.tianhu.app.database.enums.RecognitionType.FRUIT_DETECTION
                            )

                            item.status = ImageStatus.SUCCESS
                            item.result = "${result.className} (${String.format("%.1f%%", result.confidence * 100)})"
                        } catch (e: Exception) {
                            item.status = ImageStatus.FAILED
                            item.result = "识别失败"
                        }

                        adapter.notifyItemChanged(index)
                        updateUI()
                    }
                }

                Toast.makeText(this@BatchRecognitionActivity, "批量识别完成", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@BatchRecognitionActivity, "批量识别失败: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isRecognizing = false
                progressBar.visibility = android.view.View.GONE
                updateUI()
            }
        }
    }

    inner class BatchImageAdapter(
        private val items: List&lt;BatchImageItem&gt;,
        private val onRemoveClick: (Int) -&gt; Unit
    ) : RecyclerView.Adapter&lt;BatchImageAdapter.ViewHolder&gt;() {

        inner class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val imageView: android.widget.ImageView = view.findViewById(R.id.imageView)
            val statusText: TextView = view.findViewById(R.id.statusText)
            val resultText: TextView = view.findViewById(R.id.resultText)
            val removeButton: Button = view.findViewById(R.id.removeButton)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_batch_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]

            try {
                holder.imageView.setImageURI(item.uri)
            } catch (e: Exception) {
                holder.imageView.setImageResource(R.drawable.resource__apple)
            }

            holder.statusText.text = when (item.status) {
                ImageStatus.PENDING -&gt; "待识别"
                ImageStatus.RECOGNIZING -&gt; "识别中..."
                ImageStatus.SUCCESS -&gt; "识别成功"
                ImageStatus.FAILED -&gt; "识别失败"
            }

            holder.statusText.setTextColor(
                when (item.status) {
                    ImageStatus.SUCCESS -&gt; ContextCompat.getColor(this@BatchRecognitionActivity, R.color.primary)
                    ImageStatus.FAILED -&gt; ContextCompat.getColor(this@BatchRecognitionActivity, android.R.color.holo_red_dark)
                    else -&gt; ContextCompat.getColor(this@BatchRecognitionActivity, android.R.color.black)
                }
            )

            holder.resultText.text = item.result ?: ""
            holder.removeButton.visibility = if (isRecognizing) android.view.View.GONE else android.view.View.VISIBLE
            holder.removeButton.setOnClickListener { onRemoveClick(position) }
        }

        override fun getItemCount(): Int = items.size
    }
}
