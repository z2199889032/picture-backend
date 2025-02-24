package com.zzm.picturebackend.exception;

import com.zzm.picturebackend.common.BaseResponse;
import com.zzm.picturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 用于统一处理项目中的异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     * 当控制器层抛出BusinessException时，该方法会捕获异常并返回具体的错误信息
     *
     * @param e 业务异常实例，包含错误代码和错误信息
     * @return 返回包含错误代码和错误信息的响应对象
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理运行时异常
     * 当控制器层抛出RuntimeException时，该方法会捕获异常并返回系统错误的响应
     *
     * @param e 运行时异常实例
     * @return 返回表示系统错误的响应对象
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
