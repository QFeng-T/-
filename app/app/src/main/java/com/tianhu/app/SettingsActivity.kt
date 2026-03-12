package com.tianhu.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var languageItem: RelativeLayout
    private lateinit var languageValue: TextView
    private lateinit var autoSaveItem: RelativeLayout
    private lateinit var autoSaveSwitch: Switch
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
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        languageItem.setOnClickListener {
            showLanguageDialog()
        }

        autoSaveSwitch.setOnCheckedChangeListener { _, isChecked ->
            saveAutoSaveSetting(isChecked)
        }

        aboutItem.setOnClickListener {
            showAboutDialog()
        }

        updateItem.setOnClickListener {
            checkUpdate()
        }
    }

    private fun showLanguageDialog() {
        AlertDialog.Builder(this)
            .setTitle("选择语言")
            .setSingleChoiceItems(languages, currentLanguageIndex) { _, which ->
                currentLanguageIndex = which
            }
            .setPositiveButton("确定") { _, _ ->
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
        Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show()
    }

}