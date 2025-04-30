package com.example.applock.custom.lock_pattern.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Lớp chứa các hàm tiện ích tạo số ngẫu nhiên.
 */
public class RandomUtils {

    // Đối tượng Random dùng chung cho toàn bộ lớp
    private static final Random RANDOM = new Random();

    // Constructor private để ngăn tạo đối tượng từ bên ngoài
    private RandomUtils() {
        throw new AssertionError("Không thể khởi tạo lớp này. Hãy dùng các hàm static.");
    }

    /**
     * Tạo ra một số nguyên ngẫu nhiên.
     * @return Một số nguyên trong khoảng [0, Integer.MAX_VALUE)
     */
    public static int randInt() {
        return RANDOM.nextInt((int) (System.nanoTime() % Integer.MAX_VALUE));
    }

    /**
     * Tạo ra một số nguyên ngẫu nhiên trong khoảng [0, max)
     * @param max Giá trị tối đa (không bao gồm)
     * @return Một số nguyên ngẫu nhiên, hoặc 0 nếu max <= 0
     */
    public static int randInt(int max) {
        return max > 0 ? randInt() % max : 0;
    }

    /**
     * Tạo ra một mảng số nguyên ngẫu nhiên không trùng nhau,
     * chứa tất cả các số từ start đến end - 1, và được xáo trộn thứ tự.
     *
     * @param start Giá trị bắt đầu (bao gồm)
     * @param end Giá trị kết thúc (không bao gồm)
     * @return Mảng số nguyên ngẫu nhiên, hoặc mảng rỗng nếu end <= start
     */
    public static int[] randIntArray(int start, int end) {
        if (end <= start) {
            return new int[0];
        }

        final List<Integer> values = new ArrayList<>();
        // Thêm các số từ start đến end - 1 vào danh sách
        for (int i = start; i < end; i++) {
            values.add(i);
        }

        // Mảng kết quả
        final int[] result = new int[values.size()];
        for (int i = 0; i < result.length; i++) {
            // Chọn ngẫu nhiên một phần tử trong danh sách
            int k = randInt(values.size());
            result[i] = values.get(k);
            values.remove(k); // Đảm bảo không trùng lặp
        }

        return result;
    }

    /**
     * Giống như hàm trên, nhưng mặc định bắt đầu từ 0.
     * Tạo mảng số ngẫu nhiên từ 0 đến end - 1.
     *
     * @param end Giá trị kết thúc (không bao gồm)
     * @return Mảng số nguyên ngẫu nhiên
     */
    public static int[] randIntArray(int end) {
        return randIntArray(0, end);
    }

}
