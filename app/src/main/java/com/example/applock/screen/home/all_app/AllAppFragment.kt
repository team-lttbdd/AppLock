package com.example.applock.screen.home.all_app

import com.example.applock.base.BaseFragment
import com.example.applock.databinding.FragmentAllAppsBinding

import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class AllAppFragment : BaseFragment<FragmentAllAppsBinding>() {

    private lateinit var allAppAdapter: AllAppAdapter

    override fun onResume() {
        super.onResume()
        allAppAdapter.setNewList(listAppInfo)

    }

    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentAllAppsBinding {
        return FragmentAllAppsBinding.inflate(layoutInflater)
    }

    override fun initData() {

    }

    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            allAppAdapter = AllAppAdapter(listAppInfo) { clickedAppInfo ->
                // Avoid adding the same app to listLockedAppInfo multiple times when the user clicks repeatedly
                if(!listLockedAppInfo.contains(clickedAppInfo)) {
                    //Update the database
                    lifecycleScope.launch(Dispatchers.IO) {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        db.appInfoDAO().updateAppLockStatus(clickedAppInfo.packageName, true)
                    }

                    //Ensure that DiffUtil can accurately detect changes between the old and new lists
                    val tempList = listAppInfo.filterNot { it == clickedAppInfo }
                    AppInfoUtil.insertSortedAppInfo(listLockedAppInfo, clickedAppInfo)

                    allAppAdapter.setNewList(tempList)
                    listAppInfo.remove(clickedAppInfo)
                }

            }

            recyclerView.adapter = allAppAdapter
            recyclerView.itemAnimator = allAppAdapter.SlideOutRightItemAnimator()


            searchBar.clearFocus()
            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterList(newText ?: "")
                    return true
                }
            })
        }

    }

    private fun filterList(text : String) {
        val filteredList = listAppInfo.filter {
            it.name.lowercase().contains(text.lowercase())
        }

        allAppAdapter.setNewList(filteredList)
        if (filteredList.isEmpty()) Toast.makeText(
            requireContext(),
            "No data found",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun handleEvent() {

    }
}
