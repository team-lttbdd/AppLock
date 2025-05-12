package com.example.applock.screen.home.all_app

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.applock.R
import com.example.applock.base.BaseFragment
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.databinding.FragmentAllAppsBinding
import com.example.applock.model.AppInfo
import com.example.applock.screen.home.AppLockViewModel
import com.example.applock.util.AppInfoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AllAppFragment : BaseFragment<FragmentAllAppsBinding>() {
    private lateinit var allAppAdapter: AllAppAdapter
    private var checkBox: Boolean = false
    private var lastButtonClickTime = 0L
    private var _viewModel: AppLockViewModel? = null
    private val viewModel: AppLockViewModel
        get() = _viewModel ?: throw IllegalStateException("ViewModel not initialized")

    fun setViewModel(viewModel: AppLockViewModel) {
        _viewModel = viewModel
    }

    override fun onResume() {
        super.onResume()
        val appList = viewModel.allApps.value ?: mutableListOf()
        checkBox = false
        allAppAdapter.updateAllPosition(false)
        allAppAdapter.count = 0
        updateBtnLock()
        updateCheckboxState(appList)
        allAppAdapter.setNewList(appList)
    }

    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentAllAppsBinding {
        return FragmentAllAppsBinding.inflate(layoutInflater)
    }

    override fun initData() {
        AppInfoUtil.initInstalledApps(requireContext())
        val initialApps = AppInfoUtil.listAppInfo.filter { !it.isLocked }
        viewModel.updateAllApps(initialApps.toMutableList())
    }

    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            allAppAdapter = AllAppAdapter(mutableListOf()) { clickedAppInfo ->
                allAppAdapter.updateSelectedPosition(clickedAppInfo)
                updateBtnLock()
                updateCheckboxState(allAppAdapter.getCurrentList()) // Cập nhật checkbox khi chọn ứng dụng
            }
            recyclerView.adapter = allAppAdapter
            recyclerView.itemAnimator = SlideOutRightItemAnimator()

            viewModel.allApps.observe(viewLifecycleOwner) { apps ->
                allAppAdapter.setNewList(apps ?: mutableListOf())
                binding.cbSelectAll.isEnabled = (apps?.isNotEmpty() == true)
                if (apps.isNullOrEmpty()) {
                    checkBox = false
                    allAppAdapter.updateAllPosition(false)
                    allAppAdapter.count = 0
                    updateBtnLock()
                    updateCheckboxState(apps ?: mutableListOf())
                } else {
                    updateCheckboxState(apps)
                }
            }

            searchBar.clearFocus()
            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    val currentList = viewModel.allApps.value ?: mutableListOf()
                    AppInfoUtil.filterList(requireContext(), newText ?: "", currentList) { filteredList ->
                        allAppAdapter.setNewList(filteredList.toMutableList())
                        updateCheckboxState(filteredList)
                    }
                    return true
                }
            })
        }
    }

    override fun handleEvent() {
        binding.apply {
            btnLock.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000 && allAppAdapter.count != 0) {
                    lastButtonClickTime = currentTime
                    val transferList: MutableList<AppInfo> = mutableListOf()
                    val currentAllApps = viewModel.allApps.value?.toMutableList() ?: mutableListOf()

                    val flagsSnapshot = allAppAdapter.booleanArray.copyOf()

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                db.appInfoDAO().updateAppLockStatus(currentAllApps[i].packageName, true)
                                transferList.add(currentAllApps[i])
                            }
                        }
                        withContext(Dispatchers.Main) {
                            viewModel.loadInitialData(requireContext())
                            allAppAdapter.count = 0
                            updateBtnLock()
                            updateCheckboxState(viewModel.allApps.value ?: mutableListOf())
                        }
                    }
                }
            }

            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000) {
                    lastButtonClickTime = currentTime
                    val currentList = allAppAdapter.getCurrentList()
                    if (!checkBox) {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_checked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.remove_all)
                        checkBox = true
                        allAppAdapter.updateAllPosition(true)
                        allAppAdapter.count = currentList.size // Đặt count bằng tổng số phần tử
                        updateBtnLock()
                    } else {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_unchecked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.select_all)
                        checkBox = false
                        allAppAdapter.updateAllPosition(false)
                        allAppAdapter.count = 0
                        updateBtnLock()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            if (allAppAdapter.count != 0) {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_active_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tvLock.text = "(${allAppAdapter.count}) ${getString(R.string.lock)}"
            } else {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_inactive_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.hint_text))
                tvLock.text = "${getString(R.string.lock)}"
            }
        }
    }

    private fun updateCheckboxState(appList: List<AppInfo>) {
        val allSelected = allAppAdapter.count == appList.size
        if (allSelected && appList.isNotEmpty()) {
            binding.cbSelectAll.setBackgroundResource(R.drawable.checkbox_checked)
            binding.tvSelectOrRemove.text = ContextCompat.getString(binding.tvSelectOrRemove.context, R.string.remove_all)
            checkBox = true
        } else {
            binding.cbSelectAll.setBackgroundResource(R.drawable.checkbox_unchecked)
            binding.tvSelectOrRemove.text = ContextCompat.getString(binding.tvSelectOrRemove.context, R.string.select_all)
            checkBox = false
        }
        binding.cbSelectAll.isEnabled = appList.isNotEmpty()
        updateBtnLock()
    }
}