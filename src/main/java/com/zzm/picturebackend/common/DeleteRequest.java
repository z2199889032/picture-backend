package com.zzm.picturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 表示删除操作的请求对象，包含要删除的实体的唯一标识符。
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * 要删除的实体的唯一标识符。
     */
    private Long id;

    /**
     * 序列化版本的唯一标识符，用于确保序列化兼容性。
     */
    private static final long serialVersionUID = 1L;
}
