package com.example.applock.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import  com.example.applock.util.LanguageUtil

abstract class BaseDialogFragment<BINDING : ViewBinding>(private val layoutInflater: (LayoutInflater, ViewGroup?, Boolean) -> BINDING) : DialogFragment() {

    lateinit var binding: BINDING

    abstract fun onViewCreated()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageUtil.setLanguage(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = layoutInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewCreated()
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            // 1. Không cho dismiss khi bấm phím Back
            dialog.setCancelable(false)
            // 2. Cho phép dismiss khi bấm ra ngoài vùng dialog
            dialog.setCanceledOnTouchOutside(true)

            // Lấy đối tượng Window của dialog để chỉnh thêm
            dialog.window?.let { window ->
                // 3. Mờ nền phía sau dialog với hệ số alpha = 0.7
                window.setDimAmount(0.7f)
                // 4. Đặt nền window trong suốt (bỏ mặc định shape/background)
                window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                // 5. Tính chiều rộng dialog = 85% chiều rộng màn hình
                val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
                // 6. Áp kích thước: width = 85% màn hình, height = wrap_content (-2)
                window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        }
    }
}