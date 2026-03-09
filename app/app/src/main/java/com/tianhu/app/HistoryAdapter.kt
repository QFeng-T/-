package com.tianhu.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tianhu.app.database.entities.RecognitionRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val records: MutableList<RecognitionRecord>,
    private val onItemClick: (RecognitionRecord) -> Unit,
    private val onFavoriteClick: (RecognitionRecord, Int) -> Unit,
    private val onDeleteClick: (RecognitionRecord, Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val historyImage: ImageView = itemView.findViewById(R.id.historyImage)
        val historyName: TextView = itemView.findViewById(R.id.historyName)
        val historyTime: TextView = itemView.findViewById(R.id.historyTime)
        val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        val arrowIcon: ImageView = itemView.findViewById(R.id.arrowIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val record = records[position]
        
        holder.historyName.text = record.fruit_veg_name
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        holder.historyTime.text = dateFormat.format(Date(record.create_time))
        
        try {
            val uri = Uri.parse(record.image_uri)
            holder.historyImage.setImageURI(uri)
        } catch (e: Exception) {
            holder.historyImage.setImageResource(R.drawable.resource__apple)
        }
        
        if (record.is_collected) {
            holder.favoriteIcon.setImageResource(R.drawable.ic_star_filled)
        } else {
            holder.favoriteIcon.setImageResource(R.drawable.ic_star)
        }
        
        holder.favoriteIcon.setOnClickListener {
            onFavoriteClick(record, position)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteClick(record, position)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(record)
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateRecords(newRecords: List<RecognitionRecord>) {
        records.clear()
        records.addAll(newRecords)
        notifyDataSetChanged()
    }

    fun removeItem(position: Int) {
        if (position >= 0 && position < records.size) {
            records.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun updateItem(record: RecognitionRecord, position: Int) {
        if (position >= 0 && position < records.size) {
            records[position] = record
            notifyItemChanged(position)
        }
    }
}
