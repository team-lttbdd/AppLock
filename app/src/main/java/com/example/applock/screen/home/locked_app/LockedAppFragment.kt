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
import com.example.applock.preference.MyPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.DefaultItemAnimator

// Fragment hiển thị danh sách ứng dụng đã khóa, cho phép chọn và mở khóa ứng dụng
class LockedAppFragment : BaseFragment<FragmentLockedAppsBinding>() {
    private lateinit var lockedAppAdapter: LockedAppAdapter // Adapter cho RecyclerView hiển thị danh sách ứng dụng
    private var checkBox: Boolean = false // Trạng thái checkbox chọn tất cả
    private var lastButtonClickTime = 0L // Thời gian click nút cuối cùng, dùng để debounce
    private var _viewModel: AppLockViewModel? = null // ViewModel lưu trữ dữ liệu ứng dụng
    private val viewModel: AppLockViewModel
        get() = _viewModel ?: throw IllegalStateException("ViewModel not initialized") // Truy cập ViewModel an toàn

    // Gán ViewModel từ bên ngoài (do FragmentPageAdapter cung cấp)
    fun setViewModel(viewModel: AppLockViewModel) {
        _viewModel = viewModel
    }

    // Đặt lại trạng thái và giao diện khi Fragment hiển thị lại
    override fun onResume() {
        super.onResume()
        val appList = viewModel.lockedApps.value ?: mutableListOf() // Lấy danh sách ứng dụng đã khóa
        checkBox = false // Đặt lại checkbox chọn tất cả
        lockedAppAdapter.updateAllPosition(false) // Bỏ chọn tất cả ứng dụng
        lockedAppAdapter.count = 0 // Đặt lại số lượng ứng dụng được chọn
        updateBtnLock() // Cập nhật giao diện nút mở khóa
        updateCheckboxState(appList) // Cập nhật trạng thái checkbox
        lockedAppAdapter.setNewList(appList) // Cập nhật danh sách trong adapter
    }

    // Khởi tạo giao diện Fragment
    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentLockedAppsBinding {
        return FragmentLockedAppsBinding.inflate(layoutInflater)
    }

    // Khởi tạo dữ liệu ban đầu
    override fun initData() {
        // AppInfoUtil.initInstalledApps(requireContext()) // Tải danh sách ứng dụng từ hệ thống - Đã di chuyển sang SplashActivity
        // Tải danh sách ứng dụng đã khóa từ database bất đồng bộ
        CoroutineScope(Dispatchers.IO).launch {
            val lockedApps = AppInfoUtil.getLockedApp(requireContext())
            withContext(Dispatchers.Main) {
                viewModel.updateLockedApps(lockedApps.toMutableList()) // Cập nhật vào ViewModel
            }
        }
    }

    // Thiết lập giao diện và các thành phần UI
    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext()) // Đặt LinearLayoutManager cho RecyclerView
            // Khởi tạo adapter với callback xử lý click ứng dụng
            lockedAppAdapter = LockedAppAdapter(mutableListOf()) { clickedAppInfo ->
                lockedAppAdapter.updateSelectedPosition(clickedAppInfo) // Cập nhật trạng thái chọn
                updateBtnLock() // Cập nhật nút mở khóa
                updateCheckboxState(lockedAppAdapter.getCurrentList()) // Cập nhật checkbox
            }
            recyclerView.adapter = lockedAppAdapter
            recyclerView.itemAnimator = DefaultItemAnimator() // Sử dụng DefaultItemAnimator cho hiệu ứng

            // Theo dõi thay đổi danh sách ứng dụng từ ViewModel
            viewModel.lockedApps.observe(viewLifecycleOwner) { apps ->
                // Tắt animator để tránh hiệu ứng khi cập nhật danh sách
                val originalAnimator = recyclerView.itemAnimator
                recyclerView.itemAnimator = null

                lockedAppAdapter.setNewList(apps ?: mutableListOf()) // Cập nhật danh sách ứng dụng
                cbSelectAll.isEnabled = (apps?.isNotEmpty() == true) // Kích hoạt checkbox nếu có ứng dụng

                // Khôi phục animator sau khi cập nhật
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

            searchBar.clearFocus() // Xóa tiêu điểm thanh tìm kiếm
            // Xử lý tìm kiếm theo thời gian thực
            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    val currentList = viewModel.lockedApps.value ?: mutableListOf()
                    AppInfoUtil.filterList(requireContext(), newText ?: "", currentList) { filteredList ->
                        lockedAppAdapter.setNewList(filteredList.toMutableList()) // Cập nhật danh sách lọc
                        updateCheckboxState(filteredList) // Cập nhật checkbox
                    }
                    return true
                }
            })
        }
    }

    // Xử lý các sự kiện click
    override fun handleEvent() {
        binding.apply {
            // Xử lý click nút mở khóa
            btnLock.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000 && lockedAppAdapter.count != 0) { // Debounce 1000ms
                    lastButtonClickTime = currentTime
                    val flagsSnapshot = lockedAppAdapter.booleanArray.copyOf()

                    // Cập nhật trạng thái mở khóa trong database
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        val unlockedAppsList = mutableListOf<AppInfo>() // Danh sách ứng dụng sẽ mở khóa
                        val lockedAppsList = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                        val removedPositions = mutableListOf<Int>()

                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                val appToUnlock = lockedAppsList[i]
                                db.appInfoDAO().updateAppLockStatus(appToUnlock.packageName, false)
                                appToUnlock.isLocked = false
                                unlockedAppsList.add(appToUnlock) // Thêm vào danh sách mở khóa
                                removedPositions.add(i) // Ghi lại vị trí xóa
                            }
                        }

                        withContext(Dispatchers.Main) {
                            // Cập nhật danh sách chưa khóa trong ViewModel
                            unlockedAppsList.forEach { appInfo ->
                                viewModel.addToAllApps(appInfo)
                            }

                            // Xóa ứng dụng đã mở khóa khỏi danh sách đã khóa
                            val currentLockedApps = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                            val appsToRemove = unlockedAppsList.map { it.packageName }
                            currentLockedApps.removeAll { appsToRemove.contains(it.packageName) }
                            currentLockedApps.sortBy { it.name } // Sắp xếp theo tên
                            viewModel.updateLockedApps(currentLockedApps)

                            // Cập nhật adapter
                            lockedAppAdapter.setNewList(currentLockedApps)
                            lockedAppAdapter.count = 0
                            updateBtnLock()
                            updateCheckboxState(viewModel.lockedApps.value ?: mutableListOf())
                        }
                    }
                }
            }

            // Xử lý click checkbox chọn tất cả
            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000) { // Debounce 1000ms
                    lastButtonClickTime = currentTime
                    val currentList = lockedAppAdapter.getCurrentList()
                    if (!checkBox) {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_checked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.remove_all)
                        checkBox = true
                        lockedAppAdapter.updateAllPosition(true) // Chọn tất cả
                        lockedAppAdapter.count = currentList.size
                        updateBtnLock()
                    } else {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_unchecked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.select_all)
                        checkBox = false
                        lockedAppAdapter.updateAllPosition(false) // Bỏ chọn tất cả
                        lockedAppAdapter.count = 0
                        updateBtnLock()
                    }
                }
            }
        }
    }

    // Cập nhật giao diện nút mở khóa dựa trên số lượng ứng dụng được chọn
    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            if (lockedAppAdapter.count != 0) {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_active_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tvLock.text = "(${lockedAppAdapter.count}) ${getString(R.string.unlock)}"
            } else {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_inactive_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.hint_text))
                tvLock.text = "${getString(R.string.unlock)}"
            }
        }
    }

    // Cập nhật trạng thái checkbox dựa trên danh sách ứng dụng
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