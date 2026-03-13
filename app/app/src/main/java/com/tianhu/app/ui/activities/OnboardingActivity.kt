package com.tianhu.app.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.tianhu.app.ui.adapters.OnboardingPagerAdapter

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var skipButton: TextView
    private lateinit var nextButton: Button
    private lateinit var agreeButton: Button
    private lateinit var pageIndicator1: ImageView
    private lateinit var pageIndicator2: ImageView
    private lateinit var pageIndicator3: ImageView

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions -&gt;
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            markOnboardingComplete()
            navigateToMain()
        } else {
            showPermissionDeniedDialog()
        }
    }

    private val requiredPermissions: Array&lt;String&gt;
        get() {
            val permissions = mutableListOf(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT &gt;= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            return permissions.toTypedArray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        initViews()
        setupViewPager()
        setupClickListeners()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        skipButton = findViewById(R.id.skipButton)
        nextButton = findViewById(R.id.nextButton)
        agreeButton = findViewById(R.id.agreeButton)
        pageIndicator1 = findViewById(R.id.pageIndicator1)
        pageIndicator2 = findViewById(R.id.pageIndicator2)
        pageIndicator3 = findViewById(R.id.pageIndicator3)
    }

    private fun setupViewPager() {
        val adapter = OnboardingPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = true

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicators(position)
                updateButtons(position)
            }
        })

        updatePageIndicators(0)
        updateButtons(0)
    }

    private fun updatePageIndicators(position: Int) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val tertiaryColor = ContextCompat.getColor(this, R.color.text_tertiary)

        pageIndicator1.setColorFilter(if (position &gt;= 0) primaryColor else tertiaryColor)
        pageIndicator2.setColorFilter(if (position &gt;= 1) primaryColor else tertiaryColor)
        pageIndicator3.setColorFilter(if (position &gt;= 2) primaryColor else tertiaryColor)
    }

    private fun updateButtons(position: Int) {
        when (position) {
            0, 1 -&gt; {
                skipButton.visibility = TextView.VISIBLE
                nextButton.visibility = Button.VISIBLE
                agreeButton.visibility = Button.GONE
            }
            2 -&gt; {
                skipButton.visibility = TextView.GONE
                nextButton.visibility = Button.GONE
                agreeButton.visibility = Button.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        skipButton.setOnClickListener {
            viewPager.currentItem = 2
        }

        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem &lt; 2) {
                viewPager.currentItem = currentItem + 1
            }
        }

        agreeButton.setOnClickListener {
            checkPermissionsAndProceed()
        }
    }

    private fun checkPermissionsAndProceed() {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            markOnboardingComplete()
            navigateToMain()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限说明")
            .setMessage("相机和存储权限用于拍照、保存图片和识别记录。您可以稍后在系统设置中开启。")
            .setPositiveButton("继续使用") { _, _ -&gt;
                markOnboardingComplete()
                navigateToMain()
            }
            .setCancelable(false)
            .show()
    }

    private fun markOnboardingComplete() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (viewPager.currentItem &gt; 0) {
            viewPager.currentItem = viewPager.currentItem - 1
        } else {
            super.onBackPressed()
        }
    }
}
