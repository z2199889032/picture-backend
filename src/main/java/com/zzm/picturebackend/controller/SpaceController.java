package com.zzm.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzm.picturebackend.annotation.AuthCheck;
import com.zzm.picturebackend.common.BaseResponse;
import com.zzm.picturebackend.common.DeleteRequest;
import com.zzm.picturebackend.common.ResultUtils;
import com.zzm.picturebackend.constant.UserConstant;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import com.zzm.picturebackend.model.dto.space.*;
import com.zzm.picturebackend.model.entity.Space;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.enums.SpaceLevelEnum;
import com.zzm.picturebackend.model.vo.SpaceVO;
import com.zzm.picturebackend.service.SpaceService;
import com.zzm.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhou
 * @Description:
 * @createDate 2025/2/27上午9:11
 */
@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;


    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);


    }

    /**
     * 删除空间
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        // 检查请求参数是否为空或ID是否小于等于0
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取要删除的空间ID
        long id = deleteRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceService.getById(id);
        // 如果空间不存在，抛出异常
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断是否为空间所有者或管理员
        spaceService.checkSpaceAuth(loginUser,oldSpace);

        // 操作数据库删除空间
        boolean result = spaceService.removeById(id);
        // 如果删除失败，抛出异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回删除成功结果
        return ResultUtils.success(true);
    }

    /**
     * 更新空间（仅管理员可用）
     * @param spaceUpdateRequest
     * @param request
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                               HttpServletRequest request) {
        // 检查请求参数是否为空或ID是否小于等于0
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space,false);
        // 获取要更新的空间ID
        long id = spaceUpdateRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceService.getById(id);
        // 如果空间不存在，抛出异常
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = spaceService.updateById(space);
        // 如果更新失败，抛出异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回更新成功结果
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        // 检查ID是否小于等于0
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        // 如果空间不存在，抛出异常
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        // 检查ID是否小于等于0
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        // 如果空间不存在，抛出异常
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVO(space, request));
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        // 获取当前页码
        long current = spaceQueryRequest.getCurrent();
        // 获取每页大小
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 返回分页结果
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                             HttpServletRequest request) {
        // 获取当前页码
        long current = spaceQueryRequest.getCurrent();
        // 获取每页大小
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 编辑空间（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        // 检查请求参数是否为空或ID是否小于等于0
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        //自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validSpace(space,false);
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        // 获取要编辑的空间ID
        long id = spaceEditRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceService.getById(id);
        // 如果空间不存在，抛出异常
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 判断是否为空间所有者或管理员
        spaceService.checkSpaceAuth(loginUser,oldSpace);
        // 操作数据库更新空间
        boolean result = spaceService.updateById(space);
        // 如果更新失败，抛出异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回编辑成功结果
        return ResultUtils.success(true);
    }

    /**
     * 获取空间等级列表：便于用户选择空间等级;
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());

        return ResultUtils.success(spaceLevelList);
    }


}
