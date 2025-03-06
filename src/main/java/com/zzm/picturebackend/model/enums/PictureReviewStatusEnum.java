package com.zzm.picturebackend.model.enums;

import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 图片审核状态枚举类
 * 定义了图片审核的可能状态，包括待审核、通过和拒绝
 * 该枚举类还提供了一个根据状态值获取枚举实例的方法
 *
 * @author <a href="https://github.com/lieeew">leikooo</a>
 */
@Getter
public enum PictureReviewStatusEnum {
    /**
     * 待审核状态
     */
    REVIEWING("待审核", 0),
    /**
     * 审核通过状态
     */
    PASS("通过", 1),
    /**
     * 审核拒绝状态
     */
    REJECT("拒绝", 2);

    private final String text;

    private final int value;

    /**
     * 枚举值到枚举实例的映射，用于高效查找
     */
    private static final Map<Integer, PictureReviewStatusEnum> PICTURE_REVIEW_STATUS_ENUM_MAP =
            Arrays.stream(PictureReviewStatusEnum.values())
                    .collect(Collectors.toMap(PictureReviewStatusEnum::getValue, e -> e));

    /**
     * 构造函数，初始化枚举实例
     *
     * @param text  枚举的文本描述
     * @param value 枚举对应的状态值
     */
    PictureReviewStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据状态值获取对应的枚举实例
     * 如果传入的状态值为空或找不到对应的枚举实例，则抛出业务异常
     *
     * @param value 状态值
     * @return 对应的枚举实例
     * @throws BusinessException 如果状态值为空或无效
     */
    public static PictureReviewStatusEnum getEnumByValue(Integer value) {
        PictureReviewStatusEnum pictureReviewStatusEnum = value == null ? null : PICTURE_REVIEW_STATUS_ENUM_MAP.getOrDefault(value, null);
        ThrowUtils.throwIf(Objects.isNull(pictureReviewStatusEnum), new BusinessException(ErrorCode.PARAMS_ERROR));
        return pictureReviewStatusEnum;
    }
}
