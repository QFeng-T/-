package com.tianhu.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var shareButton: Button
    private lateinit var favoriteButton: LinearLayout
    private lateinit var favoriteIcon: ImageView
    private lateinit var favoriteText: TextView
    private lateinit var recognizeAgainButton: Button

    private var recordId: Long? = null
    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 获取记录 ID
        recordId = intent.getLongExtra("record_id", -1L).let {
            if (it == -1L) null else it
        }

        // 初始化视图
        backButton = findViewById(R.id.backButton)
        shareButton = findViewById(R.id.shareButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        favoriteIcon = findViewById(R.id.favoriteIcon)
        favoriteText = findViewById(R.id.favoriteText)
        recognizeAgainButton = findViewById(R.id.recognizeAgainButton)

        // 设置点击事件
        backButton.setOnClickListener {
            finish()
        }

        shareButton.setOnClickListener {
            // 分享功能
        }

        favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        recognizeAgainButton.setOnClickListener {
            // 跳转到图像识别页面
            val intent = Intent(this, ImageUploadActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 加载收藏状态
        loadFavoriteStatus()
    }

    private fun loadFavoriteStatus() {
        // 暂时使用模拟的收藏状态，先演示 UI 效果
        // TODO: 等数据库集成后，从数据库查询真实的收藏状态
        updateFavoriteButton(false)
    }

    private fun toggleFavorite() {
        // 立即更新 UI，提供即时反馈（先演示效果，等有真实数据后再关联数据库）
        isFavorite = !isFavorite
        updateFavoriteButton(isFavorite)
        
        // 如果有记录 ID，后台执行数据库操作
        lifecycleScope.launch {
            recordId?.let { id ->
                RecognitionRecordService.toggleCollection(this@ResultActivity, id)
            }
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        this.isFavorite = isFavorite
        
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.ic_star_filled)
            favoriteText.text = "已收藏"
            favoriteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            favoriteIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
            favoriteText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_star)
            favoriteText.text = "收藏"
            favoriteButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            favoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
            favoriteText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
        
        favoriteIcon.requestLayout()
        favoriteText.requestLayout()
        favoriteButton.requestLayout()
    }

}