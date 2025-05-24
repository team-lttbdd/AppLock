package com.example.applock.screen.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.applock.screen.home.all_app.AllAppFragment
import com.example.applock.screen.home.locked_app.LockedAppFragment

// Adapter cho ViewPager2 để quản lý AllAppFragment và LockedAppFragment
class FragmentPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val viewModel: AppLockViewModel // ViewModel chia sẻ giữa các Fragment
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    // Số lượng Fragment (2: AllAppFragment và LockedAppFragment)
    override fun getItemCount(): Int = 2

    // Tạo Fragment tương ứng với vị trí
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllAppFragment().apply { setViewModel(viewModel) } // Fragment danh sách ứng dụng chưa khóa
            1 -> LockedAppFragment().apply { setViewModel(viewModel) } // Fragment danh sách ứng dụng đã khóa
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}