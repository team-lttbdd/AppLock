package com.example.applock.screen.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.example.applock.model.Language
import com.example.applock.databinding.ItemLanguageBinding

class LanguageItemAdapter(
    private val languageList: List<Language>,
    private val clickListener: (Language) -> Unit
) : RecyclerView.Adapter<LanguageItemViewHolder>() {

    private var selectedPosition: Int = -1
    private lateinit var itemView : ItemLanguageBinding

    fun updateSelectedPosition(selectedLanguage: String) {
        val previousPosition = selectedPosition
        for(i in languageList.indices) {
            if(languageList[i].locale == selectedLanguage) {
                selectedPosition = i
                break
            }
        }

        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageItemViewHolder {
        itemView = ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LanguageItemViewHolder, position: Int) {
        val language = languageList[position]
        holder.bind(language, position == selectedPosition, clickListener)
    }

    override fun getItemCount(): Int = languageList.size
}

class LanguageItemViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(language: Language, isSelected: Boolean, onLanguageSelected: (Language) -> Unit) {
        binding.apply {
            imgLanguageIcon.setImageResource(language.icon)
            tvLanguageName.text = language.name
            tvLanguageName.setTextColor(
                if(isSelected) Color.parseColor("#FFFFFF") else Color.parseColor("#131936")
            )
            itemView.setBackgroundResource(
                if (isSelected) R.drawable.bg_selected_language_item else R.drawable.bg_language_item
            )
            root.setOnClickListener { onLanguageSelected(language) }
        }
    }
}
