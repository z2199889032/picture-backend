package com.zzm.picturebackend.common;


import com.zzm.picturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;
/*
  通用返回类
  用于封装接口返回的数据，提供统一的响应格式
  @param <T> 返回数据的类型
*/
@Data
public class BaseResponse<T> implements Serializable {

    // 响应码，表示接口调用的结果状态
    private int code;

    // 响应数据，存放接口返回的具体内容
    private T data;

    // 响应消息，用于描述接口调用的附加信息
    private String message;

    /**
     * 构造方法，用于创建包含响应码、数据和消息的BaseResponse对象
     * @param code 响应码
     * @param data 响应数据
     * @param message 响应消息
     */
    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    /**
     * 构造方法，用于创建只包含响应码和数据的BaseResponse对象，消息默认为空字符串
     * @param code 响应码
     * @param data 响应数据
     */
    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    /**
     * 构造方法，用于根据ErrorCode枚举创建BaseResponse对象，数据为null
     * @param errorCode 错误码枚举，包含响应码和消息
     */
    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
