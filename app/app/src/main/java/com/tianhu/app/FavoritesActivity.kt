package com.tianhu.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var goToScanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // 初始化视图
        backButton = findViewById(R.id.backButton)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        goToScanButton = findViewById(R.id.goToScanButton)

        // 设置点击事件
        backButton.setOnClickListener {
            finish()
        }

        goToScanButton.setOnClickListener {
            val intent = Intent(this, ImageUploadActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 初始化RecyclerView
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // 加载收藏记录
        loadFavorites()
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val records = RecognitionRecordService.getCollectedRecords(this@FavoritesActivity)
            
            if (records.isEmpty()) {
                emptyState.visibility = LinearLayout.VISIBLE
                favoritesRecyclerView.visibility = RecyclerView.GONE
            } else {
                emptyState.visibility = LinearLayout.GONE
                favoritesRecyclerView.visibility = RecyclerView.VISIBLE
                
                // TODO: 等创建了适配器后，设置适配器
                // val adapter = HistoryAdapter(records) { record ->
                //     // 处理点击事件
                // }
                // favoritesRecyclerView.adapter = adapter
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}
