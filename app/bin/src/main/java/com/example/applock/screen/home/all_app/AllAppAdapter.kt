package com.example.applock.screen.home.all_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.R
import com.example.applock.databinding.ItemAppBinding
import com.example.applock.model.AppInfo
import com.example.applock.util.AppLockConfig

// Adapter để hiển thị danh sách ứng dụng trong AllAppFragment
class AllAppAdapter(
    private var appList: MutableList<AppInfo>, // Danh sách ứng dụng hiển thị
    private val onItemClick: (AppInfo) -> Unit // Callback khi click vào ứng dụng
) : RecyclerView.Adapter<AllAppAdapter.AppItemViewHolder>() {

    // Mảng lưu trạng thái chọn của từng ứng dụng
    internal var booleanArray = BooleanArray(appList.size)
    // Đếm số ứng dụng được chọn
    internal var count = 0
    // Thời gian click cuối cùng để ngăn click liên tục
    private var lastClickTime = 0L

    // Cập nhật trạng thái chọn của một ứng dụng
    fun updateSelectedPosition(selectedAppInfo: AppInfo) {
        val index = appList.indexOfFirst { it.packageName == selectedAppInfo.packageName }
        if (index != -1) {
            booleanArray[index] = !booleanArray[index]
            count += if (booleanArray[index]) 1 else -1
            notifyItemChanged(index) // Cập nhật giao diện cho mục được thay đổi
        }
    }

    // Chọn hoặc bỏ chọn tất cả ứng dụng
    fun updateAllPosition(isSelected: Boolean) {
        booleanArray.fill(isSelected)
        count = if (isSelected) booleanArray.size else 0
        notifyDataSetChanged() // Cập nhật toàn bộ giao diện
    }

    // Cập nhật danh sách ứng dụng mới
    fun setNewList(newList: List<AppInfo>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = appList.size
            override fun getNewListSize(): Int = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return appList[oldItemPosition].packageName == newList[newItemPosition].packageName
            }
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return appList[oldItemPosition] == newList[newItemPosition]
            }
        })
        appList.clear()
        appList.addAll(newList)
        booleanArray = BooleanArray(appList.size) // Đặt lại mảng trạng thái
        count = 0 // Đặt lại số lượng chọn
        diffResult.dispatchUpdatesTo(this) // Cập nhật giao diện hiệu quả
    }

    // Trả về bản sao danh sách hiện tại
    fun getCurrentList(): List<AppInfo> = appList.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppItemViewHolder {
        val itemView = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppItemViewHolder, position: Int) {
        holder.itemView.translationX = 0f // Đặt lại vị trí ban đầu
        holder.bind(appList[position], position)
    }

    override fun getItemCount(): Int = appList.size

    // ViewHolder để hiển thị thông tin ứng dụng
    inner class AppItemViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo, position: Int) {
            binding.apply {
                imgAppIcon.setImageDrawable(app.icon) // Hiển thị biểu tượng ứng dụng
                tvAppName.text = app.name // Hiển thị tên ứng dụng
                // Đặt màu chữ dựa trên trạng thái chọn
                tvAppName.setTextColor(
                    ContextCompat.getColor(
                        itemView.context,
                        if (booleanArray[position]) R.color.white else R.color.text_color
                    )
                )
                // Đặt nền dựa trên trạng thái chọn
                itemView.setBackgroundResource(
                    if (booleanArray[position]) R.drawable.bg_selected_language_item else R.drawable.bg_language_item
                )
                // Xử lý sự kiện click với debounce
                itemView.setOnClickListener {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime > AppLockConfig.ITEM_CLICK_DEBOUNCE_MS) {
                        lastClickTime = currentTime
                        onItemClick(app)
                    }
                }
            }
        }
    }
}