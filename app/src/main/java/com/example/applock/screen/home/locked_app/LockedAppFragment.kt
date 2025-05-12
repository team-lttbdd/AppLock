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

class LockedAppFragment : BaseFragment<FragmentLockedAppsBinding>() {
    private lateinit var lockedAppAdapter: LockedAppAdapter
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
        viewModel.lockedApps.value?.let { lockedAppAdapter.setNewList(it) } ?: lockedAppAdapter.setNewList(mutableListOf())
    }

    override fun getViewBinding(layoutInflater: LayoutInflater): FragmentLockedAppsBinding {
        return FragmentLockedAppsBinding.inflate(layoutInflater)
    }

    override fun initData() {
        AppInfoUtil.initInstalledApps(requireContext())
        val initialLockedApps = AppInfoUtil.listLockedAppInfo
        viewModel.updateLockedApps(initialLockedApps.toMutableList())
    }

    override fun setupView() {
        binding.apply {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            lockedAppAdapter = LockedAppAdapter(mutableListOf()) { clickedAppInfo ->
                lockedAppAdapter.updateSelectedPosition(clickedAppInfo)
                updateBtnLock()
            }
            recyclerView.adapter = lockedAppAdapter
            recyclerView.itemAnimator = SlideOutRightItemAnimator()

            viewModel.lockedApps.observe(viewLifecycleOwner) { apps ->
                lockedAppAdapter.setNewList(apps ?: mutableListOf())
            }

            searchBar.clearFocus()
            searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    val currentList = viewModel.lockedApps.value ?: mutableListOf()
                    AppInfoUtil.filterList(requireContext(), newText ?: "", currentList) { filteredList ->
                        lockedAppAdapter.setNewList(filteredList.toMutableList())
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
                if (currentTime - lastButtonClickTime > 1000 && lockedAppAdapter.count != 0) {
                    lastButtonClickTime = currentTime
                    val transferList: MutableList<AppInfo> = mutableListOf()
                    val currentLockedApps = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()

                    val flagsSnapshot = lockedAppAdapter.booleanArray.copyOf()

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(requireContext())
                        for (i in flagsSnapshot.indices) {
                            if (flagsSnapshot[i]) {
                                db.appInfoDAO().updateAppLockStatus(currentLockedApps[i].packageName, false)
                                transferList.add(currentLockedApps[i])
                            }
                        }
                        withContext(Dispatchers.Main) {
                            viewModel.loadInitialData(requireContext())
                            lockedAppAdapter.count = 0
                            updateBtnLock()
                        }
                    }
                }
            }

            cbSelectAll.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastButtonClickTime > 1000) {
                    lastButtonClickTime = currentTime
                    if (!checkBox) {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_checked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.remove_all)
                        checkBox = true
                        lockedAppAdapter.updateAllPosition(true)
                        updateBtnLock()
                    } else {
                        cbSelectAll.setBackgroundResource(R.drawable.checkbox_unchecked)
                        tvSelectOrRemove.text = ContextCompat.getString(tvSelectOrRemove.context, R.string.select_all)
                        checkBox = false
                        lockedAppAdapter.updateAllPosition(false)
                        updateBtnLock()
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateBtnLock() {
        binding.apply {
            if (lockedAppAdapter.count != 0) {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_active_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                tvLock.text = "(${lockedAppAdapter.count}) Unlock"
            } else {
                btnLock.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.bg_inactive_button))
                tvLock.setTextColor(ContextCompat.getColor(requireContext(), R.color.hint_text))
                tvLock.text = "Unlock"
            }
        }
    }
}