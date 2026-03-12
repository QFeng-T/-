package com.tianhu.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import com.tianhu.app.database.entities.RecognitionRecord
import kotlinx.coroutines.launch

class FavoritesActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var goToScanButton: Button
    private lateinit var searchEditText: EditText
    private lateinit var searchClearButton: Button
    private lateinit var adapter: HistoryAdapter

    private val allRecords = mutableListOf<RecognitionRecord>()
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        loadFavorites()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        goToScanButton = findViewById(R.id.goToScanButton)
        searchEditText = findViewById(R.id.searchEditText)
        searchClearButton = findViewById(R.id.searchClearButton)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        goToScanButton.setOnClickListener {
            val intent = Intent(this, ImageUploadActivity::class.java)
            startActivity(intent)
            finish()
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString()
                searchClearButton.visibility = if (searchQuery.isEmpty()) Button.GONE else Button.VISIBLE
                applyFilter()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        searchClearButton.setOnClickListener {
            searchEditText.text.clear()
        }
    }

    private fun setupRecyclerView() {
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = HistoryAdapter(
            records = mutableListOf(),
            onItemClick = { record ->
                val intent = Intent(this, ResultActivity::class.java)
                intent.putExtra("record_id", record.id)
                startActivity(intent)
            },
            onFavoriteClick = { record, position ->
                toggleFavorite(record, position)
            },
            onShareClick = { record, position ->
            },
            onDeleteClick = { record, position ->
                showDeleteConfirmDialog(record, position)
            }
        )
        
        favoritesRecyclerView.adapter = adapter
    }

    private fun loadFavorites() {
        lifecycleScope.launch {
            val records = RecognitionRecordService.getCollectedRecords(this@FavoritesActivity)
            allRecords.clear()
            allRecords.addAll(records)
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filteredRecords = if (searchQuery.isEmpty()) {
            allRecords
        } else {
            allRecords.filter { 
                it.fruit_veg_name.contains(searchQuery, ignoreCase = true) 
            }
        }

        updateUI(filteredRecords)
    }

    private fun updateUI(recordList: List<RecognitionRecord>) {
        if (recordList.isEmpty()) {
            emptyState.visibility = LinearLayout.VISIBLE
            favoritesRecyclerView.visibility = RecyclerView.GONE
        } else {
            emptyState.visibility = LinearLayout.GONE
            favoritesRecyclerView.visibility = RecyclerView.VISIBLE
            adapter.updateRecords(recordList)
        }
    }

    private fun toggleFavorite(record: RecognitionRecord, position: Int) {
        lifecycleScope.launch {
            RecognitionRecordService.toggleCollection(this@FavoritesActivity, record.id)
            loadFavorites()
        }
    }

    private fun showDeleteConfirmDialog(record: RecognitionRecord, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteRecord(record, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteRecord(record: RecognitionRecord, position: Int) {
        lifecycleScope.launch {
            RecognitionRecordService.deleteRecord(this@FavoritesActivity, record.id)
            loadFavorites()
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }
}
