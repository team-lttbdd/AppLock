package com.example.applock.presentations.language

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.example.applock.model.Language

class LanguageItemAdapter(val languageList: List<Language>) : RecyclerView.Adapter<LanguageItemViewHolder>(){
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageItemViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val listItem = layoutInflater.inflate(R.layout.language_item, parent, false)
        return LanguageItemViewHolder(listItem)
    }

    override fun onBindViewHolder(holder: LanguageItemViewHolder, position: Int) {
        val language = languageList[position]
    }

    override fun getItemCount(): Int = languageList.size
}

class LanguageItemViewHolder (val view: View): RecyclerView.ViewHolder(view){

    fun bind(language : Language) {
        val myTextView = view.findViewById<TextView>(R.id.languageName)
        val myImageView = view.findViewById<ImageView>(R.id.languageIcon)
        myTextView.text = language.name
        myImageView.setImageResource(language.icon)
    }

}