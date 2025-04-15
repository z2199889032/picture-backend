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
import com.zzm.picturebackend.mapper.SpaceMapper;
import com.zzm.picturebackend.model.dto.space.SpaceAddRequest;
import com.zzm.picturebackend.model.dto.space.SpaceQueryRequest;
import com.zzm.picturebackend.model.entity.Space;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.enums.SpaceLevelEnum;
import com.zzm.picturebackend.model.vo.SpaceVO;
import com.zzm.picturebackend.model.vo.UserVO;
import com.zzm.picturebackend.service.SpaceService;
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

    /**
     * 添加空间
     *
     * @param spaceAddRequest 空间添加请求对象，包含空间的基本信息
     * @param loginUser      登录用户对象，用于获取用户ID和权限验证
     * @return 新创建的空间ID，若操作失败返回-1
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 将请求对象转换为实体类对象
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);

        // 设置默认值：若名称为空则设为"默认空间"，级别为空则设为普通级别
        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        // 根据空间级别填充其他相关字段
        // 填充数据
        this.fillSpaceBySpaceLevel(space);

        // 执行数据校验，确保空间信息合法
        // 数据校验
        this.validSpace(space, true);

        Long userId = loginUser.getId();
        space.setUserId(userId);

        // 权限校验：非管理员用户无法创建非普通级别的空间
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }

        // 针对用户进行加锁，确保同一用户并发操作时数据一致性
        // 使用事务确保操作原子性：检查用户是否已有私有空间，并保存空间信息到数据库
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");

                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

                return space.getId();
            });

            return Optional.ofNullable(newSpaceId).orElse(-1L);
        }
    }

    /**
     * 验证空间对象的参数有效性。
     *
     * @param space 待验证的空间对象
     * @param add   是否为新增操作（true为新增，false为修改）
     */
    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

        // 新增时的必填项验证
        if (add) {
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }

        // 修改时的空间级别有效性验证
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }

        // 空间名称长度限制验证
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }
    }

    /**
     * 将Space实体对象转换为封装类SpaceVO，并关联查询用户信息。
     *
     * @param space     空间实体对象
     * @param request   HTTP请求对象（当前未使用）
     * @return 封装后的SpaceVO对象，包含关联的用户信息（若存在）
     */
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

    /**
     * 将分页空间实体转换为分页空间视图对象
     * @param spacePage 需要转换的分页空间实体对象
     * @param request 当前HTTP请求对象（未被使用）
     * @return 转换后的分页空间视图对象
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        // 将空间实体列表转换为视图对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());

        // 执行用户信息关联查询
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 填充用户信息到视图对象
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });

        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 根据查询请求构建查询条件包装器
     *
     * @param spaceQueryRequest 查询条件请求对象
     * @return 构建的查询包装器
     */
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
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 根据空间级别自动填充空间的限额（maxSize和maxCount）。
     *
     * @param space 需要填充限额的空间对象
     * @return 无返回值
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        // 根据空间级别值获取对应的枚举对象
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

        if (spaceLevelEnum != null) {
            // 如果枚举对象有效，则继续处理限额设置

            // 如果空间当前未设置最大尺寸，则使用枚举定义的值填充
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }

            // 如果空间当前未设置最大数量，则使用枚举定义的值填充
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

    /**
     * 检查用户是否有权限编辑指定的空间。
     *
     * @param loginUser 登录的用户对象
     * @param space 需要检查权限的空间对象
     * @throws BusinessException 当用户无权限时抛出（错误码：NO_AUTH_ERROR）
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        // 仅本人或管理员可编辑
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }
}




