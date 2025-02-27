package com.zzm.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import com.zzm.picturebackend.manager.FileManager;
import com.zzm.picturebackend.model.dto.file.UploadPictureResult;
import com.zzm.picturebackend.model.dto.picture.PictureQueryRequest;
import com.zzm.picturebackend.model.dto.picture.PictureUploadRequest;
import com.zzm.picturebackend.model.entity.Picture;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.vo.PictureVO;
import com.zzm.picturebackend.model.vo.UserVO;
import com.zzm.picturebackend.service.PictureService;
import com.zzm.picturebackend.mapper.PictureMapper;
import com.zzm.picturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author zhou
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-26 19:17:02
 */
@Service // 声明这是一个 Spring 的 Service 类
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> // 继承 MyBatis-Plus 的 Service 实现类
    implements PictureService { // 实现 PictureService 接口

    // 注入 FileManager 用于文件管理操作
    @Resource // 使用 Resource 注解注入 FileManager
    private FileManager fileManager;

    // 注入 UserService 用于用户相关操作
    @Resource // 使用 Resource 注解注入 UserService
    private UserService userService;

    /**
     * 校验图片
     * 该方法用于校验图片对象的有效性
     * @param picture 需要校验的图片对象
     */
    @Override
    public void validPicture(Picture picture) {
        // 如果图片对象为空，抛出参数错误异常
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果 url 不为空，检查其长度是否超过 1024
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        // 如果简介不为空，检查其长度是否超过 800
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 上传图片
     * 该方法用于处理图片上传请求，将图片保存到服务器并返回图片信息
     * @param multipartFile 上传的图片文件
     * @param pictureUploadRequest 图片上传请求对象，包含图片的元数据信息
     * @param loginUser 当前登录的用户对象
     * @return 包含上传图片信息的 PictureVO 对象
     */
    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 如果当前登录用户为空，抛出无权限异常
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            boolean exists = this.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        }
        // 上传图片，得到信息
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        // 设置图片的URL
        picture.setUrl(uploadPictureResult.getUrl());
        // 设置图片的名称
        picture.setName(uploadPictureResult.getPicName());
        // 设置图片的大小
        picture.setPicSize(uploadPictureResult.getPicSize());
        // 设置图片的宽度
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        // 设置图片的高度
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        // 设置图片的尺寸比例
        picture.setPicScale(uploadPictureResult.getPicScale());
        // 设置图片的格式
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        // 设置图片所属的用户ID
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 保存或更新图片信息
        boolean result = this.saveOrUpdate(picture);
        // 如果保存或更新失败，抛出操作错误异常
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
        // 返回图片的 VO 对象
        return PictureVO.objToVo(picture);
    }

    /**
     * 获取图片的 VO 对象
     * 该方法将图片实体对象转换为前端展示的 VO 对象
     * @param picture 图片实体对象
     * @param request HTTP 请求对象，用于获取请求上下文信息
     * @return 包含图片信息的 PictureVO 对象
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     * 该方法将图片分页实体对象转换为前端展示的分页 VO 对象
     * @param picturePage 图片分页实体对象
     * @param request HTTP 请求对象，用于获取请求上下文信息
     * @return 包含图片分页信息的 Page<PictureVO> 对象
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        // 获取分页中的图片列表
        List<Picture> pictureList = picturePage.getRecords();
        // 创建分页 VO 对象
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 如果图片列表为空，直接返回空的分页 VO 对象
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        // 设置分页 VO 对象的记录列表
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 获取查询条件包装器
     * 该方法根据图片查询请求对象生成 MyBatis-Plus 的查询条件包装器
     * @param pictureQueryRequest 图片查询请求对象，包含查询条件信息
     * @return MyBatis-Plus 的查询条件包装器 QueryWrapper
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        // 创建查询条件包装器
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        // 如果查询请求对象为空，直接返回空的查询条件包装器
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId(); // 获取图片的唯一标识符
        String name = pictureQueryRequest.getName(); // 获取图片的名称
        String introduction = pictureQueryRequest.getIntroduction(); // 获取图片的简介
        String category = pictureQueryRequest.getCategory(); // 获取图片的分类
        List<String> tags = pictureQueryRequest.getTags(); // 获取图片的标签列表
        Long picSize = pictureQueryRequest.getPicSize(); // 获取图片的大小
        Integer picWidth = pictureQueryRequest.getPicWidth(); // 获取图片的宽度
        Integer picHeight = pictureQueryRequest.getPicHeight(); // 获取图片的高度
        Double picScale = pictureQueryRequest.getPicScale(); // 获取图片的比例
        String picFormat = pictureQueryRequest.getPicFormat(); // 获取图片的格式
        String searchText = pictureQueryRequest.getSearchText(); // 获取搜索文本
        Long userId = pictureQueryRequest.getUserId(); // 获取用户的唯一标识符
        String sortField = pictureQueryRequest.getSortField(); // 获取排序字段
        String sortOrder = pictureQueryRequest.getSortOrder(); // 获取排序顺序
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        // 添加查询条件
        // 根据id查询，如果id不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        // 根据userId查询，如果userId不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        // 根据name查询，如果name不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        // 根据introduction查询，如果introduction不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        // 根据picFormat查询，如果picFormat不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        // 根据category查询，如果category不为空且不为单个空格
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        // 根据picWidth查询，如果picWidth不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        // 根据picHeight查询，如果picHeight不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        // 根据picSize查询，如果picSize不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        // 根据picScale查询，如果picScale不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
}
