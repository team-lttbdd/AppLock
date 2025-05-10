package com.example.applock.screen.setting

import android.view.LayoutInflater
import com.example.applock.base.BaseActivity
import com.example.applock.databinding.ActivitySettingBinding


class SettingActivity : BaseActivity<ActivitySettingBinding>() {
    override fun getViewBinding(layoutInflater: LayoutInflater): ActivitySettingBinding {
        return ActivitySettingBinding.inflate(layoutInflater)
    }

    override fun initData() {

    }

    override fun setupView() {

    }

    override fun handleEvent() {
        binding.apply {
            imgToggle1.setOnClickListener {
                imgToggle1.startAnim {

                }
            }

            imgToggle2.setOnClickListener {
                imgToggle2.startAnim {

                }
            }
            imgToggle3.setOnClickListener {
                imgToggle3.startAnim {

                }
            }
            imgChevron1.setOnClickListener {
                imgChevron1.startAnim {

                }
            }

            imgChevron3.setOnClickListener {
                imgChevron3.startAnim {

                }
            }
        }
    }
}