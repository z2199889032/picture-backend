package com.zzm.picturebackend.aop;

import com.zzm.picturebackend.annotation.AuthCheck;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.enums.UserRoleEnum;
import com.zzm.picturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author zhou
 * @Description: 切面类，用于权限校验
 * @createDate 2025/2/25上午8:28
 */

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截逻辑。
     *
     * @param joinPoint 切入点，表示被拦截的方法信息
     * @param authCheck 权限校验注解，包含所需的角色信息
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 获取注解中指定的必须角色
        String mustRole = authCheck.mustRole();

        // 获取当前请求的属性
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();

        // 从请求属性中获取HttpServletRequest对象
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 将字符串形式的角色转换为枚举类型
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

        // 如果不需要特定权限，则直接放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        // 如果用户没有分配任何角色，拒绝访问
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 如果需要管理员权限，但当前用户不是管理员，拒绝访问
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 通过权限校验，放行请求
        return joinPoint.proceed();
    }
}
