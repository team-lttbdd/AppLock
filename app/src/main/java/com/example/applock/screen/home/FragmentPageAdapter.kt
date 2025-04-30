package com.example.applock.screen.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.applock.screen.home.all_app.AllAppsFragment
import com.example.applock.screen.home.locked_app.LockedAppsFragment

class FragmentPageAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int {
        return 2 // Số lượng tab: "Tất Cả Ứng Dụng" và "Ứng Dụng Đã Khóa"
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllAppsFragment()
            1 -> LockedAppsFragment()
            else -> AllAppsFragment() // Mặc định
        }
    }
}