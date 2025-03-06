package com.zzm.picturebackend.model.dto.picture;

import lombok.Data;

/**
 *批量导入图片请求
 *
 */
@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 名称前缀
     */
    private String namePrefix;

    /**
     * 抓取数量  
     */
    private Integer count = 10;

    /*private Integer page; // 新增分页参数

    private Integer offset; // 新增偏移量参数*/


    private static final long serialVersionUID = 1L;
}

