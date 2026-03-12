package com.tianhu.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tianhu.app.database.entities.RecognitionRecord
import kotlinx.coroutines.launch
import org.json.JSONObject

class ResultActivity : AppCompatActivity() {

    private lateinit var backButton: Button
    private lateinit var shareButton: Button
    private lateinit var favoriteButton: LinearLayout
    private lateinit var favoriteIcon: ImageView
    private lateinit var favoriteText: TextView
    private lateinit var recognizeAgainButton: Button
    private lateinit var fruitImage: ImageView
    private lateinit var fruitName: TextView
    private lateinit var confidence: TextView
    private lateinit var calorieTag: TextView
    private lateinit var vitaminTag: TextView
    private lateinit var tipTag: TextView
    private lateinit var nutritionList: LinearLayout
    private lateinit var suggestionContent: TextView

    private var recordId: Long? = null
    private var record: RecognitionRecord? = null
    private var isFavorite: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        recordId = intent.getLongExtra("record_id", -1L).let {
            if (it == -1L) null else it
        }

        initViews()
        setupClickListeners()
        
        recordId?.let { loadRecord(it) }
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        shareButton = findViewById(R.id.shareButton)
        favoriteButton = findViewById(R.id.favoriteButton)
        favoriteIcon = findViewById(R.id.favoriteIcon)
        favoriteText = findViewById(R.id.favoriteText)
        recognizeAgainButton = findViewById(R.id.recognizeAgainButton)
        fruitImage = findViewById(R.id.fruitImage)
        fruitName = findViewById(R.id.fruitName)
        confidence = findViewById(R.id.confidence)
        calorieTag = findViewById(R.id.calorieTag)
        vitaminTag = findViewById(R.id.vitaminTag)
        tipTag = findViewById(R.id.tipTag)
        nutritionList = findViewById(R.id.nutritionList)
        suggestionContent = findViewById(R.id.suggestionContent)
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            finish()
        }

        shareButton.setOnClickListener {
            shareResult()
        }

        favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        recognizeAgainButton.setOnClickListener {
            val intent = Intent(this, ImageUploadActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loadRecord(id: Long) {
        lifecycleScope.launch {
            record = RecognitionRecordService.getRecordById(this@ResultActivity, id)
            record?.let {
                displayRecord(it)
                isFavorite = it.is_collected
                updateFavoriteButton(isFavorite)
            } ?: run {
                Toast.makeText(this@ResultActivity, "记录不存在", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayRecord(record: RecognitionRecord) {
        try {
            val uri = Uri.parse(record.image_uri)
            fruitImage.setImageURI(uri)
        } catch (e: Exception) {
            fruitImage.setImageResource(R.drawable.resource__apple)
        }

        fruitName.text = record.fruit_veg_name
        confidence.text = String.format("置信度 %.1f%%", record.confidence * 100)

        record.nutrition_data?.let { nutritionData ->
            try {
                val json = JSONObject(nutritionData)
                
                val calories = json.optString("calories", "-")
                val vitaminC = json.optString("vitamin_c", "-")
                val tips = json.optString("tips", "")
                
                calorieTag.text = "热量：$calories"
                vitaminTag.text = "维生素C：$vitaminC"
                tipTag.text = "选购建议：$tips"
                
                nutritionList.removeAllViews()
                json.keys().forEach { key ->
                    if (key !in listOf("calories", "vitamin_c", "tips")) {
                        val value = json.optString(key, "-")
                        val textView = TextView(this).apply {
                            text = "${getNutritionLabel(key)}：$value"
                            textSize = 14f
                            setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.text_tertiary))
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                topMargin = 4
                            }
                        }
                        nutritionList.addView(textView)
                    }
                }
                
                suggestionContent.text = json.optString("tips", "暂无食用建议")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getNutritionLabel(key: String): String {
        return when (key) {
            "water" -> "水分"
            "protein" -> "蛋白质"
            "fat" -> "脂肪"
            "carbohydrates" -> "碳水化合物"
            "fiber" -> "膳食纤维"
            "potassium" -> "钾"
            "vitamin_a" -> "维生素A"
            else -> key
        }
    }

    private fun shareResult() {
        record?.let {
            val shareText = "我用 FreshID 识别了 ${it.fruit_veg_name}，置信度 ${String.format("%.1f%%", it.confidence * 100)}！"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "分享识别结果"))
        }
    }

    private fun toggleFavorite() {
        val newStatus = !isFavorite
        updateFavoriteButton(newStatus)
        
        lifecycleScope.launch {
            recordId?.let { id ->
                RecognitionRecordService.toggleCollection(this@ResultActivity, id)
                isFavorite = newStatus
            }
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        this.isFavorite = isFavorite
        
        if (isFavorite) {
            favoriteIcon.setImageResource(R.drawable.ic_star_filled)
            favoriteText.text = "已收藏"
            favoriteButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            favoriteIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
            favoriteText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_star)
            favoriteText.text = "收藏"
            favoriteButton.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            favoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
            favoriteText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
        
        favoriteIcon.requestLayout()
        favoriteText.requestLayout()
        favoriteButton.requestLayout()
    }

}