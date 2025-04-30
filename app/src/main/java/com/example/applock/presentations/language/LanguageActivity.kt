package com.example.applock.presentations.language

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.applock.databinding.ActivityLanguageBinding
import com.example.applock.R
import com.example.applock.model.Language

class LanguageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLanguageBinding
    val languageList = listOf(
        Language("English", R.drawable.ic_english),
        Language("Korean", R.drawable.ic_korean),
        Language("Portugal", R.drawable.ic_portugal),
        Language("Spanish", R.drawable.ic_spanish),
        Language("Japanese", R.drawable.ic_japanese),
        Language("German", R.drawable.ic_german),
        Language("Polish", R.drawable.ic_polish),
        Language("Italian", R.drawable.ic_italian),
        Language("French", R.drawable.ic_french),
        Language("Hindi", R.drawable.ic_hindi),
        Language("VIetnamese", R.drawable.ic_vietnamese)

    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_language)
        binding.apply {
            val languagerv = recyclerView
            languagerv.layoutManager = LinearLayoutManager(this@LanguageActivity)
            languagerv.adapter = LanguageItemAdapter(languageList)

        }
    }
}