package com.example.applock.screen.home.locked_app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.example.applock.databinding.ItemAppBinding
import com.example.applock.model.AppInfo

// Adapter để hiển thị danh sách ứng dụng đã khóa trong LockedAppFragment
class LockedAppAdapter(
    private var lockedAppList: MutableList<AppInfo>, // Danh sách ứng dụng đã khóa
    private val onItemClick: (AppInfo) -> Unit // Callback khi click vào ứng dụng
) : RecyclerView.Adapter<LockedAppAdapter.AppItemViewHolder>() {

    // Mảng lưu trạng thái chọn của từng ứng dụng
    internal var booleanArray = BooleanArray(lockedAppList.size)
    // Đếm số ứng dụng được chọn
    internal var count = 0
    // Thời gian click cuối cùng để ngăn click liên tục
    private var lastClickTime = 0L

    // Cập nhật trạng thái chọn của một ứng dụng
    fun updateSelectedPosition(selectedAppInfo: AppInfo) {
        for (i in lockedAppList.indices) {
            if (lockedAppList[i].packageName == selectedAppInfo.packageName) {
                // Đảo trạng thái chọn và cập nhật số lượng
                booleanArray[i] = !booleanArray[i]
                count += if (booleanArray[i]) 1 else -1
                notifyItemChanged(i) // Cập nhật giao diện chỉ cho mục được thay đổi
                break
            }
        }
    }

    // Chọn hoặc bỏ chọn tất cả ứng dụng
    fun updateAllPosition(isSelected: Boolean) {
        for (i in booleanArray.indices) {
            booleanArray[i] = isSelected
            notifyItemChanged(i) // Cập nhật giao diện từng mục
        }
        // Cập nhật số lượng chọn
        count = if (isSelected) booleanArray.size else 0
    }

    // Cập nhật danh sách ứng dụng mới
    fun setNewList(newList: List<AppInfo>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = lockedAppList.size
            override fun getNewListSize(): Int = newList.size
            // So sánh packageName để xác định cùng một mục
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return lockedAppList[oldItemPosition].packageName == newList[newItemPosition].packageName
            }
            // So sánh toàn bộ nội dung để xác định thay đổi
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return lockedAppList[oldItemPosition] == newList[newItemPosition]
            }
        })
        lockedAppList.clear()
        lockedAppList.addAll(newList)
        booleanArray = BooleanArray(lockedAppList.size) // Đặt lại mảng trạng thái
        count = 0 // Đặt lại số lượng chọn
        diffResult.dispatchUpdatesTo(this) // Cập nhật giao diện hiệu quả
    }

    // Trả về bản sao danh sách hiện tại
    fun getCurrentList(): List<AppInfo> {
        return lockedAppList.toList()
    }

    // Tạo ViewHolder cho mỗi mục trong RecyclerView
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppItemViewHolder {
        val itemView = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppItemViewHolder(itemView)
    }

    // Gán dữ liệu cho ViewHolder tại vị trí cụ thể
    override fun onBindViewHolder(holder: AppItemViewHolder, position: Int) {
        holder.itemView.translationX = 0f // Đặt lại vị trí ban đầu
        val app = lockedAppList[position]
        holder.bind(app, position)
    }

    // Trả về số lượng mục trong danh sách
    override fun getItemCount(): Int = lockedAppList.size

    // ViewHolder để hiển thị thông tin ứng dụng
    inner class AppItemViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo, position: Int) {
            binding.apply {
                imgAppIcon.setImageDrawable(app.icon) // Hiển thị biểu tượng ứng dụng
                tvAppName.text = app.name // Hiển thị tên ứng dụng
                // Đặt màu chữ dựa trên trạng thái chọn
                tvAppName.setTextColor(
                    if (booleanArray[position]) Color.parseColor("#FFFFFF") else Color.parseColor("#131936")
                )
                // Đặt nền dựa trên trạng thái chọn
                itemView.setBackgroundResource(
                    if (booleanArray[position]) R.drawable.bg_selected_language_item else R.drawable.bg_language_item
                )
                // Xử lý sự kiện click với debounce
                itemView.setOnClickListener {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > 800) { // Ngăn click liên tục trong 800ms
                        lastClickTime = currentTime
                        onItemClick(app)
                    }
                }
            }
        }
    }
}