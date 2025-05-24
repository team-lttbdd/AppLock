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
import com.example.applock.util.AppLockConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Fragment hiển thị danh sách ứng dụng chưa khóa, cho phép chọn và khóa ứng dụng
class AllAppFragment : BaseFragment<FragmentAllAppsBinding>() {
    private lateinit var allAppAdapter: AllAppAdapter // Adapter cho RecyclerView
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
        val appList = viewModel.allApps.value ?: mutableListOf()
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
        val initialApps = AppInfoUtil.listAppInfo.filter { !it.isLocked }
        viewModel.updateAllApps(initialApps.toMutableList())
    }

    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            allAppAdapter = AllAppAdapter(mutableListOf()) { clickedAppInfo ->
                allAppAdapter.updateSelectedPosition(clickedAppInfo)
                updateBtnLock()
                updateCheckboxState(allAppAdapter.getCurrentList())
            }
            recyclerView.adapter = allAppAdapter

            viewModel.allApps.observe(viewLifecycleOwner) { apps ->
                allAppAdapter.setNewList(apps ?: mutableListOf())
                cbSelectAll.isEnabled = !apps.isNullOrEmpty()
                updateCheckboxState(apps ?: mutableListOf())
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
                if (currentTime - lastButtonClickTime > AppLockConfig.BUTTON_CLICK_DEBOUNCE_MS && allAppAdapter.count != 0) {
                    lastButtonClickTime = currentTime
                    val flagsSnapshot = allAppAdapter.booleanArray.copyOf()
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        val lockedAppsList = mutableListOf<AppInfo>()
                        val unlockedAppsList = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                val appToLock = unlockedAppsList[i]
                                viewModel.updateAppLockStatus(requireContext(), appToLock, true)
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
                            updateCheckboxState(currentAllApps)
                        }
                    }
                }
            }

            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > AppLockConfig.BUTTON_CLICK_DEBOUNCE_MS) {
                    lastButtonClickTime = currentTime
                    val currentList = allAppAdapter.getCurrentList()
                    checkBox = !checkBox
                    allAppAdapter.updateAllPosition(checkBox)
                    allAppAdapter.count = if (checkBox) currentList.size else 0
                    updateBtnLock()
                    updateCheckboxState(currentList)
                }
            }
        }
    }

    // Cập nhật giao diện nút khóa dựa trên số ứng dụng được chọn
    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            val isActive = allAppAdapter.count > 0
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
                "(${allAppAdapter.count}) ${getString(R.string.lock)}"
            } else {
                getString(R.string.lock)
            }
        }
    }

    // Cập nhật trạng thái checkbox dựa trên danh sách ứng dụng
    private fun updateCheckboxState(appList: List<AppInfo>) {
        val allSelected = allAppAdapter.count == appList.size && appList.isNotEmpty()
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