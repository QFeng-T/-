package com.tianhu.app.ui.activities

import android.content.Context
import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.tianhu.app.model.YOLOModelManager
import com.tianhu.app.model.ModelType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ModelManagerActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var fruitModelCard: CardView
    private lateinit var fruitModelStatus: TextView
    private lateinit var fruitModelSize: TextView
    private lateinit var fruitModelDownloadBtn: CardView
    private lateinit var fruitModelDeleteBtn: CardView
    private lateinit var fruitModelProgress: ProgressBar
    
    private lateinit var freshnessModelCard: CardView
    private lateinit var freshnessModelStatus: TextView
    private lateinit var freshnessModelSize: TextView
    private lateinit var freshnessModelDownloadBtn: CardView
    private lateinit var freshnessModelDeleteBtn: CardView
    private lateinit var freshnessModelProgress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_manager)

        initViews()
        setupClickListeners()
        updateModelStatus()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        
        fruitModelCard = findViewById(R.id.fruitModelCard)
        fruitModelStatus = findViewById(R.id.fruitModelStatus)
        fruitModelSize = findViewById(R.id.fruitModelSize)
        fruitModelDownloadBtn = findViewById(R.id.fruitModelDownloadBtn)
        fruitModelDeleteBtn = findViewById(R.id.fruitModelDeleteBtn)
        fruitModelProgress = findViewById(R.id.fruitModelProgress)
        
        freshnessModelCard = findViewById(R.id.freshnessModelCard)
        freshnessModelStatus = findViewById(R.id.freshnessModelStatus)
        freshnessModelSize = findViewById(R.id.freshnessModelSize)
        freshnessModelDownloadBtn = findViewById(R.id.freshnessModelDownloadBtn)
        freshnessModelDeleteBtn = findViewById(R.id.freshnessModelDeleteBtn)
        freshnessModelProgress = findViewById(R.id.freshnessModelProgress)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        fruitModelDownloadBtn.setOnClickListener {
            downloadModel(ModelType.FRUIT_DETECTION)
        }

        fruitModelDeleteBtn.setOnClickListener {
            deleteModel(ModelType.FRUIT_DETECTION)
        }

        freshnessModelDownloadBtn.setOnClickListener {
            downloadModel(ModelType.FRESHNESS_DETECTION)
        }

        freshnessModelDeleteBtn.setOnClickListener {
            deleteModel(ModelType.FRESHNESS_DETECTION)
        }
    }

    private fun updateModelStatus() {
        val fruitModelLoaded = YOLOModelManager.isModelLoaded(ModelType.FRUIT_DETECTION)
        val fruitModelFile = YOLOModelManager.getModelFile(this, ModelType.FRUIT_DETECTION)
        
        if (fruitModelLoaded || fruitModelFile != null) {
            fruitModelStatus.text = "已安装"
            fruitModelStatus.setTextColor(getColor(R.color.primary))
            fruitModelDeleteBtn.visibility = CardView.VISIBLE
            
            fruitModelFile?.let {
                val sizeMB = it.length() / (1024 * 1024.0)
                fruitModelSize.text = String.format("%.2f MB", sizeMB)
            }
        } else {
            fruitModelStatus.text = "未安装"
            fruitModelStatus.setTextColor(getColor(R.color.text_tertiary))
            fruitModelDeleteBtn.visibility = CardView.GONE
            fruitModelSize.text = ""
        }

        val freshnessModelLoaded = YOLOModelManager.isModelLoaded(ModelType.FRESHNESS_DETECTION)
        val freshnessModelFile = YOLOModelManager.getModelFile(this, ModelType.FRESHNESS_DETECTION)
        
        if (freshnessModelLoaded || freshnessModelFile != null) {
            freshnessModelStatus.text = "已安装"
            freshnessModelStatus.setTextColor(getColor(R.color.primary))
            freshnessModelDeleteBtn.visibility = CardView.VISIBLE
            
            freshnessModelFile?.let {
                val sizeMB = it.length() / (1024 * 1024.0)
                freshnessModelSize.text = String.format("%.2f MB", sizeMB)
            }
        } else {
            freshnessModelStatus.text = "未安装"
            freshnessModelStatus.setTextColor(getColor(R.color.text_tertiary))
            freshnessModelDeleteBtn.visibility = CardView.GONE
            freshnessModelSize.text = ""
        }
    }

    private fun downloadModel(modelType: ModelType) {
        val (progressBar, statusText, downloadBtn, deleteBtn) = when (modelType) {
            ModelType.FRUIT_DETECTION -&gt; 
                Quadruple(fruitModelProgress, fruitModelStatus, fruitModelDownloadBtn, fruitModelDeleteBtn)
            ModelType.FRESHNESS_DETECTION -&gt; 
                Quadruple(freshnessModelProgress, freshnessModelStatus, freshnessModelDownloadBtn, freshnessModelDeleteBtn)
        }

        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "正在下载..."
        downloadBtn.isClickable = false
        deleteBtn.isClickable = false

        lifecycleScope.launch {
            try {
                val downloadUrl = getModelDownloadUrl(modelType)

                if (downloadUrl != null) {
                    downloadModelFromUrl(modelType, downloadUrl, progressBar, statusText, downloadBtn, deleteBtn)
                } else {
                    simulateDownload(modelType, progressBar, statusText, downloadBtn, deleteBtn)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.GONE
                    statusText.text = "下载失败"
                    downloadBtn.isClickable = true
                    deleteBtn.isClickable = true
                    Toast.makeText(this@ModelManagerActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getModelDownloadUrl(modelType: ModelType): String? {
        return null
    }

    private suspend fun downloadModelFromUrl(
        modelType: ModelType,
        url: String,
        progressBar: ProgressBar,
        statusText: TextView,
        downloadBtn: CardView,
        deleteBtn: CardView
    ) {
        withContext(Dispatchers.Main) {
            statusText.text = "正在下载..."
        }

        val success = YOLOModelManager.loadModel(this@ModelManagerActivity, modelType)

        withContext(Dispatchers.Main) {
            progressBar.visibility = ProgressBar.GONE
            downloadBtn.isClickable = true
            deleteBtn.isClickable = true

            if (success) {
                Toast.makeText(this@ModelManagerActivity, "模型下载成功", Toast.LENGTH_SHORT).show()
                updateModelStatus()
            } else {
                statusText.text = "下载失败"
                Toast.makeText(this@ModelManagerActivity, "模型下载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun simulateDownload(
        modelType: ModelType,
        progressBar: ProgressBar,
        statusText: TextView,
        downloadBtn: CardView,
        deleteBtn: CardView
    ) {
        for (progress in 0..100 step 10) {
            withContext(Dispatchers.Main) {
                progressBar.progress = progress
            }
            kotlinx.coroutines.delay(100)
        }

        val success = YOLOModelManager.loadModel(this@ModelManagerActivity, modelType)

        withContext(Dispatchers.Main) {
            progressBar.visibility = ProgressBar.GONE
            downloadBtn.isClickable = true
            deleteBtn.isClickable = true

            if (success) {
                Toast.makeText(this@ModelManagerActivity, "模型下载成功", Toast.LENGTH_SHORT).show()
                updateModelStatus()
            } else {
                statusText.text = "下载失败"
                Toast.makeText(this@ModelManagerActivity, "模型下载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteModel(modelType: ModelType) {
        val modelName = when (modelType) {
            ModelType.FRUIT_DETECTION -&gt; "果蔬检测模型"
            ModelType.FRESHNESS_DETECTION -&gt; "新鲜度检测模型"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除${modelName}吗？")
            .setPositiveButton("删除") { _, _ -&gt;
                performDeleteModel(modelType)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performDeleteModel(modelType: ModelType) {
        lifecycleScope.launch {
            val success = YOLOModelManager.deleteModel(this@ModelManagerActivity, modelType)
            
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(this@ModelManagerActivity, "模型删除成功", Toast.LENGTH_SHORT).show()
                    updateModelStatus()
                } else {
                    Toast.makeText(this@ModelManagerActivity, "模型删除失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    data class Quadruple&lt;A, B, C, D&gt;(val first: A, val second: B, val third: C, val fourth: D)
}
