package com.zzm.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import com.zzm.picturebackend.model.dto.space.SpaceAddRequest;
import com.zzm.picturebackend.model.dto.space.SpaceQueryRequest;
import com.zzm.picturebackend.model.entity.Picture;
import com.zzm.picturebackend.model.entity.Space;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.enums.SpaceLevelEnum;
import com.zzm.picturebackend.model.vo.PictureVO;
import com.zzm.picturebackend.model.vo.SpaceVO;
import com.zzm.picturebackend.model.vo.UserVO;
import com.zzm.picturebackend.service.SpaceService;
import com.zzm.picturebackend.mapper.SpaceMapper;
import com.zzm.picturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author zhou
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-03-10 18:56:24
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);

        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        // 填充数据
        this.fillSpaceBySpaceLevel(space);

        // 数据校验
        this.validSpace(space, true);

        Long userId = loginUser.getId();
        space.setUserId(userId);

        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // 针对用户进行加锁
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");

                // 写入数据库
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

                // 返回新写入的数据 id
                return space.getId();
            });

            // 返回结果是包装类，可以做一些处理
            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }


    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }

        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }

        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 获取分页中的图片列表
        List<Space> spaceList = spacePage.getRecords();
        // 创建分页 VO 对象
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        // 如果图片列表为空，直接返回空的分页 VO 对象
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        // 设置分页 VO 对象的记录列表
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        // 创建查询条件包装器
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        // 如果查询请求对象为空，直接返回空地查询条件包装器
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值

        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        // 添加查询条件
        // 根据id查询，如果id不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        // 根据userId查询，如果userId不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        // 根据name查询，如果name不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        // 根据introduction查询，如果introduction不为空且不为单个空格
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }

            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }
}




