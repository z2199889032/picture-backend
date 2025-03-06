package com.zzm.picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片审核请求类
 * 用于处理图片审核相关的请求
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * 图片的唯一标识符
     */
    private Long id;

    /**
     * 审核状态
     * 0-待审核, 1-通过, 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     * 用于记录审核过程中的相关信息或拒绝原因
     */
    private String reviewMessage;


    // 序列化ID，用于序列化兼容性
    private static final long serialVersionUID = 1L;
}
