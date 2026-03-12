package com.tianhu.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserManagementActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var avatarImage: ImageView
    private lateinit var nicknameText: TextView
    private lateinit var uidText: TextView
    private lateinit var editProfileButton: Button
    private lateinit var recognizeCount: TextView
    private lateinit var favoriteCount: TextView
    private lateinit var historyItem: LinearLayout
    private lateinit var favoritesItem: LinearLayout
    private lateinit var settingsItem: LinearLayout
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        initViews()
        setClickListeners()
        loadUserData()
        loadStatistics()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        avatarImage = findViewById(R.id.avatarImage)
        nicknameText = findViewById(R.id.nicknameText)
        uidText = findViewById(R.id.uidText)
        editProfileButton = findViewById(R.id.editProfileButton)
        recognizeCount = findViewById(R.id.recognizeCount)
        favoriteCount = findViewById(R.id.favoriteCount)
        historyItem = findViewById(R.id.historyItem)
        favoritesItem = findViewById(R.id.favoritesItem)
        settingsItem = findViewById(R.id.settingsItem)
        logoutButton = findViewById(R.id.logoutButton)
    }

    private fun setClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        editProfileButton.setOnClickListener {
            showEditNicknameDialog()
        }

        historyItem.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        favoritesItem.setOnClickListener {
            val intent = Intent(this, FavoritesActivity::class.java)
            startActivity(intent)
        }

        settingsItem.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showEditNicknameDialog() {
        val currentNickname = nicknameText.text.toString()
        
        val editText = EditText(this)
        editText.setText(currentNickname)
        editText.inputType = InputType.TYPE_CLASS_TEXT
        editText.hint = "请输入昵称"
        editText.setSingleLine()
        editText.maxLines = 1
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("编辑昵称")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newNickname = editText.text.toString().trim()
                if (newNickname.isNotEmpty()) {
                    saveNickname(newNickname)
                } else {
                    Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .create()
        
        dialog.show()
    }

    private fun saveNickname(nickname: String) {
        val sharedPrefs = getSharedPreferences("freshid_prefs", MODE_PRIVATE)
        sharedPrefs.edit().putString("nickname", nickname).apply()
        
        nicknameText.text = nickname
        Toast.makeText(this, "昵称已更新", Toast.LENGTH_SHORT).show()
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出") { _, _ ->
                performLogout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performLogout() {
        val sharedPrefs = getSharedPreferences("freshid_prefs", MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("nickname", "游客用户")
            .apply()
        
        loadUserData()
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
    }

    private fun loadUserData() {
        val sharedPrefs = getSharedPreferences("freshid_prefs", MODE_PRIVATE)
        val nickname = sharedPrefs.getString("nickname", "游客用户")
        val uid = sharedPrefs.getString("uid", "guest_001")

        nicknameText.text = nickname
        uidText.text = "UID: $uid"
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            val records = withContext(Dispatchers.IO) {
                RecognitionRecordService.getAllRecords(this@UserManagementActivity)
            }

            val favoriteRecords = records.filter { it.is_collected }

            recognizeCount.text = records.size.toString()
            favoriteCount.text = favoriteRecords.size.toString()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }
}
