package com.zzm.picturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author zhou
 * @Description: 空间级别
 * @createDate 2025/3/11上午12:40
 */
@Data
@AllArgsConstructor
public class SpaceLevel {
    /**
     *值
     */
    private int value;
    /**
     *中文
     */
    private String text;
    /**
     *最大数量
     */
    private long maxCount;
    /**
     *最大容量
     */
    private long maxSize;
}
