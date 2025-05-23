package com.example.applock.screen.home.locked_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.example.applock.databinding.ItemAppBinding
import com.example.applock.model.AppInfo

// Adapter hiển thị danh sách ứng dụng đã khóa trong LockedAppFragment
class LockedAppAdapter(
    private var lockedAppList: MutableList<AppInfo>, // Danh sách ứng dụng đã khóa
    private val onItemClick: (AppInfo) -> Unit // Callback khi click ứng dụng
) : RecyclerView.Adapter<LockedAppAdapter.AppItemViewHolder>() {

    // Mảng trạng thái chọn của ứng dụng
    internal var booleanArray = BooleanArray(lockedAppList.size)
    // Số ứng dụng được chọn
    internal var count = 0
    // Thời gian click cuối để ngăn click liên tục
    private var lastClickTime = 0L

    // Cập nhật trạng thái chọn của ứng dụng
    fun updateSelectedPosition(selectedAppInfo: AppInfo) {
        for (i in lockedAppList.indices) {
            if (lockedAppList[i].packageName == selectedAppInfo.packageName) {
                booleanArray[i] = !booleanArray[i]
                count += if (booleanArray[i]) 1 else -1
                notifyItemChanged(i)
                break
            }
        }
    }

    // Chọn hoặc bỏ chọn tất cả ứng dụng
    fun updateAllPosition(isSelected: Boolean) {
        for (i in booleanArray.indices) {
            booleanArray[i] = isSelected
            notifyItemChanged(i)
        }
        count = if (isSelected) booleanArray.size else 0
    }

    // Cập nhật danh sách ứng dụng mới
    fun setNewList(newList: List<AppInfo>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = lockedAppList.size
            override fun getNewListSize(): Int = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return lockedAppList[oldItemPosition].packageName == newList[newItemPosition].packageName
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return lockedAppList[oldItemPosition] == newList[newItemPosition]
            }
        })
        lockedAppList.clear()
        lockedAppList.addAll(newList)
        booleanArray = BooleanArray(lockedAppList.size)
        count = 0
        diffResult.dispatchUpdatesTo(this)
    }

    // Trả về danh sách hiện tại
    fun getCurrentList(): List<AppInfo> = lockedAppList.toList()

    // Tạo ViewHolder cho mục RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppItemViewHolder {
        val itemView = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppItemViewHolder(itemView)
    }

    // Gán dữ liệu cho ViewHolder
    override fun onBindViewHolder(holder: AppItemViewHolder, position: Int) {
        holder.itemView.translationX = 0f
        holder.bind(lockedAppList[position], position)
    }

    // Số lượng mục trong danh sách
    override fun getItemCount(): Int = lockedAppList.size

    // ViewHolder hiển thị thông tin ứng dụng
    inner class AppItemViewHolder(private val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo, position: Int) {
            binding.apply {
                imgAppIcon.setImageDrawable(app.icon)
                tvAppName.text = app.name
                tvAppName.setTextColor(
                    if (booleanArray[position]) ContextCompat.getColor(itemView.context, R.color.white)
                    else ContextCompat.getColor(itemView.context, R.color.dark_blue)
                )
                itemView.setBackgroundResource(
                    if (booleanArray[position]) R.drawable.bg_selected_language_item else R.drawable.bg_language_item
                )
                itemView.setOnClickListener {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > 800) {
                        lastClickTime = currentTime
                        onItemClick(app)
                    }
                }
            }
        }
    }
}