package com.zzm.picturebackend.utils;

import java.awt.*;

/**
 * 工具类：计算颜色相似度
 */
public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    /**
     * 获取标准颜色
     * @param color
     * @return
     */
    public static String getStandardColor(String color) {
        if (color.length() == 7){
            color = color.substring(0,4) + "0" + color.substring(4,7);
        }
        return color;
    }

}
