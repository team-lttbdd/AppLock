package com.example.applock.screen.home.all_app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.applock.databinding.ItemAppBinding
import com.example.applock.model.AppInfo

class AllAppAdapter(
    var appList: List<AppInfo>,
    private val onItemClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AllAppAdapter.AppItemViewHolder>() {

    private lateinit var itemView: ItemAppBinding

    private var lastClickTime: Long = 0L
    private val CLICK_INTERVAL = 800L

    fun setNewList(newList: List<AppInfo>) {
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = appList.size

            override fun getNewListSize(): Int = newList.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return appList[oldItemPosition].packageName ==
                        newList[newItemPosition].packageName
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return appList[oldItemPosition] == newList[newItemPosition]
            }
        })

        appList = newList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AppItemViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(app: AppInfo) {
            binding.apply {
                imgAppIcon.setImageDrawable(app.icon)
                tvAppName.text = app.name
                itemView.setOnClickListener {
                    // Check the time interval before processing the click
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastClickTime >= CLICK_INTERVAL) {
                        lastClickTime = currentTime
                        app.isLocked = true
                        onItemClick(app)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppItemViewHolder {
        itemView = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppItemViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: AppItemViewHolder, position: Int) {
        holder.itemView.translationX = 0f
        val app = appList[position]
        holder.bind(app)
    }

    override fun getItemCount(): Int = appList.size

    inner class SlideOutRightItemAnimator : DefaultItemAnimator() {
        override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
            val view = holder.itemView
            view.animate()
                .translationX(view.width.toFloat())
                .setDuration(300)
                .withEndAction {
                    dispatchRemoveFinished(holder)
                }
                .start()
            return true
        }

        override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
            val view = holder.itemView
            view.translationX = view.width.toFloat()
            view.animate()
                .translationX(0f)
                .setDuration(300)
                .withEndAction {
                    dispatchAddFinished(holder)
                }
                .start()
            return true
        }
    }
}

