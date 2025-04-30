package com.example.applock.screen.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.example.applock.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeActivity : AppCompatActivity() {
    private lateinit var viewPager2: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Khởi tạo ViewPager2 và TabLayout
        viewPager2 = findViewById(R.id.viewPager2)
        tabLayout = findViewById(R.id.tabLayout)

        // Thiết lập adapter cho ViewPager2
        val adapter = FragmentPageAdapter(this)
        viewPager2.adapter = adapter

        // Liên kết TabLayout với ViewPager2
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.all_apps)
                1 -> getString(R.string.locked_apps)
                else -> getString(R.string.all_apps)
            }
        }.attach()
    }
}