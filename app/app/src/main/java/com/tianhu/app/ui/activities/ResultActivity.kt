package com.tianhu.app.ui.activities

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tianhu.app.util.ImageCache
import com.tianhu.app.service.RecognitionRecordService
import com.tianhu.app.database.entities.RecognitionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ResultActivity : AppCompatActivity() {

    private lateinit var backButton: ImageView
    private lateinit var shareButton: ImageView
    private lateinit var favoriteButton: CardView
    private lateinit var favoriteIcon: ImageView
    private lateinit var favoriteText: TextView
    private lateinit var recognizeAgainButton: CardView
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
        loadImageAsync(record.image_uri)

        fruitName.text = record.fruit_veg_name
        confidence.text = String.format("置信度 %.1f%%", record.confidence * 100)

        if (record.recognition_type == com.tianhu.app.database.enums.RecognitionType.FRESHNESS_DETECTION) {
            calorieTag.text = if (record.is_fresh == true) "新鲜" else "不新鲜"
            vitaminTag.text = String.format("新鲜度评分: %.1f%%", (record.freshness_score ?: 0f) * 100)
            tipTag.text = if (record.is_fresh == true) "建议立即食用或保存" else "建议尽快食用"
            
            nutritionList.removeAllViews()
            
            val freshnessTextView = TextView(this).apply {
                text = if (record.is_fresh == true) "这是一个新鲜的果蔬，品质良好，可以放心食用。" else "这个果蔬不太新鲜了，建议尽快食用或谨慎选择。"
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@ResultActivity, R.color.text_tertiary))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4
                }
            }
            nutritionList.addView(freshnessTextView)
            
            suggestionContent.text = if (record.is_fresh == true) "新鲜的果蔬不仅口感更好，而且营养价值更高。建议在最佳食用期内享用。" else "不太新鲜的果蔬可能会失去部分营养，口感也会下降。建议尽快食用，或者用于烹饪而非生食。"
        } else {
            record.nutrition_data?.let { nutritionData -&gt;
                try {
                    val json = JSONObject(nutritionData)
                    
                    val calories = json.optString("热量", json.optString("calories", "-"))
                    val vitaminC = json.optString("维生素C", json.optString("vitamin_c", "-"))
                    val tips = json.optString("tips", "")
                    
                    calorieTag.text = "热量：$calories"
                    vitaminTag.text = "维生素C：$vitaminC"
                    tipTag.text = "选购建议：$tips"
                    
                    nutritionList.removeAllViews()
                    json.keys().forEach { key -&gt;
                        if (key !in listOf("calories", "vitamin_c", "tips", "热量", "维生素C")) {
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
    }

    private fun loadImageAsync(imageUri: String) {
        val cachedBitmap = ImageCache.get(imageUri)
        if (cachedBitmap != null) {
            fruitImage.setImageBitmap(cachedBitmap)
            return
        }
        
        fruitImage.setImageResource(R.drawable.resource__apple)
        
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val path = Uri.parse(imageUri).path ?: return@withContext null
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        inSampleSize = calculateInSampleSize(path, fruitImage.width, fruitImage.height)
                    }
                    BitmapFactory.decodeFile(path, options)
                }
                
                bitmap?.let {
                    ImageCache.put(imageUri, it)
                    fruitImage.setImageBitmap(it)
                } ?: run {
                    fruitImage.setImageResource(R.drawable.resource__apple)
                }
            } catch (e: Exception) {
                fruitImage.setImageResource(R.drawable.resource__apple)
            }
        }
    }
    
    private fun calculateInSampleSize(imagePath: String, reqWidth: Int, reqHeight: Int): Int {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)
        
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        
        if (height &gt; reqHeight || width &gt; reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize &gt;= reqHeight &amp;&amp; halfWidth / inSampleSize &gt;= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun getNutritionLabel(key: String): String {
        return when (key) {
            "water" -&gt; "水分"
            "protein" -&gt; "蛋白质"
            "fat" -&gt; "脂肪"
            "carbohydrates" -&gt; "碳水化合物"
            "fiber" -&gt; "膳食纤维"
            "potassium" -&gt; "钾"
            "vitamin_a" -&gt; "维生素A"
            else -&gt; key
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
            recordId?.let { id -&gt;
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
            favoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
            favoriteText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        } else {
            favoriteIcon.setImageResource(R.drawable.ic_star)
            favoriteText.text = "收藏"
            favoriteIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary))
            favoriteText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

}
