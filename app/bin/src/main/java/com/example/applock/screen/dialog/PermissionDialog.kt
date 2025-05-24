package com.example.applock.screen.dialog

import com.example.applock.R
import com.example.applock.base.BaseDialogFragment
import com.example.applock.databinding.DialogPermissionBinding
import com.example.applock.util.PermissionUtil

class PermissionDialog :
    BaseDialogFragment<DialogPermissionBinding>(DialogPermissionBinding::inflate) {

    var onGotoSettingClick: (() -> Unit)? = null
    var onToggleUsageClick: (() -> Unit)? = null
    var onToggleOverlayClick: (() -> Unit)? = null
    var onToggleDeviceAdminClick: (() -> Unit)? = null

    override fun onViewCreated() {
        binding.btnUsageToggle.setOnClickListener {
            onGotoSettingClick?.invoke()
        }
        binding.btnOverlayToggle.setOnClickListener {
            onToggleOverlayClick?.invoke()
        }
        binding.btnDeviceAdminToggle.setOnClickListener {
            onToggleDeviceAdminClick?.invoke()
        }
        binding.btnGoToSetting.setOnClickListener {
            onGotoSettingClick?.invoke()
        }
    }

    fun updateToggle() {
        binding.btnUsageToggle.setImageResource(
            if (PermissionUtil.checkUsageStatsPermission()) R.drawable.ic_toggle_inactive
            else R.drawable.ic_toggle_active
        )
        binding.btnOverlayToggle.setImageResource(
            if (PermissionUtil.checkOverlayPermission()) R.drawable.ic_toggle_inactive
            else R.drawable.ic_toggle_active
        )
        binding.btnDeviceAdminToggle.setImageResource(
            if (PermissionUtil.checkDeviceAdminPermission()) R.drawable.ic_toggle_inactive
            else R.drawable.ic_toggle_active
        )
        if (PermissionUtil.isAllPermissionRequested()) {
            dismiss()
        }
    }
}