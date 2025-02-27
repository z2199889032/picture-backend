package com.zzm.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zzm.picturebackend.model.dto.user.UserQueryRequest;
import com.zzm.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzm.picturebackend.model.vo.LoginUserVO;
import com.zzm.picturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author zhou
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-21 10:28:22
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      请求对象，用于获取登录信息
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword 明文密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param request 请求对象，用于获取当前用户信息
     * @return 当前登录用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user 用户实体对象
     * @return 脱敏后的用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户注销
     *
     * @param request 请求对象，用于处理用户注销
     * @return 注销成功返回true，否则返回false
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取用户脱敏信息
     *
     * @param user 脱敏前的信息
     * @return 脱敏后的信息
     */
    UserVO getUserVO(User user);

    /**
     * 批量获取用户脱敏信息
     *
     * @param userList 脱敏前的信息列表
     * @return 脱敏后的 List 列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest 查询条件请求对象
     * @return 查询条件对象
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);


}
