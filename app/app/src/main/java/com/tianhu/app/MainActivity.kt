package com.tianhu.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var startBtn: Button
    private lateinit var tabHome: LinearLayout
    private lateinit var tabHistory: LinearLayout
    private lateinit var tabMine: LinearLayout
    private lateinit var tabHomeIcon: ImageView
    private lateinit var tabHistoryIcon: ImageView
    private lateinit var tabMineIcon: ImageView
    private lateinit var moreBtn: ImageView

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isFirstLaunch()) {
            navigateToOnboarding()
            return
        }
        
        setContentView(R.layout.activity_main)
        initViews()
        setupClickListeners()
        checkPermissionsOnNonFirstLaunch()
        updateTabSelection(0)
    }

    private fun isFirstLaunch(): Boolean {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        return prefs.getBoolean("is_first_launch", true)
    }

    private fun navigateToOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        startBtn = findViewById(R.id.startBtn)
        tabHome = findViewById(R.id.tabHome)
        tabHistory = findViewById(R.id.tabHistory)
        tabMine = findViewById(R.id.tabMine)
        tabHomeIcon = findViewById(R.id.tabHomeIcon)
        tabHistoryIcon = findViewById(R.id.tabHistoryIcon)
        tabMineIcon = findViewById(R.id.tabMineIcon)
        moreBtn = findViewById(R.id.moreBtn)
    }

    private fun setupClickListeners() {
        startBtn.setOnClickListener {
            startActivity(Intent(this, ImageUploadActivity::class.java))
        }

        tabHome.setOnClickListener {
            updateTabSelection(0)
        }

        tabHistory.setOnClickListener {
            updateTabSelection(1)
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        tabMine.setOnClickListener {
            updateTabSelection(2)
            startActivity(Intent(this, UserManagementActivity::class.java))
        }

        moreBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissionsOnNonFirstLaunch() {
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            showPermissionGuideDialog()
        }
    }

    private fun showPermissionGuideDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限提示")
            .setMessage("开启相机和存储权限可体验完整功能")
            .setPositiveButton("前往设置") { _, _ ->
            }
            .setNegativeButton("稍后") { _, _ ->
            }
            .show()
    }

    private fun updateTabSelection(position: Int) {
        tabHomeIcon.setColorFilter(getColor(if (position == 0) R.color.primary else R.color.text_tertiary))
        tabHistoryIcon.setColorFilter(getColor(if (position == 1) R.color.primary else R.color.text_tertiary))
        tabMineIcon.setColorFilter(getColor(if (position == 2) R.color.primary else R.color.text_tertiary))
        
        (tabHome.getChildAt(1) as TextView).setTextColor(getColor(if (position == 0) R.color.primary else R.color.text_tertiary))
        (tabHistory.getChildAt(1) as TextView).setTextColor(getColor(if (position == 1) R.color.primary else R.color.text_tertiary))
        (tabMine.getChildAt(1) as TextView).setTextColor(getColor(if (position == 2) R.color.primary else R.color.text_tertiary))
    }

}