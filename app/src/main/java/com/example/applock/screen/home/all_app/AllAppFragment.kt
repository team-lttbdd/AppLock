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

// Fragment hiển thị danh sách ứng dụng chưa khóa
class AllAppFragment : BaseFragment<FragmentAllAppsBinding>() {
    private lateinit var allAppAdapter: AllAppAdapter // Adapter cho RecyclerView
    private var checkBox: Boolean = false // Trạng thái checkbox chọn tất cả
    private var lastButtonClickTime = 0L // Thời gian click nút cuối
    private var _viewModel: AppLockViewModel? = null // ViewModel lưu dữ liệu
    private val viewModel: AppLockViewModel
        get() = _viewModel ?: throw IllegalStateException("ViewModel not initialized")

    // Gán ViewModel từ bên ngoài
    fun setViewModel(viewModel: AppLockViewModel) {
        _viewModel = viewModel
    }

    // Đặt lại trạng thái khi Fragment hiển thị lại
    override fun onResume() {
        super.onResume()
        val appList = viewModel.allApps.value ?: mutableListOf()
        binding.recyclerView.itemAnimator = null
        checkBox = false
        allAppAdapter.updateAllPosition(false)
        allAppAdapter.count = 0
        updateBtnLock()
        updateCheckboxState(appList)
        allAppAdapter.setNewList(appList)
        binding.recyclerView.post { binding.recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator() }
    }

    // Khởi tạo giao diện
    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentAllAppsBinding {
        return FragmentAllAppsBinding.inflate(layoutInflater)
    }

    // Khởi tạo dữ liệu
    override fun initData() {
        val initialApps = AppInfoUtil.listAppInfo.filter { !it.isLocked }
        viewModel.updateAllApps(initialApps.toMutableList())
    }

    // Thiết lập giao diện
    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            allAppAdapter = AllAppAdapter(mutableListOf()) { clickedAppInfo ->
                allAppAdapter.updateSelectedPosition(clickedAppInfo)
                updateBtnLock()
                updateCheckboxState(allAppAdapter.getCurrentList())
            }
            recyclerView.adapter = allAppAdapter
            recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

            viewModel.allApps.observe(viewLifecycleOwner) { apps ->
                val originalAnimator = recyclerView.itemAnimator
                recyclerView.itemAnimator = null
                allAppAdapter.setNewList(apps ?: mutableListOf())
                cbSelectAll.isEnabled = apps?.isNotEmpty() == true
                recyclerView.post { recyclerView.itemAnimator = originalAnimator }
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

    // Xử lý sự kiện click
    override fun handleEvent() {
        binding.apply {
            btnLock.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000 && allAppAdapter.count != 0) {
                    lastButtonClickTime = currentTime
                    val flagsSnapshot = allAppAdapter.booleanArray.copyOf()
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        val lockedAppsList = mutableListOf<AppInfo>()
                        val unlockedAppsList = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                val appToLock = unlockedAppsList[i]
                                db.appInfoDAO().updateAppLockStatus(appToLock.packageName, true)
                                appToLock.isLocked = true
                                lockedAppsList.add(appToLock)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            viewModel.updateLockedApps((viewModel.lockedApps.value ?: mutableListOf()).apply { addAll(lockedAppsList) })
                            val currentAllApps = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                            val appsToRemove = lockedAppsList.map { it.packageName }
                            currentAllApps.removeAll { appsToRemove.contains(it.packageName) }
                            currentAllApps.sortBy { it.name }
                            viewModel.updateAllApps(currentAllApps)
                            allAppAdapter.setNewList(currentAllApps)
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
                        allAppAdapter.count = currentList.size
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

    // Cập nhật giao diện nút khóa
    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            if (allAppAdapter.count != 0) {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_active_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tvLock.text = getString(R.string.lock_with_count, allAppAdapter.count, getString(R.string.lock))
            } else {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_inactive_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.hint_text))
                tvLock.text = getString(R.string.lock)
            }
        }
    }

    // Cập nhật trạng thái checkbox
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