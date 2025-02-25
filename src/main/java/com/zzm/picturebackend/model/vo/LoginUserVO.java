package com.zzm.picturebackend.model.vo;

/**
 * @author zhou
 * @Description:
 * @createDate 2025/2/24下午11:23
 */

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已登录用户视图对象(脱敏)
 */
@Data
public class LoginUserVO implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;


    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;



    private static final long serialVersionUID = 1L;

}
