package com.tianhu.app.ui.activities

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
import com.tianhu.app.service.RecognitionRecordService
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
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        initViews()
        setClickListeners()
        loadUserData()
        loadStatistics()
        updateUIForLoginState()
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
        loginButton = findViewById(R.id.loginButton)
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

        loginButton.setOnClickListener {
            showLoginDialog()
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
            .setPositiveButton("保存") { _, _ -&gt;
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
            .setPositiveButton("退出") { _, _ -&gt;
                performLogout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performLogout() {
        com.tianhu.app.network.ApiClient.clearTokens(this)
        
        val sharedPrefs = getSharedPreferences("freshid_prefs", MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("nickname", "游客用户")
            .putString("uid", "guest_001")
            .apply()
        
        loadUserData()
        updateUIForLoginState()
        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateUIForLoginState() {
        val accessToken = com.tianhu.app.network.ApiClient.getAccessToken(this)
        val isLoggedIn = accessToken.isNotEmpty()
        
        loginButton.visibility = if (isLoggedIn) android.view.View.GONE else android.view.View.VISIBLE
        logoutButton.visibility = if (isLoggedIn) android.view.View.VISIBLE else android.view.View.GONE
        editProfileButton.isEnabled = isLoggedIn
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

    private fun showLoginDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login, null)

        val phoneEditText = dialogView.findViewById&lt;EditText&gt;(R.id.phoneEditText)
        val codeEditText = dialogView.findViewById&lt;EditText&gt;(R.id.codeEditText)
        val sendCodeButton = dialogView.findViewById&lt;Button&gt;(R.id.sendCodeButton)
        val loginButton = dialogView.findViewById&lt;Button&gt;(R.id.loginButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        sendCodeButton.setOnClickListener {
            val phone = phoneEditText.text.toString().trim()
            if (phone.isNotEmpty()) {
                sendVerificationCode(phone)
            } else {
                Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show()
            }
        }

        loginButton.setOnClickListener {
            val phone = phoneEditText.text.toString().trim()
            val code = codeEditText.text.toString().trim()
            if (phone.isNotEmpty() &amp;&amp; code.isNotEmpty()) {
                performLogin(phone, code, dialog)
            } else {
                Toast.makeText(this, "请输入手机号和验证码", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun sendVerificationCode(phone: String) {
        lifecycleScope.launch {
            try {
                val apiService = com.tianhu.app.network.ApiClient.getApiService(this@UserManagementActivity)
                val request = com.tianhu.app.network.dto.SendCodeRequest(phone_number = phone)
                val response = apiService.sendCode(request)

                if (response.isSuccessful &amp;&amp; response.body()?.success == true) {
                    Toast.makeText(this@UserManagementActivity, "验证码已发送", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UserManagementActivity, "发送验证码失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserManagementActivity, "发送验证码失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performLogin(phone: String, code: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                val apiService = com.tianhu.app.network.ApiClient.getApiService(this@UserManagementActivity)
                val request = com.tianhu.app.network.dto.LoginRequest(phone_number = phone, code = code)
                val response = apiService.login(request)

                if (response.isSuccessful &amp;&amp; response.body()?.success == true) {
                    val loginResponse = response.body()?.data
                    if (loginResponse != null) {
                        com.tianhu.app.network.ApiClient.saveTokens(
                            this@UserManagementActivity,
                            loginResponse.access_token,
                            loginResponse.refresh_token
                        )

                        val sharedPrefs = getSharedPreferences("freshid_prefs", MODE_PRIVATE)
                        sharedPrefs.edit()
                            .putString("nickname", loginResponse.user.nickname ?: phone)
                            .putString("uid", loginResponse.user.uid)
                            .apply()

                        dialog.dismiss()
                        loadUserData()
                        updateUIForLoginState()
                        Toast.makeText(this@UserManagementActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@UserManagementActivity, "登录失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserManagementActivity, "登录失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }
}
