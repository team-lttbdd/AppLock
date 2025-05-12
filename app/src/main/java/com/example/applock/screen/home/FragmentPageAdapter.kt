package com.example.applock.screen.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.applock.screen.home.all_app.AllAppFragment
import com.example.applock.screen.home.locked_app.LockedAppFragment

class FragmentPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val viewModel: AppLockViewModel
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AllAppFragment().apply { setViewModel(viewModel) }
            1 -> LockedAppFragment().apply { setViewModel(viewModel) }
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}