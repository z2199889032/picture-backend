package com.zzm.picturebackend.controller;

import com.zzm.picturebackend.annotation.AuthCheck;
import com.zzm.picturebackend.common.BaseResponse;
import com.zzm.picturebackend.common.ResultUtils;
import com.zzm.picturebackend.constant.UserConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }

}
