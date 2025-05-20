package com.example.applock.screen.dialog

import android.view.View
import com.example.applock.R
import com.example.applock.base.BaseDialogFragment
import com.example.applock.databinding.DialogPermissionBinding
import com.example.applock.preference.MyPreferences
import com.example.applock.util.PermissionUtils

class PermissionDialog :
    BaseDialogFragment<DialogPermissionBinding>(DialogPermissionBinding::inflate) {
    private var numStar = 0
    var onGotoSettingClick: (() -> Unit)? = null
    var onToggleUsageClick: (() -> Unit)? = null
    var onToggleOverlayClick: (() -> Unit)? = null

    override fun onViewCreated() {
        binding.btnUsageToggle.setOnClickListener {
            onGotoSettingClick?.invoke()
        }
        binding.btnOverlayToggle.setOnClickListener {
            onToggleOverlayClick?.invoke()
        }
        binding.btnGoToSetting.setOnClickListener {
            onGotoSettingClick?.invoke()
        }
    }

    fun updateToggle() {
        binding.btnUsageToggle.setImageResource(
            if (PermissionUtils.checkUsageStatsPermission()) R.drawable.ic_toggle_inactive
            else R.drawable.ic_toggle_active
        )
        binding.btnOverlayToggle.setImageResource(
            if (PermissionUtils.checkOverlayPermission()) R.drawable.ic_toggle_inactive
            else R.drawable.ic_toggle_active
        )

        if (PermissionUtils.isAllPermissisionRequested()) {
            dismiss()
        }
    }
}