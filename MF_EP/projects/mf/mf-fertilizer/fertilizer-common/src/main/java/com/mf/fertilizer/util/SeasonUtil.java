package com.mf.fertilizer.util;

import java.time.LocalDate;

public final class SeasonUtil {

    private SeasonUtil() {}

    /** 根据月份判断季节 */
    public static String currentSeason() {
        return seasonOf(LocalDate.now().getMonthValue());
    }

    /** 根据月份获取季节: 3-5春, 6-8夏, 9-11秋, 12-2冬 */
    public static String seasonOf(int month) {
        if (month >= 3 && month <= 5) return "spring";
        if (month >= 6 && month <= 8) return "summer";
        if (month >= 9 && month <= 11) return "autumn";
        return "winter";
    }
}
