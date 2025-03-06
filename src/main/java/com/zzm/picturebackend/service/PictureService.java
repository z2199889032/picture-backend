package com.zzm.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zzm.picturebackend.model.dto.picture.PictureQueryRequest;
import com.zzm.picturebackend.model.dto.picture.PictureReviewRequest;
import com.zzm.picturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.zzm.picturebackend.model.dto.picture.PictureUploadRequest;
import com.zzm.picturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zzm.picturebackend.model.entity.User;
import com.zzm.picturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author zhou
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-02-26 19:17:02
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     * 该方法用于校验图片对象的有效性
     * @param picture 需要校验的图片对象
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     * 该方法用于处理图片上传请求，将图片保存到服务器并返回图片信息
     * @param inputSource 文件输入源
     * @param pictureUploadRequest 图片上传请求对象，包含图片的元数据信息
     * @param loginUser 当前登录的用户对象
     * @return 包含上传图片信息的 PictureVO 对象
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 获取图片的 VO 对象
     * 该方法将图片实体对象转换为前端展示的 VO 对象
     * @param picture 图片实体对象
     * @param request HTTP 请求对象，用于获取请求上下文信息
     * @return 包含图片信息的 PictureVO 对象
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片的分页 VO 对象
     * 该方法将图片分页实体对象转换为前端展示的分页 VO 对象
     * @param picturePage 图片分页实体对象
     * @param request HTTP 请求对象，用于获取请求上下文信息
     * @return 包含图片分页信息的 Page<PictureVO> 对象
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 获取查询条件包装器
     * 该方法根据图片查询请求对象生成 MyBatis-Plus 的查询条件包装器
     * @param pictureQueryRequest 图片查询请求对象，包含查询条件信息
     * @return MyBatis-Plus 的查询条件包装器 QueryWrapper
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     *图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest,User loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );


}
