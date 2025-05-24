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
import com.example.applock.util.AppLockConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Fragment hiển thị danh sách ứng dụng đã khóa, cho phép chọn và mở khóa ứng dụng
class LockedAppFragment : BaseFragment<FragmentLockedAppsBinding>() {
    private lateinit var lockedAppAdapter: LockedAppAdapter // Adapter cho RecyclerView
    private var checkBox: Boolean = false // Trạng thái checkbox chọn tất cả
    private var lastButtonClickTime = 0L // Thời gian click nút cuối cùng để debounce
    private var _viewModel: AppLockViewModel? = null // ViewModel lưu trữ dữ liệu ứng dụng
    private val viewModel: AppLockViewModel
        get() = _viewModel ?: throw IllegalStateException("ViewModel chưa được khởi tạo")

    // Gán ViewModel từ bên ngoài
    fun setViewModel(viewModel: AppLockViewModel) {
        _viewModel = viewModel
    }

    override fun onResume() {
        super.onResume()
        val appList = viewModel.lockedApps.value ?: mutableListOf()
        lockedAppAdapter.updateAllPosition(false)
        lockedAppAdapter.count = 0
        updateBtnLock()
        updateCheckboxState(appList)
        lockedAppAdapter.setNewList(appList)
    }

    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentLockedAppsBinding {
        return FragmentLockedAppsBinding.inflate(layoutInflater)
    }

    override fun initData() {
        CoroutineScope(Dispatchers.IO).launch {
            val lockedApps = AppInfoUtil.getLockedApp(requireContext())
            withContext(Dispatchers.Main) {
                viewModel.updateLockedApps(lockedApps.toMutableList())
            }
        }
    }

    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            lockedAppAdapter = LockedAppAdapter(mutableListOf()) { clickedAppInfo ->
                lockedAppAdapter.updateSelectedPosition(clickedAppInfo)
                updateBtnLock()
                updateCheckboxState(lockedAppAdapter.getCurrentList())
            }
            recyclerView.adapter = lockedAppAdapter

            viewModel.lockedApps.observe(viewLifecycleOwner) { apps ->
                lockedAppAdapter.setNewList(apps ?: mutableListOf())
                cbSelectAll.isEnabled = !apps.isNullOrEmpty()
                updateCheckboxState(apps ?: mutableListOf())
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

    override fun handleEvent() {
        binding.apply {
            btnLock.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > AppLockConfig.BUTTON_CLICK_DEBOUNCE_MS && lockedAppAdapter.count != 0) {
                    lastButtonClickTime = currentTime
                    val flagsSnapshot = lockedAppAdapter.booleanArray.copyOf()
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        val unlockedAppsList = mutableListOf<AppInfo>()
                        val lockedAppsList = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                val appToUnlock = lockedAppsList[i]
                                viewModel.updateAppLockStatus(requireContext(), appToUnlock, false)
                                unlockedAppsList.add(appToUnlock)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            unlockedAppsList.forEach { appInfo ->
                                viewModel.addToAllApps(appInfo)
                            }
                            // Thêm dòng này để đảm bảo làm mới dữ liệu
                            viewModel.refreshData()
                            val currentLockedApps = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                            val appsToRemove = unlockedAppsList.map { it.packageName }
                            currentLockedApps.removeAll { appsToRemove.contains(it.packageName) }
                            currentLockedApps.sortBy { it.name }
                            viewModel.updateLockedApps(currentLockedApps)
                            lockedAppAdapter.setNewList(currentLockedApps)
                            lockedAppAdapter.count = 0
                            updateBtnLock()
                            updateCheckboxState(currentLockedApps)
                        }
                    }
                }
            }

            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > AppLockConfig.BUTTON_CLICK_DEBOUNCE_MS) {
                    lastButtonClickTime = currentTime
                    val currentList = lockedAppAdapter.getCurrentList()
                    checkBox = !checkBox
                    lockedAppAdapter.updateAllPosition(checkBox)
                    lockedAppAdapter.count = if (checkBox) currentList.size else 0
                    updateBtnLock()
                    updateCheckboxState(currentList)
                }
            }
        }
    }

    // Cập nhật giao diện nút mở khóa dựa trên số ứng dụng được chọn
    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            val isActive = lockedAppAdapter.count > 0
            btnLock.setImageDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (isActive) R.drawable.bg_active_button else R.drawable.bg_inactive_button
                )
            )
            tvLock.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isActive) R.color.white else R.color.hint_text
                )
            )
            tvLock.text = if (isActive) {
                "(${lockedAppAdapter.count}) ${getString(R.string.unlock)}"
            } else {
                getString(R.string.unlock)
            }
        }
    }

    // Cập nhật trạng thái checkbox dựa trên danh sách ứng dụng
    private fun updateCheckboxState(appList: List<AppInfo>) {
        val allSelected = lockedAppAdapter.count == appList.size && appList.isNotEmpty()
        binding.apply {
            cbSelectAll.isEnabled = appList.isNotEmpty()
            cbSelectAll.setBackgroundResource(
                if (allSelected) R.drawable.checkbox_checked else R.drawable.checkbox_unchecked
            )
            tvSelectOrRemove.text = ContextCompat.getString(
                tvSelectOrRemove.context,
                if (allSelected) R.string.remove_all else R.string.select_all
            )
            checkBox = allSelected
        }
    }
}
