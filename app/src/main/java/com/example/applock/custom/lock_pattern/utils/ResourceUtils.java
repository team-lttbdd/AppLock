package com.example.applock.custom.lock_pattern.utils;

import static androidx.core.view.ViewCompat.setBackground;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

/**
 * Lớp tiện ích để truy xuất tài nguyên (resource) trong Android một cách dễ dàng.
 */
public class ResourceUtils {

    // Constructor private để ngăn tạo instance – chỉ dùng các phương thức static
    private ResourceUtils() {
        throw new AssertionError("Không thể khởi tạo lớp này. Hãy sử dụng các phương thức static.");
    }

    /**
     * Lấy mã màu từ resource ID
     *
     * @param context  Ngữ cảnh ứng dụng (thường là Activity, Application...)
     * @param colorRes ID của màu trong file resources (ví dụ: R.color.primaryColor)
     * @return Giá trị màu đã được giải mã (int)
     */
    public static int getColor(@NonNull Context context, @ColorRes int colorRes) {
        return ContextCompat.getColor(context, colorRes);
    }

    /**
     * Lấy chuỗi (string) từ resource ID
     *
     * @param context   Ngữ cảnh ứng dụng
     * @param stringRes ID của chuỗi trong file resources (ví dụ: R.string.app_name)
     * @return Nội dung chuỗi
     */
    public static String getString(@NonNull Context context, @StringRes int stringRes) {
        return context.getString(stringRes);
    }

    /**
     * Lấy giá trị kích thước (dimension) tính bằng pixel từ resource ID
     *
     * @param context  Ngữ cảnh ứng dụng
     * @param dimenRes ID của giá trị dimension (ví dụ: R.dimen.margin_16dp)
     * @return Giá trị kích thước đã chuyển sang pixel
     */
    public static float getDimensionInPx(@NonNull Context context, @DimenRes int dimenRes) {
        return context.getResources().getDimension(dimenRes);
    }
}

