package com.zzm.picturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * @author zhou
 * @Description: 图片标签分类列表视图
 * @createDate 2025/2/27上午10:12
 */
@Data
public class PictureTagCategory {
    /**
     * 标签列表
     */
    private List<String> tagList;
    /**
     * 分类列表
     */
    private List<String> categoryList;
}
