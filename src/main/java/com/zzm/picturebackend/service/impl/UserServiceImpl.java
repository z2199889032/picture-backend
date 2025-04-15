package com.zzm.picturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzm.picturebackend.constant.UserConstant;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.mapper.UserMapper;
import com.zzm.picturebackend.model.dto.user.UserQueryRequest;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.enums.UserRoleEnum;
import com.zzm.picturebackend.model.vo.LoginUserVO;
import com.zzm.picturebackend.model.vo.UserVO;
import com.zzm.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
* @author zhou
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-02-21 10:28:22
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 用户id
     *
     * 步骤：
     * 1. 校验输入参数是否为空或不符合要求（如长度限制）
     * 2. 检查用户账号是否已存在
     * 3. 对用户密码进行加密处理
     * 4. 将新用户信息插入数据库，并返回用户ID
     */
    @Override
public long userRegister(String userAccount, String userPassword, String checkPassword) {
    // 检查用户账号、密码和确认密码是否为空
    if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空"); // 如果有空参数，抛出业务异常
    }

    // 检查用户账号长度是否小于4个字符
    if (userAccount.length() < 4) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短"); // 如果账号过短，抛出业务异常
    }

    // 检查用户密码和确认密码长度是否小于8个字符
    if (userPassword.length() < 8 || checkPassword.length() < 8) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短"); // 如果密码过短，抛出业务异常
    }

    // 检查两次输入的密码是否一致
    if (!userPassword.equals(checkPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致"); // 如果密码不一致，抛出业务异常
    }

    // 创建查询条件对象
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();

    // 设置查询条件为用户账号等于传入的账号
    queryWrapper.eq("userAccount", userAccount);

    // 查询数据库中是否存在相同账号的用户
    long count = this.baseMapper.selectCount(queryWrapper);

    // 如果存在相同账号的用户，抛出业务异常
    if (count > 0) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
    }

    // 对用户密码进行加密处理
    String encryptPassword = getEncryptPassword(userPassword);

    // 创建新的用户对象
    User user = new User();

    // 设置用户账号
    user.setUserAccount(userAccount);

    // 设置加密后的用户密码
    user.setUserPassword(encryptPassword);

    // 设置默认用户名为“无名”
    user.setUserName("无名");

    // 设置用户角色为普通用户
    user.setUserRole(UserRoleEnum.USER.getValue());

    // 将用户信息保存到数据库
    boolean saveResult = this.save(user);

    // 如果保存失败，抛出业务异常
    if (!saveResult) {
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
    }

    // 返回新用户的ID
    return user.getId();
}

    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request HTTP请求对象，用于获取和设置会话属性
     * @return 登录成功的用户信息
     *
     * 步骤：
     * 1. 校验输入参数是否为空或不符合要求
     * 2. 对用户密码进行加密处理
     * 3. 查询用户是否存在
     * 4. 如果用户存在，记录用户的登录状态到会话中
     */
   @Override
public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
    // 1. 校验输入参数的有效性

    // 检查用户账号和密码是否为空
    if (StrUtil.hasBlank(userAccount, userPassword)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空"); // 如果有空参数，抛出业务异常
    }

    // 检查用户账号长度是否小于4个字符
    if (userAccount.length() < 4) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误"); // 如果账号过短，抛出业务异常
    }

    // 检查用户密码长度是否小于8个字符
    if (userPassword.length() < 8) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误"); // 如果密码过短，抛出业务异常
    }

    // 2. 对用户密码进行加密处理
    String encryptPassword = getEncryptPassword(userPassword);

    // 3. 查询用户是否存在

    // 创建查询条件对象
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();

    // 设置查询条件为用户账号等于传入的账号
    queryWrapper.eq("userAccount", userAccount);

    // 设置查询条件为用户密码等于加密后的密码
    queryWrapper.eq("userPassword", encryptPassword);

    // 执行查询操作，获取用户信息
    User user = this.baseMapper.selectOne(queryWrapper);

    // 如果用户不存在或密码不匹配，记录日志并抛出业务异常
    if (user == null) {
        log.info("user login failed, userAccount cannot match userPassword"); // 输出登录失败的日志
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
    }

    // 4. 记录用户的登录状态

    // 将用户信息保存到当前会话中
    request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

    // 返回包含用户信息的视图对象
    return this.getLoginUserVO(user);
}



    /**
     * 获取加密后的密码
     *
     * @param userPassword 用户的原始密码
     * @return 加密后的密码
     *
     * 使用MD5算法结合固定盐值对用户密码进行加密，以提高安全性
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，用于混淆密码，提高密码安全性
        final String SALT = "zzm123456789";
        // 将盐值和用户密码组合后，使用MD5算法进行加密，返回加密后的密码
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request HTTP请求对象，用于获取会话属性
     * @return 当前登录的用户信息
     *
     * 从会话中获取用户信息，如果未登录则抛出异常
     */
   @Override
public User getLoginUser(HttpServletRequest request) {
    // 1. 获取当前会话中的登录用户信息

    // 从会话中获取用户对象
    Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);

    // 将对象转换为User类型
    User currentUser = (User) userObj;

    // 检查用户是否已登录（即会话中是否有用户信息）
    if (currentUser == null || currentUser.getId() == null) {
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR); // 如果未登录，抛出业务异常
    }

    // 2. 从数据库中查询用户信息（可选步骤，追求性能的话可以省略）

    // 获取用户的ID
    long userId = currentUser.getId();

    // 从数据库中重新查询用户信息以确保数据最新
    currentUser = this.getById(userId);

    // 如果用户不存在，抛出业务异常
    if (currentUser == null) {
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }

    // 返回查询到的用户信息
    return currentUser;
}



    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的用户信息
     *
     * 将用户对象转换为脱敏后的视图对象，避免敏感信息泄露
     */
   @Override
public LoginUserVO getLoginUserVO(User user) {
    // 创建一个新的LoginUserVO对象
    LoginUserVO loginUserVO = new LoginUserVO();

    // 将User对象的属性复制到LoginUserVO对象中
    BeanUtil.copyProperties(user, loginUserVO);

    // 返回填充了用户信息的LoginUserVO对象
    return loginUserVO;
}

    /**
     * 用户登出
     *
     * @param request HTTP请求对象，用于移除会话属性
     * @return 是否成功登出
     *
     * 移除会话中的用户登录状态，完成登出操作
     */
    @Override
public boolean userLogout(HttpServletRequest request) {
    // 1. 判断用户是否已登录

    // 从会话中获取用户对象
    Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);

    // 如果用户对象为空，说明用户未登录，抛出业务异常
    if (userObj == null) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
    }

    // 2. 移除用户的登录状态

    // 从会话中移除用户对象
    request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

    // 返回true表示注销成功
    return true;
}

    /**
     * 将用户实体转换为用户视图对象
     *
     * @param user 用户实体
     * @return 用户视图对象
     *
     * 如果用户实体为空，则返回null；否则将用户实体转换为视图对象
     */
   @Override
public UserVO getUserVO(User user) {
    // 检查用户对象是否为空
    if (user == null) {
        return null; // 如果用户对象为空，返回null
    }

    // 创建一个新的UserVO对象
    UserVO userVO = new UserVO();

    // 将User对象的属性复制到UserVO对象中
    BeanUtils.copyProperties(user, userVO);

    // 返回填充了用户信息的UserVO对象
    return userVO;
}

    /**
     * 将用户实体列表转换为用户视图对象列表
     *
     * @param userList 用户实体列表
     * @return 用户视图对象列表
     *
     * 如果用户实体列表为空，则返回空列表；否则将每个用户实体转换为视图对象并收集到列表中
     */
   @Override
public List<UserVO> getUserVOList(List<User> userList) {
    // 检查用户列表是否为空或为空集合
    if (CollUtil.isEmpty(userList)) {
        return new ArrayList<>(); // 如果用户列表为空，返回一个空的ArrayList
    }

    // 将用户列表转换为UserVO列表
    return userList.stream()
                   .map(this::getUserVO) // 将每个User对象转换为UserVO对象
                   .collect(Collectors.toList()); // 收集结果到一个新的List中
}

    /**
     * 构建用户查询条件
     *
     * @param userQueryRequest 用户查询请求
     * @return 查询条件封装对象
     *
     * 根据用户查询请求构建MyBatis Plus的查询条件对象，支持多字段组合查询和排序
     */
   @Override
public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
    // 1. 检查请求参数是否为空

    // 如果请求参数为空，抛出业务异常
    if (userQueryRequest == null) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
    }

    // 2. 从请求参数中提取各个字段

    // 获取用户ID
    Long id = userQueryRequest.getId();

    // 获取用户账号
    String userAccount = userQueryRequest.getUserAccount();

    // 获取用户名
    String userName = userQueryRequest.getUserName();

    // 获取用户简介
    String userProfile = userQueryRequest.getUserProfile();

    // 获取用户角色
    String userRole = userQueryRequest.getUserRole();

    // 获取排序字段
    String sortField = userQueryRequest.getSortField();

    // 获取排序顺序
    String sortOrder = userQueryRequest.getSortOrder();

    // 3. 创建查询条件对象

    // 创建QueryWrapper对象
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();

    // 添加ID等于条件
    queryWrapper.eq(Objects.nonNull(id), "id", id);

    // 添加用户角色等于条件
    queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);

    // 添加用户账号模糊查询条件
    queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);

    // 添加用户名模糊查询条件
    queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);

    // 添加用户简介模糊查询条件
    queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);

    // 添加排序条件
    queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);

    // 返回构建好地查询条件对象
    return queryWrapper;
}
    /**
     * 检查用户是否为管理员角色。
     *
     * @param user 需要检查的用户对象
     * @return 是否为管理员角色（true/false）
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }



}




