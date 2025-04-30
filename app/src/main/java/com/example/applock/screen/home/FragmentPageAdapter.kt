package com.example.applock.screen.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.applock.screen.home.all_app.AllAppsFragment
import com.example.applock.screen.home.locked_app.LockedAppsFragment

class FragmentPageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0)
            AllAppsFragment()
        else
            LockedAppsFragment()
    }

}