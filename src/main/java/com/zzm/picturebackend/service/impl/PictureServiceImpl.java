package com.zzm.picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import com.zzm.picturebackend.manager.CosManager;
import com.zzm.picturebackend.manager.FileManager;
import com.zzm.picturebackend.manager.upload.FilePictureUpload;
import com.zzm.picturebackend.manager.upload.PictureUploadTemplate;
import com.zzm.picturebackend.manager.upload.UrlPictureUpload;
import com.zzm.picturebackend.mapper.PictureMapper;
import com.zzm.picturebackend.model.dto.file.UploadPictureResult;
import com.zzm.picturebackend.model.dto.picture.*;
import com.zzm.picturebackend.model.entity.Picture;
import com.zzm.picturebackend.model.entity.Space;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.enums.PictureReviewStatusEnum;
import com.zzm.picturebackend.model.vo.PictureVO;
import com.zzm.picturebackend.model.vo.UserVO;
import com.zzm.picturebackend.service.PictureService;
import com.zzm.picturebackend.service.SpaceService;
import com.zzm.picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
@Slf4j
@Service // 声明这是一个 Spring 的 Service 类
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> // 继承 MyBatis-Plus 的 Service 实现类
    implements PictureService { // 实现 PictureService 接口

    // 注入 FileManager 用于文件管理操作
    @Resource // 使用 Resource 注解注入 FileManager
    private FileManager fileManager;

    // 注入 UserService 用于用户相关操作
    @Resource // 使用 Resource 注解注入 UserService
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Autowired
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

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
     * @param inputSource 上传的图片文件
     * @param pictureUploadRequest 图片上传请求对象，包含图片的元数据信息
     * @param loginUser 当前登录的用户对象
     * @return 包含上传图片信息的 PictureVO 对象
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 如果当前登录用户为空，抛出无权限异常
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR); // 校验空间是否存在

        // 空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();

        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");

            // 必须空间创建人（管理员）才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }

            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }


        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可编辑
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 上传图片，得到信息

        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", loginUser.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", spaceId);
        }
        //根据 inputSourced 的类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 构造要入库的图片信息
        Picture picture = new Picture();
        // 补充设置 spaceId
        picture.setSpaceId(spaceId);
        // 设置图片的URL
        picture.setUrl(uploadPictureResult.getUrl());
        // 设置图片的缩略图URL
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        // 支持外层传递图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        // 设置图片的名称
        picture.setName(picName);
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
        //补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 开启事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");

            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();

                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }

            return picture;
        });

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
        // 如果查询请求对象为空，直接返回空地查询条件包装器
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
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
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
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
        // 根据name查询，如果name不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        // 根据introduction查询，如果introduction不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        // 根据picFormat查询，如果picFormat不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        // 根据reviewMessage查询，如果reviewMessage不为空且不为单个空格
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
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
        // 根据reviewStatus查询，如果reviewStatus不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        // 根据reviewerId查询，如果reviewerId不为空
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

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

    /**
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量抓取图片请求
     * @param loginUser                   登录用户
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        //文件名前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }

        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        //图片元素
        //Elements imgElementList = div.select("img.mimg");
        Elements imgElementList = div.select(".iusc");  // 修改选择器，获取包含完整数据的元素
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            //String fileUrl = imgElement.attr("src");

            // 获取data-m属性中的JSON字符串
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

   @Async
@Override
public void clearPictureFile(Picture oldPicture) {
    // 判断该图片是否被多条记录使用
    String pictureUrl = oldPicture.getUrl();
    long count = this.lambdaQuery()
            .eq(Picture::getUrl, pictureUrl)
            .count();
    // 有不止一条记录用到了该图片，不清理
    if (count > 1) {
        return;
    }
    try {
        // 提取原图路径部分
        String picturePath = new URL(pictureUrl).getPath();
        // wei
//        cosManager.deleteObject(picturePath);
//        log.info("原图已删除: {}", picturePath);

        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            String thumbnailPath = new URL(thumbnailUrl).getPath();
            cosManager.deleteObject(thumbnailPath);
            log.info("缩略图已删除: {}", thumbnailPath);
        }
    } catch (MalformedURLException e) {
        log.error("处理图片删除时遇到格式错误的 URL。图片 URL: {}", pictureUrl, e);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "格式错误的 URL");
    }
}


    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 校验权限
        checkPictureAuth(loginUser, oldPicture);

        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

            // 释放额度
            Long spaceId = oldPicture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();

                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }

            return true;
        });

// 异步清理文件
        this.clearPictureFile(oldPicture);

    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        // 设置编辑时间
        picture.setEditTime(new Date());

        // 数据校验
        this.validPicture(picture);

        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //校验权限
        checkPictureAuth(loginUser, oldPicture);

        // 补充审核参数
        this.fillReviewParams(picture, loginUser);

        // 操作数据库
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();

        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

    }
}
