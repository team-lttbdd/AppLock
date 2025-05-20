package com.example.applock.base


import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.example.applock.util.LanguageUtil

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

    @Suppress("OVERRIDE_DEPRECATION")
    @SuppressWarnings("deprecation")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        var viewParent = view
        while (viewParent is View) {
            viewParent.fitsSystemWindows = false
            viewParent.setOnApplyWindowInsetsListener { _, insets -> insets }
            viewParent = viewParent.parent as View?
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(true)
            dialog.window?.let { window ->
                window.setDimAmount(0.7f)
                window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
                window.setLayout(width, -2)
            }
        }
    }
}