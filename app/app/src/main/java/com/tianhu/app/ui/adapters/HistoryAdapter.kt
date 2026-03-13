package com.tianhu.app.ui.adapters

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tianhu.app.util.ImageCache
import com.tianhu.app.database.entities.RecognitionRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class HistoryAdapter(
    private val records: MutableList&lt;RecognitionRecord&gt;,
    private val onItemClick: (RecognitionRecord) -&gt; Unit,
    private val onFavoriteClick: (RecognitionRecord, Int) -&gt; Unit,
    private val onShareClick: (RecognitionRecord, Int) -&gt; Unit,
    private val onDeleteClick: (RecognitionRecord, Int) -&gt; Unit
) : RecyclerView.Adapter&lt;HistoryAdapter.HistoryViewHolder&gt;() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val loadingTasks = ConcurrentHashMap&lt;String, Boolean&gt;()
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        setHasStableIds(true)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val historyImage: ImageView = itemView.findViewById(R.id.historyImage)
        val historyName: TextView = itemView.findViewById(R.id.historyName)
        val historyTime: TextView = itemView.findViewById(R.id.historyTime)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        val shareButton: ImageView = itemView.findViewById(R.id.shareButton)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)
        var currentImageKey: String? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = records[position]
        
        holder.historyName.text = record.fruit_veg_name
        holder.historyTime.text = dateFormat.format(Date(record.create_time))
        
        val imageKey = record.image_uri
        holder.currentImageKey = imageKey
        
        val cachedBitmap = ImageCache.get(imageKey)
        if (cachedBitmap != null) {
            holder.historyImage.setImageBitmap(cachedBitmap)
        } else {
            holder.historyImage.setImageResource(R.drawable.resource__apple)
            loadBitmapAsync(holder, record, imageKey)
        }
        
        if (record.is_collected) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_star_filled)
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_star)
        }
        
        holder.favoriteIcon.setOnClickListener {
            onFavoriteClick(record, position)
        }
        
        holder.shareButton.setOnClickListener {
            onShareClick(record, position)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(record, position)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    private fun loadBitmapAsync(holder: HistoryViewHolder, record: RecognitionRecord, imageKey: String) {
        if (loadingTasks.putIfAbsent(imageKey, true) != null) {
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val path = Uri.parse(record.image_uri).path
                if (path != null &amp;&amp; File(path).exists()) {
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        inSampleSize = 4
                    }
                    val bitmap = BitmapFactory.decodeFile(path, options)
                    
                    if (bitmap != null) {
                        ImageCache.put(imageKey, bitmap)
                        
                        mainHandler.post {
                            if (holder.currentImageKey == imageKey) {
                                holder.historyImage.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadingTasks.remove(imageKey)
            }
        }
    }

    override fun onViewRecycled(holder: HistoryViewHolder) {
        super.onViewRecycled(holder)
        holder.currentImageKey = null
        holder.historyImage.setImageResource(R.drawable.resource__apple)
    }

    override fun getItemCount(): Int = records.size

    override fun getItemId(position: Int): Long {
        return records[position].id
    }

    fun updateRecords(newRecords: List&lt;RecognitionRecord&gt;) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position &gt;= 0 &amp;&amp; position &lt; records.size) {
            val record = records[position]
            ImageCache.remove(record.image_uri)
            records.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItem(record: RecognitionRecord, position: Int) {
        if (position &gt;= 0 &amp;&amp; position &lt; records.size) {
            records[position] = record
            notifyItemChanged(position)
        }
    }
}
