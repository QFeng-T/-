package com.tianhu.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var skipButton: TextView
    private lateinit var nextButton: Button
    private lateinit var agreeButton: Button
    private lateinit var pageIndicator1: ImageView
    private lateinit var pageIndicator2: ImageView
    private lateinit var pageIndicator3: ImageView

    private val PERMISSION_REQUEST_CODE = 1001
    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

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

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePageIndicators(position)
                updateButtons(position)
            }
        })
    }

    private fun updatePageIndicators(position: Int) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val tertiaryColor = ContextCompat.getColor(this, R.color.text_tertiary)

        pageIndicator1.setColorFilter(if (position >= 0) primaryColor else tertiaryColor)
        pageIndicator2.setColorFilter(if (position >= 1) primaryColor else tertiaryColor)
        pageIndicator3.setColorFilter(if (position >= 2) primaryColor else tertiaryColor)
    }

    private fun updateButtons(position: Int) {
        when (position) {
            0, 1 -> {
                skipButton.visibility = TextView.VISIBLE
                nextButton.visibility = Button.VISIBLE
                agreeButton.visibility = Button.GONE
            }
            2 -> {
                skipButton.visibility = TextView.GONE
                nextButton.visibility = Button.GONE
                agreeButton.visibility = Button.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        skipButton.setOnClickListener {
            checkPermissionsAndProceed()
        }

        nextButton.setOnClickListener {
            val currentItem = viewPager.currentItem
            if (currentItem < 2) {
                viewPager.currentItem = currentItem + 1
            }
        }

        agreeButton.setOnClickListener {
            checkPermissionsAndProceed()
        }
    }

    private fun checkPermissionsAndProceed() {
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            markOnboardingComplete()
            navigateToMain()
        } else {
            requestPermissions(missingPermissions.toTypedArray())
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            this,
            permissions,
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allGranted) {
                markOnboardingComplete()
                navigateToMain()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限提示")
            .setMessage("开启相机和存储权限可体验完整功能")
            .setPositiveButton("前往设置") { _, _ ->
                markOnboardingComplete()
                navigateToMain()
            }
            .setNegativeButton("稍后") { _, _ ->
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
        startActivity(intent)
        finish()
    }
}
