package com.tianhu.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

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

    private var selectedImageUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(bitmap)
            imagePreview.visibility = android.view.View.VISIBLE
            recognizeButton.visibility = android.view.View.VISIBLE
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            imageView.setImageURI(selectedImageUri)
            imagePreview.visibility = android.view.View.VISIBLE
            recognizeButton.visibility = android.view.View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_upload)

        // 初始化视图
        backButton = findViewById(R.id.backButton)
        helpButton = findViewById(R.id.helpButton)
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        deleteButton = findViewById(R.id.deleteButton)
        recognizeButton = findViewById(R.id.recognizeButton)
        imageView = findViewById(R.id.imageView)
        imagePreview = findViewById(R.id.imagePreview)
        progressBar = findViewById(R.id.progressBar)

        // 设置点击事件
        backButton.setOnClickListener {
            finish()
        }

        helpButton.setOnClickListener {
            // 显示帮助信息
        }

        cameraButton.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(intent)
        }

        galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }

        deleteButton.setOnClickListener {
            // 清空图片
            selectedImageUri = null
            imageView.setImageURI(null)
            imagePreview.visibility = android.view.View.GONE
            recognizeButton.visibility = android.view.View.GONE
        }

        recognizeButton.setOnClickListener {
            if (selectedImageUri != null) {
                recognizeImage()
            }
        }
    }

    private fun recognizeImage() {
        // 显示加载动画
        progressBar.visibility = android.view.View.VISIBLE

        // 模拟识别过程
        Thread {
            Thread.sleep(2000) // 模拟网络延迟
            runOnUiThread {
                progressBar.visibility = android.view.View.GONE
                // 跳转到结果页面
                val intent = Intent(this, ResultActivity::class.java)
                startActivity(intent)
            }
        }.start()
    }

}