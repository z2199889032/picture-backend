package com.zzm.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzm.picturebackend.config.CosClientConfig;
import com.zzm.picturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import com.zzm.picturebackend.annotation.AuthCheck;
import com.zzm.picturebackend.common.BaseResponse;
import com.zzm.picturebackend.common.DeleteRequest;
import com.zzm.picturebackend.common.ResultUtils;
import com.zzm.picturebackend.constant.UserConstant;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import com.zzm.picturebackend.model.dto.user.*;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.vo.LoginUserVO;
import com.zzm.picturebackend.model.vo.UserVO;
import com.zzm.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private CosManager cosManager;

    @Resource
    private UserService userService;

    @Resource
    protected CosClientConfig cosClientConfig;
    /**
     * 用户注册接口。
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 检查请求参数是否为空
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取用户账号
        String userAccount = userRegisterRequest.getUserAccount();

        // 获取用户密码
        String userPassword = userRegisterRequest.getUserPassword();

        // 获取确认密码
        String checkPassword = userRegisterRequest.getCheckPassword();

        // 调用服务层方法进行用户注册，并返回注册结果
        long result = userService.userRegister(userAccount, userPassword, checkPassword);

        // 返回成功响应，包含新用户的ID
        return ResultUtils.success(result);
    }

    /**
     * 用户登录接口。
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 检查请求参数是否为空
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取用户账号
        String userAccount = userLoginRequest.getUserAccount();

        // 获取用户密码
        String userPassword = userLoginRequest.getUserPassword();

        // 调用服务层方法进行用户登录，并返回登录结果
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);

        // 返回成功响应，包含登录用户信息
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息接口。
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 返回成功响应，包含当前登录用户信息
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销接口。
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 检查请求对象是否为空
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);

        // 调用服务层方法进行用户注销，并返回注销结果
        boolean result = userService.userLogout(request);

        // 返回成功响应，包含注销结果
        return ResultUtils.success(result);
    }

    /**
     * 创建用户接口（仅管理员）。
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 检查请求参数是否为空
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 创建新的用户对象
        User user = new User();

        // 将请求参数复制到用户对象中
        BeanUtils.copyProperties(userAddRequest, user);

        // 设置默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";

        // 获取加密后的默认密码
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);

        // 设置用户密码为加密后的默认密码
        user.setUserPassword(encryptPassword);

        // 调用服务层方法保存用户，并返回保存结果
        boolean result = userService.save(user);

        // 如果保存失败，抛出操作错误异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回成功响应，包含新创建用户的ID
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）。
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        // 检查用户ID是否合法
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 根据ID获取用户
        User user = userService.getById(id);

        // 如果用户不存在，抛出未找到错误异常
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);

        // 返回成功响应，包含用户信息
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类。
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        // 根据ID获取用户信息
        BaseResponse<User> response = getUserById(id);

        // 获取用户对象
        User user = response.getData();

        // 返回成功响应，包含用户封装对象
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户接口（仅管理员）。
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 检查删除请求参数是否为空或ID是否合法
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 调用服务层方法删除用户，并返回删除结果
        boolean b = userService.removeById(deleteRequest.getId());

        // 返回成功响应，包含删除结果
        return ResultUtils.success(b);
    }

    /**
     * 更新用户接口（仅管理员）。
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 检查更新请求参数是否为空或ID是否合法
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 创建新的用户对象
        User user = new User();

        // 将请求参数复制到用户对象中
        BeanUtils.copyProperties(userUpdateRequest, user);

        // 调用服务层方法更新用户，并返回更新结果
        boolean result = userService.updateById(user);

        // 如果更新失败，抛出操作错误异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回成功响应，包含更新结果
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表接口（仅管理员）。
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 检查查询请求参数是否为空
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取分页参数
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();

        // 调用服务层方法分页查询用户，并返回查询结果
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));

        // 创建分页对象用于返回用户封装列表
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());

        // 获取用户封装列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());

        // 设置分页结果中的记录列表
        userVOPage.setRecords(userVOList);

        // 返回成功响应，包含分页用户封装列表
        return ResultUtils.success(userVOPage);
    }

    /**
     * 管理员修改用户信息接口。
     */
    @PostMapping("/update/info")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUserInfo(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 检查更新请求参数是否为空或ID是否合法
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 创建新的用户对象
        User user = new User();

        // 将请求参数复制到用户对象中
        BeanUtils.copyProperties(userUpdateRequest, user);

        // 调用服务层方法更新用户，并返回更新结果
        boolean result = userService.updateById(user);

        // 如果更新失败，抛出操作错误异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回成功响应
        return ResultUtils.success(true);
    }

    /**
     * 用户修改个人信息接口。
     */
    @PostMapping("/update/my")
    public BaseResponse<Boolean> updateMyInfo(@RequestPart(value = "file", required = false) MultipartFile file,
                                             @RequestPart("userUpdateInfoRequest") UserUpdateInfoRequest userUpdateInfoRequest,
                                             HttpServletRequest request) {
        // 检查更新请求参数是否为空
        ThrowUtils.throwIf(userUpdateInfoRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 创建新的用户对象
        User user = new User();
        user.setId(loginUser.getId());

        // 设置要更新的字段
        user.setUserName(userUpdateInfoRequest.getUserName());

        // 如果上传了新的头像图片，处理文件上传
        if (file != null) {
            // 获取文件名
            String filename = file.getOriginalFilename();
            // 构建文件路径
            String filepath = String.format("/user/avatar/%s/%s", loginUser.getId(), filename);
            
            java.io.File tempFile = null;
            try {
                // 创建临时文件
                tempFile = java.io.File.createTempFile(filepath, null);
                // 将上传的文件内容写入临时文件
                file.transferTo(tempFile);
                // 上传文件到对象存储
                cosManager.putObject(filepath, tempFile);
                // 设置新的头像URL，添加域名前缀
                String fullAvatarUrl = cosClientConfig.getHost()  + filepath;
                user.setUserAvatar(fullAvatarUrl);
            } catch (Exception e) {
                log.error("avatar upload error, filepath = " + filepath, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像上传失败");
            } finally {
                if (tempFile != null) {
                    boolean delete = tempFile.delete();
                    if (!delete) {
                        log.error("temp file delete error, filepath = {}", filepath);
                    }
                }
            }
        } else {
            // 如果没有上传新图片，检查并处理请求中的头像URL
            String avatarUrl = userUpdateInfoRequest.getUserAvatar();
            if (avatarUrl != null && !avatarUrl.startsWith(cosClientConfig.getHost() )) {
                // 如果URL不包含域名前缀，则添加
                avatarUrl = cosClientConfig.getHost()  + avatarUrl;
            }
            user.setUserAvatar(avatarUrl);
        }

        // 如果提供了新密码，则更新密码
        String newPassword = userUpdateInfoRequest.getUserPassword();
        if (newPassword != null && !newPassword.isEmpty()) {
            String encryptPassword = userService.getEncryptPassword(newPassword);
            user.setUserPassword(encryptPassword);
        }

        // 调用服务层方法更新用户，并返回更新结果
        boolean result = userService.updateById(user);

        // 如果更新失败，抛出操作错误异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回成功响应
        return ResultUtils.success(true);
    }
}
