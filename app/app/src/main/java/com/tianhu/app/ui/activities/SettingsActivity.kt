package com.tianhu.app.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tianhu.app.service.SyncService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var languageItem: RelativeLayout
    private lateinit var languageValue: TextView
    private lateinit var autoSaveItem: RelativeLayout
    private lateinit var autoSaveSwitch: Switch
    private lateinit var modelManagerItem: RelativeLayout
    private lateinit var syncItem: RelativeLayout
    private lateinit var syncStatus: TextView
    private lateinit var aboutItem: RelativeLayout
    private lateinit var updateItem: RelativeLayout
    private lateinit var versionValue: TextView

    private val languages = arrayOf("简体中文", "繁體中文", "English")
    private val languageCodes = arrayOf("zh-CN", "zh-TW", "en")
    private var currentLanguageIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadSettings()
        setupClickListeners()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        languageItem = findViewById(R.id.languageItem)
        languageValue = findViewById(R.id.languageValue)
        autoSaveItem = findViewById(R.id.autoSaveItem)
        autoSaveSwitch = findViewById(R.id.autoSaveSwitch)
        modelManagerItem = findViewById(R.id.modelManagerItem)
        syncItem = findViewById(R.id.syncItem)
        syncStatus = findViewById(R.id.syncStatus)
        aboutItem = findViewById(R.id.aboutItem)
        updateItem = findViewById(R.id.updateItem)
        versionValue = findViewById(R.id.versionValue)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        
        val savedLanguage = prefs.getString("language", "zh-CN")
        currentLanguageIndex = languageCodes.indexOf(savedLanguage).coerceAtLeast(0)
        languageValue.text = languages[currentLanguageIndex]
        
        val autoSave = prefs.getBoolean("auto_save", true)
        autoSaveSwitch.isChecked = autoSave
        
        versionValue.text = "1.0.0"
        
        updateSyncStatus()
    }

    private fun updateSyncStatus() {
        lifecycleScope.launch {
            val stats = SyncService.getSyncStats(this@SettingsActivity)
            val lastSyncTime = SyncService.getLastSyncTime(this@SettingsActivity)
            
            val statusText = when {
                stats["syncFailed"] ?: 0 &gt; 0 -&gt; "同步失败 ${stats["syncFailed"]} 条"
                stats["notSynced"] ?: 0 &gt; 0 -&gt; "待同步 ${stats["notSynced"]} 条"
                lastSyncTime &gt; 0 -&gt; {
                    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                    "已同步 ${sdf.format(Date(lastSyncTime))}"
                }
                else -&gt; "未同步"
            }
            
            syncStatus.text = statusText
        }
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        languageItem.setOnClickListener {
            showLanguageDialog()
        }

        autoSaveSwitch.setOnCheckedChangeListener { _, isChecked -&gt;
            saveAutoSaveSetting(isChecked)
        }

        modelManagerItem.setOnClickListener {
            startActivity(Intent(this, ModelManagerActivity::class.java))
        }

        syncItem.setOnClickListener {
            showSyncDialog()
        }

        aboutItem.setOnClickListener {
            showAboutDialog()
        }

        updateItem.setOnClickListener {
            checkUpdate()
        }
    }

    private fun showSyncDialog() {
        lifecycleScope.launch {
            val stats = SyncService.getSyncStats(this@SettingsActivity)
            val message = """
                同步统计：
                • 已同步：${stats["synced"] ?: 0} 条
                • 待同步：${stats["notSynced"] ?: 0} 条
                • 同步失败：${stats["syncFailed"] ?: 0} 条
            """.trimIndent()

            AlertDialog.Builder(this@SettingsActivity)
                .setTitle("数据同步")
                .setMessage(message)
                .setPositiveButton("立即同步") { _, _ -&gt;
                    performSync()
                }
                .setNeutralButton("重试失败") { _, _ -&gt;
                    retryFailedSync()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun performSync() {
        lifecycleScope.launch {
            val progressDialog = AlertDialog.Builder(this@SettingsActivity)
                .setTitle("正在同步")
                .setMessage("请稍候...")
                .setCancelable(false)
                .create()
            progressDialog.show()

            val result = SyncService.syncData(this@SettingsActivity)
            
            progressDialog.dismiss()
            
            Toast.makeText(this@SettingsActivity, result.message, Toast.LENGTH_LONG).show()
            updateSyncStatus()
        }
    }

    private fun retryFailedSync() {
        lifecycleScope.launch {
            val progressDialog = AlertDialog.Builder(this@SettingsActivity)
                .setTitle("正在重试")
                .setMessage("请稍候...")
                .setCancelable(false)
                .create()
            progressDialog.show()

            val result = SyncService.retryFailedSync(this@SettingsActivity)
            
            progressDialog.dismiss()
            
            Toast.makeText(this@SettingsActivity, result.message, Toast.LENGTH_LONG).show()
            updateSyncStatus()
        }
    }

    private fun showLanguageDialog() {
        AlertDialog.Builder(this)
            .setTitle("选择语言")
            .setSingleChoiceItems(languages, currentLanguageIndex) { _, which -&gt;
                currentLanguageIndex = which
            }
            .setPositiveButton("确定") { _, _ -&gt;
                saveLanguageSetting(currentLanguageIndex)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun saveLanguageSetting(index: Int) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putString("language", languageCodes[index]).apply()
        languageValue.text = languages[index]
        Toast.makeText(this, "语言设置已保存，重启应用生效", Toast.LENGTH_SHORT).show()
    }

    private fun saveAutoSaveSetting(isChecked: Boolean) {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("auto_save", isChecked).apply()
        val message = if (isChecked) "已开启自动保存历史记录" else "已关闭自动保存历史记录"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("关于 FreshID")
            .setMessage("FreshID 智能果蔬识别系统\n\n版本：1.0.0\n\n一款基于深度学习的智能果蔬识别应用，帮助您快速识别果蔬种类并获取营养信息。")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun checkUpdate() {
        lifecycleScope.launch {
            val progressDialog = androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity)
                .setTitle("检查更新")
                .setMessage("正在检查...")
                .setCancelable(false)
                .create()
            progressDialog.show()

            try {
                val apiService = com.tianhu.app.network.ApiClient.getApiService(this@SettingsActivity)
                val response = apiService.checkVersion(
                    currentVersion = "1.0.0",
                    versionCode = 1
                )

                progressDialog.dismiss()

                if (response.isSuccessful &amp;&amp; response.body()?.success == true) {
                    val versionInfo = response.body()?.data
                    if (versionInfo != null) {
                        showUpdateDialog(versionInfo)
                    } else {
                        Toast.makeText(this@SettingsActivity, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@SettingsActivity, "检查更新失败，请稍后重试", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@SettingsActivity, "检查更新失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(versionInfo: com.tianhu.app.network.dto.VersionCheckResponse) {
        val message = buildString {
            append("最新版本: ${versionInfo.latest_version}\n")
            append("\n更新内容:\n${versionInfo.release_notes ?: "暂无更新说明"}")
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage(message)
            .setPositiveButton("立即更新") { _, _ -&gt;
                if (versionInfo.update_url != null) {
                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(versionInfo.update_url))
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "暂未提供下载链接", Toast.LENGTH_SHORT).show()
                }
            }

        if (!versionInfo.force_update) {
            dialog.setNegativeButton("稍后再说", null)
        }

        dialog.setCancelable(!versionInfo.force_update)
        dialog.show()
    }

}
