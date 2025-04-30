package com.example.applock.screen.home.all_app

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.example.applock.model.AppInfo
import com.example.applock.screen.home.AppAdapter

class AllAppsFragment : Fragment() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val packageManager = requireContext().packageManager

        val installedApps = packageManager.getInstalledApplications(
            PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
        )

        val appInfoList = installedApps.map { appInfo ->
            val icon: Drawable = appInfo.loadIcon(packageManager)
            val name = packageManager.getApplicationLabel(appInfo).toString()
            AppInfo(icon, name)
        }

        val adapter = AppAdapter(appInfoList)
        recyclerView.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_apps, container, false)
    }
}