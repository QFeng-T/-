package com.tianhu.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tianhu.app.database.entities.RecognitionRecord
import kotlinx.coroutines.launch

enum class SortType {
    TIME_DESC,
    TIME_ASC,
    NAME_ASC,
    NAME_DESC
}

class HistoryActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var clearButton: Button
    private lateinit var sortButton: Button
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var goToScanButton: Button
    
    private lateinit var adapter: HistoryAdapter
    private val records = mutableListOf<RecognitionRecord>()
    private val allRecords = mutableListOf<RecognitionRecord>()
    
    private lateinit var sharedPreferences: SharedPreferences
    private var currentSortType: SortType = SortType.TIME_DESC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        initViews()
        setupSharedPreferences()
        setupRecyclerView()
        setupClickListeners()
        loadHistory()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        clearButton = findViewById(R.id.clearButton)
        sortButton = findViewById(R.id.sortButton)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        goToScanButton = findViewById(R.id.goToScanButton)
    }

    private fun setupSharedPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val savedSortType = sharedPreferences.getInt("sort_type", SortType.TIME_DESC.ordinal)
        currentSortType = SortType.entries[savedSortType]
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            records = records,
            onItemClick = { record ->
                openResultDetail(record)
            },
            onFavoriteClick = { record, position ->
                toggleFavorite(record, position)
            },
            onDeleteClick = { record, position ->
                showDeleteConfirmation(record, position)
            }
        )
        
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = adapter
        
        setupSwipeToDelete()
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val record = records[position]
                    showDeleteConfirmation(record, position)
                    adapter.notifyItemChanged(position)
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(historyRecyclerView)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        clearButton.setOnClickListener {
            showClearAllConfirmation()
        }

        sortButton.setOnClickListener {
            showSortDialog()
        }

        goToScanButton.setOnClickListener {
            val intent = Intent(this, ImageUploadActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "时间倒序（最新在前）",
            "时间正序（最早在前）",
            "名称正序（A-Z）",
            "名称倒序（Z-A）"
        )
        
        val checkedItem = currentSortType.ordinal
        
        AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, checkedItem) { dialog, which ->
                currentSortType = SortType.entries[which]
                saveSortPreference()
                applySort()
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveSortPreference() {
        sharedPreferences.edit()
            .putInt("sort_type", currentSortType.ordinal)
            .apply()
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            val recordsFromDb = RecognitionRecordService.getAllRecords(this@HistoryActivity)
            allRecords.clear()
            allRecords.addAll(recordsFromDb)
            applySort()
        }
    }

    private fun applySort() {
        val sortedRecords = when (currentSortType) {
            SortType.TIME_DESC -> allRecords.sortedByDescending { it.create_time }
            SortType.TIME_ASC -> allRecords.sortedBy { it.create_time }
            SortType.NAME_ASC -> allRecords.sortedBy { it.fruit_veg_name }
            SortType.NAME_DESC -> allRecords.sortedByDescending { it.fruit_veg_name }
        }
        updateUI(sortedRecords)
    }

    private fun updateUI(recordList: List<RecognitionRecord>) {
        if (recordList.isEmpty()) {
            emptyState.visibility = LinearLayout.VISIBLE
            historyRecyclerView.visibility = RecyclerView.GONE
        } else {
            emptyState.visibility = LinearLayout.GONE
            historyRecyclerView.visibility = RecyclerView.VISIBLE
            records.clear()
            records.addAll(recordList)
            adapter.notifyDataSetChanged()
        }
    }

    private fun openResultDetail(record: RecognitionRecord) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("record_id", record.id)
        startActivity(intent)
    }

    private fun toggleFavorite(record: RecognitionRecord, position: Int) {
        val updatedRecord = record.copy(is_collected = !record.is_collected)
        
        val indexInAll = allRecords.indexOfFirst { it.id == record.id }
        if (indexInAll != -1) {
            allRecords[indexInAll] = updatedRecord
        }
        
        records[position] = updatedRecord
        adapter.notifyItemChanged(position)
        
        lifecycleScope.launch {
            RecognitionRecordService.toggleCollection(this@HistoryActivity, record.id)
        }
    }

    private fun showDeleteConfirmation(record: RecognitionRecord, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("是否删除该记录？")
            .setPositiveButton("确认") { _, _ ->
                deleteRecord(record, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteRecord(record: RecognitionRecord, position: Int) {
        records.removeAt(position)
        adapter.notifyItemRemoved(position)
        
        val indexInAll = allRecords.indexOfFirst { it.id == record.id }
        if (indexInAll != -1) {
            allRecords.removeAt(indexInAll)
        }
        
        checkEmptyState()
        
        lifecycleScope.launch {
            RecognitionRecordService.deleteRecord(this@HistoryActivity, record.id)
        }
    }

    private fun showClearAllConfirmation() {
        if (allRecords.isEmpty()) {
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("清空历史记录")
            .setMessage("是否清空所有历史记录？此操作不可恢复。")
            .setPositiveButton("确认") { _, _ ->
                clearAllRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearAllRecords() {
        val recordsToDelete = allRecords.toList()
        allRecords.clear()
        records.clear()
        adapter.notifyDataSetChanged()
        checkEmptyState()
        
        lifecycleScope.launch {
            RecognitionRecordService.deleteAllRecords(this@HistoryActivity)
        }
    }

    private fun checkEmptyState() {
        if (allRecords.isEmpty()) {
            emptyState.visibility = LinearLayout.VISIBLE
            historyRecyclerView.visibility = RecyclerView.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }
}
