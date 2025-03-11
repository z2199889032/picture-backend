package com.zzm.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzm.picturebackend.model.dto.space.SpaceAddRequest;
import com.zzm.picturebackend.model.dto.space.SpaceQueryRequest;
import com.zzm.picturebackend.model.entity.Space;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author zhou
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-03-10 18:56:24
*/
public interface SpaceService extends IService<Space> {
    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     * 该方法用于校验空间对象的有效性
     * @param space 需要校验的空间对象
     */
    void validSpace(Space space,boolean add);


    /**
     * 获取空间的 VO 对象
     * 该方法将空间实体对象转换为前端展示的 VO 对象
     * @param space 空间实体对象
     * @param request HTTP 请求对象，用于获取请求上下文信息
     * @return 包含空间信息的 SpaceVO 对象
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间的分页 VO 对象
     * 该方法将空间分页实体对象转换为前端展示的分页 VO 对象
     * @param spacePage 空间分页实体对象
     * @param request HTTP 请求对象，用于获取请求上下文信息
     * @return 包含空间分页信息的 Page<SpaceVO> 对象
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询条件包装器
     * 该方法根据空间查询请求对象生成 MyBatis-Plus 的查询条件包装器
     * @param spaceQueryRequest 空间查询请求对象，包含查询条件信息
     * @return MyBatis-Plus 的查询条件包装器 QueryWrapper
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别填充空间信息
     * @param space
     */

    void fillSpaceBySpaceLevel(Space space);

}
