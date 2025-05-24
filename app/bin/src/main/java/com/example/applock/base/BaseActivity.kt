package com.example.applock.base

import com.example.applock.util.LanguageUtil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB: ViewBinding> : AppCompatActivity() {
    protected lateinit var binding: VB

    abstract fun getViewBinding(layoutInflater: LayoutInflater) : VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageUtil.setLanguage(this@BaseActivity)
        binding = getViewBinding(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        hideBottomNavigation()

        initData()
        setupView()
        handleEvent()
    }

    abstract fun initData()

    abstract fun setupView()

    abstract fun handleEvent()

    private fun hideBottomNavigation() {
        window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}