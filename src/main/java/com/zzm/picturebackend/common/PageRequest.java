package com.zzm.picturebackend.common;

import lombok.Data;

/**
 * 分页请求参数封装类，用于指定分页查询的页号、页面大小以及排序信息。
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}

