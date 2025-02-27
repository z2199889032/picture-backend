package com.zzm.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;
/*
* 用户注册请求*/
@Data
public class UserRegisterRequest implements Serializable {

    /**用于序列化和反序列化过程中标识类的版本
    当类的结构发生变化时，变更此值可以确保类的实例能够正确地被反序列化
    */
    private static final long serialVersionUID = 1929384300247735177L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
