package com.example.applock.screen.home.all_app

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
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

// Fragment hiển thị danh sách ứng dụng chưa khóa, cho phép chọn và khóa ứng dụng
class AllAppFragment : BaseFragment<FragmentAllAppsBinding>() {
    private lateinit var allAppAdapter: AllAppAdapter // Adapter cho RecyclerView hiển thị danh sách ứng dụng
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
        val appList = viewModel.allApps.value ?: mutableListOf() // Lấy danh sách ứng dụng chưa khóa

        // Tắt animator để tránh hiệu ứng khi cập nhật danh sách
        val originalAnimator = binding.recyclerView.itemAnimator
        binding.recyclerView.itemAnimator = null

        checkBox = false // Đặt lại checkbox chọn tất cả
        allAppAdapter.updateAllPosition(false) // Bỏ chọn tất cả ứng dụng
        allAppAdapter.count = 0 // Đặt lại số lượng ứng dụng được chọn
        updateBtnLock() // Cập nhật giao diện nút khóa
        updateCheckboxState(appList) // Cập nhật trạng thái checkbox
        allAppAdapter.setNewList(appList) // Cập nhật danh sách trong adapter

        // Khôi phục animator sau khi cập nhật
        binding.recyclerView.post { binding.recyclerView.itemAnimator = originalAnimator }
    }

    // Khởi tạo giao diện Fragment
    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentAllAppsBinding {
        return FragmentAllAppsBinding.inflate(layoutInflater)
    }

    // Khởi tạo dữ liệu ban đầu
    override fun initData() {
        AppInfoUtil.initInstalledApps(requireContext()) // Tải danh sách ứng dụng từ hệ thống
        val initialApps = AppInfoUtil.listAppInfo.filter { !it.isLocked } // Lọc ứng dụng chưa khóa
        viewModel.updateAllApps(initialApps.toMutableList()) // Cập nhật vào ViewModel
    }

    // Thiết lập giao diện và các thành phần UI
    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext()) // Đặt LinearLayoutManager cho RecyclerView
            // Khởi tạo adapter với callback xử lý click ứng dụng
            allAppAdapter = AllAppAdapter(mutableListOf()) { clickedAppInfo ->
                allAppAdapter.updateSelectedPosition(clickedAppInfo) // Cập nhật trạng thái chọn
                updateBtnLock() // Cập nhật nút khóa
                updateCheckboxState(allAppAdapter.getCurrentList()) // Cập nhật checkbox
            }
            recyclerView.adapter = allAppAdapter
            recyclerView.itemAnimator = DefaultItemAnimator() // Sử dụng DefaultItemAnimator cho hiệu ứng

            // Theo dõi thay đổi danh sách ứng dụng từ ViewModel
            viewModel.allApps.observe(viewLifecycleOwner) { apps ->
                // Tắt animator để tránh hiệu ứng khi cập nhật danh sách
                val originalAnimator = recyclerView.itemAnimator
                recyclerView.itemAnimator = null

                allAppAdapter.setNewList(apps ?: mutableListOf()) // Cập nhật danh sách ứng dụng
                cbSelectAll.isEnabled = (apps?.isNotEmpty() == true) // Kích hoạt checkbox nếu có ứng dụng

                // Khôi phục animator sau khi cập nhật
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

            searchBar.clearFocus() // Xóa tiêu điểm thanh tìm kiếm
            // Xử lý tìm kiếm theo thời gian thực
            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    val currentList = viewModel.allApps.value ?: mutableListOf()
                    AppInfoUtil.filterList(requireContext(), newText ?: "", currentList) { filteredList ->
                        allAppAdapter.setNewList(filteredList.toMutableList()) // Cập nhật danh sách lọc
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
            // Xử lý click nút khóa
            btnLock.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000 && allAppAdapter.count != 0) { // Debounce 1000ms
                    lastButtonClickTime = currentTime
                    val flagsSnapshot = allAppAdapter.booleanArray.copyOf()

                    // Cập nhật trạng thái khóa trong database
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        val lockedAppsList = mutableListOf<AppInfo>() // Danh sách ứng dụng sẽ khóa
                        val unlockedAppsList = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                        val removedPositions = mutableListOf<Int>()

                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                val appToLock = unlockedAppsList[i]
                                db.appInfoDAO().updateAppLockStatus(appToLock.packageName, true)
                                appToLock.isLocked = true
                                lockedAppsList.add(appToLock) // Thêm vào danh sách khóa
                                removedPositions.add(i) // Ghi lại vị trí xóa
                            }
                        }

                        withContext(Dispatchers.Main) {
                            // Cập nhật danh sách đã khóa trong ViewModel
                            viewModel.updateLockedApps((viewModel.lockedApps.value ?: mutableListOf()).apply { addAll(lockedAppsList) })

                            // Xóa ứng dụng đã khóa khỏi danh sách chưa khóa
                            val currentAllApps = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                            val appsToRemove = lockedAppsList.map { it.packageName }
                            currentAllApps.removeAll { appsToRemove.contains(it.packageName) }
                            currentAllApps.sortBy { it.name } // Sắp xếp theo tên
                            viewModel.updateAllApps(currentAllApps)

                            // Cập nhật adapter
                            allAppAdapter.setNewList(currentAllApps)
                            allAppAdapter.count = 0
                            updateBtnLock()
                            updateCheckboxState(viewModel.allApps.value ?: mutableListOf())
                        }
                    }
                }
            }

            // Xử lý click checkbox chọn tất cả
            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000) { // Debounce 1000ms
                    lastButtonClickTime = currentTime
                    val currentList = allAppAdapter.getCurrentList()
                    if (!checkBox) {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_checked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.remove_all)
                        checkBox = true
                        allAppAdapter.updateAllPosition(true) // Chọn tất cả
                        allAppAdapter.count = currentList.size
                        updateBtnLock()
                    } else {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_unchecked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.select_all)
                        checkBox = false
                        allAppAdapter.updateAllPosition(false) // Bỏ chọn tất cả
                        allAppAdapter.count = 0
                        updateBtnLock()
                    }
                }
            }
        }
    }

    // Cập nhật giao diện nút khóa dựa trên số lượng ứng dụng được chọn
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

    // Cập nhật trạng thái checkbox dựa trên danh sách ứng dụng
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