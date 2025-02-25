package com.zzm.picturebackend.exception;

import lombok.Getter;

/**
 * 自定义业务异常类
 * 用于处理业务逻辑中的异常情况，提供了错误码和错误信息
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 构造函数，用于创建带有错误码和错误信息的异常对象
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造函数，用于创建带有错误码和错误信息的异常对象
     * 使用ErrorCode枚举类来统一错误码和错误信息
     *
     * @param errorCode 错误码枚举对象
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    /**
     * 构造函数，用于创建带有错误码和自定义错误信息的异常对象
     * 使用ErrorCode枚举类来统一错误码，并允许错误信息的自定义
     *
     * @param errorCode 错误码枚举对象
     * @param message   自定义错误信息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

}
