package com.example.applock.screen.home.locked_app

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.applock.R
import com.example.applock.base.BaseFragment
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.databinding.FragmentLockedAppsBinding
import com.example.applock.model.AppInfo
import com.example.applock.screen.home.AppLockViewModel
import com.example.applock.util.AppInfoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Fragment hiển thị danh sách ứng dụng đã khóa
class LockedAppFragment : BaseFragment<FragmentLockedAppsBinding>() {
    private lateinit var lockedAppAdapter: LockedAppAdapter // Adapter cho RecyclerView
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
        val appList = viewModel.lockedApps.value ?: mutableListOf()
        checkBox = false
        lockedAppAdapter.updateAllPosition(false)
        lockedAppAdapter.count = 0
        updateBtnLock()
        updateCheckboxState(appList)
        lockedAppAdapter.setNewList(appList)
    }

    // Khởi tạo giao diện
    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentLockedAppsBinding {
        return FragmentLockedAppsBinding.inflate(layoutInflater)
    }

    // Khởi tạo dữ liệu
    override fun initData() {
        CoroutineScope(Dispatchers.IO).launch {
            val lockedApps = AppInfoUtil.getLockedApp(requireContext())
            withContext(Dispatchers.Main) {
                viewModel.updateLockedApps(lockedApps.toMutableList())
            }
        }
    }

    // Thiết lập giao diện
    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            lockedAppAdapter = LockedAppAdapter(mutableListOf()) { clickedAppInfo ->
                lockedAppAdapter.updateSelectedPosition(clickedAppInfo)
                updateBtnLock()
                updateCheckboxState(lockedAppAdapter.getCurrentList())
            }
            recyclerView.adapter = lockedAppAdapter
            recyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

            viewModel.lockedApps.observe(viewLifecycleOwner) { apps ->
                val originalAnimator = recyclerView.itemAnimator
                recyclerView.itemAnimator = null
                lockedAppAdapter.setNewList(apps ?: mutableListOf())
                cbSelectAll.isEnabled = apps?.isNotEmpty() == true
                recyclerView.post { recyclerView.itemAnimator = originalAnimator }
                if (apps.isNullOrEmpty()) {
                    checkBox = false
                    lockedAppAdapter.updateAllPosition(false)
                    lockedAppAdapter.count = 0
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
                    val currentList = viewModel.lockedApps.value ?: mutableListOf()
                    AppInfoUtil.filterList(requireContext(), newText ?: "", currentList) { filteredList ->
                        lockedAppAdapter.setNewList(filteredList.toMutableList())
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
                if (currentTime - lastButtonClickTime > 1000 && lockedAppAdapter.count != 0) {
                    lastButtonClickTime = currentTime
                    val flagsSnapshot = lockedAppAdapter.booleanArray.copyOf()
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        val unlockedAppsList = mutableListOf<AppInfo>()
                        val lockedAppsList = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                val appToUnlock = lockedAppsList[i]
                                db.appInfoDAO().updateAppLockStatus(appToUnlock.packageName, false)
                                appToUnlock.isLocked = false
                                unlockedAppsList.add(appToUnlock)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            unlockedAppsList.forEach { appInfo ->
                                viewModel.addToAllApps(appInfo)
                            }
                            val currentLockedApps = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                            val appsToRemove = unlockedAppsList.map { it.packageName }
                            currentLockedApps.removeAll { appsToRemove.contains(it.packageName) }
                            currentLockedApps.sortBy { it.name }
                            viewModel.updateLockedApps(currentLockedApps)
                            lockedAppAdapter.setNewList(currentLockedApps)
                            lockedAppAdapter.count = 0
                            updateBtnLock()
                            updateCheckboxState(viewModel.lockedApps.value ?: mutableListOf())
                        }
                    }
                }
            }

            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000) {
                    lastButtonClickTime = currentTime
                    val currentList = lockedAppAdapter.getCurrentList()
                    if (!checkBox) {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_checked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.remove_all)
                        checkBox = true
                        lockedAppAdapter.updateAllPosition(true)
                        lockedAppAdapter.count = currentList.size
                        updateBtnLock()
                    } else {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_unchecked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.select_all)
                        checkBox = false
                        lockedAppAdapter.updateAllPosition(false)
                        lockedAppAdapter.count = 0
                        updateBtnLock()
                    }
                }
            }
        }
    }

    // Cập nhật giao diện nút mở khóa
    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            if (lockedAppAdapter.count != 0) {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_active_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tvLock.text = getString(R.string.unlock_with_count, lockedAppAdapter.count, getString(R.string.unlock))
            } else {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_inactive_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.hint_text))
                tvLock.text = getString(R.string.unlock)
            }
        }
    }

    // Cập nhật trạng thái checkbox
    private fun updateCheckboxState(appList: List<AppInfo>) {
        val allSelected = lockedAppAdapter.count == appList.size
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