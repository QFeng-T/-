package com.tianhu.app

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tianhu.app.database.entities.RecognitionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var exportButton: Button
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var goToScanButton: Button
    private lateinit var searchEditText: EditText
    private lateinit var searchClearButton: Button
    
    private lateinit var adapter: HistoryAdapter
    private val records = mutableListOf<RecognitionRecord>()
    private val allRecords = mutableListOf<RecognitionRecord>()
    
    private lateinit var sharedPreferences: SharedPreferences
    private var currentSortType: SortType = SortType.TIME_DESC
    private var searchQuery: String = ""

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
        exportButton = findViewById(R.id.exportButton)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        emptyState = findViewById(R.id.emptyState)
        goToScanButton = findViewById(R.id.goToScanButton)
        searchEditText = findViewById(R.id.searchEditText)
        searchClearButton = findViewById(R.id.searchClearButton)
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
            onShareClick = { record, position ->
                shareRecord(record)
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

        exportButton.setOnClickListener {
            showExportDialog()
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
                applyFilterAndSort()
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })

        searchClearButton.setOnClickListener {
            searchEditText.text.clear()
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
                applyFilterAndSort()
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
            applyFilterAndSort()
        }
    }

    private fun applyFilterAndSort() {
        var filteredRecords = if (searchQuery.isEmpty()) {
            allRecords
        } else {
            allRecords.filter { 
                it.fruit_veg_name.contains(searchQuery, ignoreCase = true) 
            }
        }

        val sortedRecords = when (currentSortType) {
            SortType.TIME_DESC -> filteredRecords.sortedByDescending { it.create_time }
            SortType.TIME_ASC -> filteredRecords.sortedBy { it.create_time }
            SortType.NAME_ASC -> filteredRecords.sortedBy { it.fruit_veg_name }
            SortType.NAME_DESC -> filteredRecords.sortedByDescending { it.fruit_veg_name }
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

    private fun showExportDialog() {
        if (allRecords.isEmpty()) {
            Toast.makeText(this, "暂无记录可导出", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("导出数据")
            .setMessage("是否导出所有识别记录为CSV文件？")
            .setPositiveButton("导出") { _, _ ->
                exportToCSV()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportToCSV() {
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                try {
                    val dateFormatFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val dateFormatRecord = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val timeStamp = dateFormatFile.format(Date())
                    val fileName = "FreshID_Records_$timeStamp.csv"
                    val exportDir = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "exports")
                    if (!exportDir.exists()) {
                        exportDir.mkdirs()
                    }
                    
                    val file = File(exportDir, fileName)
                    FileWriter(file).use { writer ->
                        writer.append("ID,果蔬名称,置信度,识别时间,是否收藏\n")
                        
                        allRecords.forEach { record ->
                            val timeStr = dateFormatRecord.format(Date(record.create_time))
                            val isCollected = if (record.is_collected) "是" else "否"
                            
                            writer.append("${record.id},")
                            writer.append("\"${record.fruit_veg_name}\",")
                            writer.append("${"%.2f".format(record.confidence * 100)}%,")
                            writer.append("$timeStr,")
                            writer.append("$isCollected\n")
                        }
                    }
                    file
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            withContext(Dispatchers.Main) {
                if (file != null) {
                    Toast.makeText(this@HistoryActivity, "导出成功: ${file.name}", Toast.LENGTH_LONG).show()
                    shareFile(file)
                } else {
                    Toast.makeText(this@HistoryActivity, "导出失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun shareRecord(record: RecognitionRecord) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timeStr = dateFormat.format(Date(record.create_time))
            val confidencePercent = "%.2f".format(record.confidence * 100)
            
            val shareText = """
                🥬 FreshID 识别结果
                
                果蔬名称: ${record.fruit_veg_name}
                置信度: $confidencePercent%
                识别时间: $timeStr
                
                来自 FreshID 智能果蔬识别应用
            """.trimIndent()
            
            val imageUri = try {
                Uri.parse(record.image_uri)
            } catch (e: Exception) {
                null
            }
            
            val intent = if (imageUri != null) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
            }
            
            startActivity(Intent.createChooser(intent, "分享识别结果"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "分享导出文件"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }
}
