package com.zzm.picturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.zzm.picturebackend.config.CosClientConfig;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.manager.CosManager;
import com.zzm.picturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法，定义上传流程  
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
    // 1. 校验图片
    validPicture(inputSource);

    // 2. 图片上传地址
    String uuid = RandomUtil.randomString(16);
    String originFilename = getOriginFilename(inputSource);
    String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
            FileUtil.getSuffix(originFilename));
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

    File file = null;
    try {
        // 3. 创建临时文件
        file = File.createTempFile(uploadPath, null);
        // 处理文件来源（本地或 URL）
        processFile(inputSource, file);

        // 4. 上传图片到对象存储
        PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
        // 获取图片信息
        ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
        // 获取图片处理结果
        ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
        List<CIObject> objectList = processResults.getObjectList();
        if (CollUtil.isNotEmpty(objectList)) {
            // 获取压缩之后得到文件信息
            CIObject compressedCiObject = objectList.get(0);
            // 缩略图默认等于压缩图片
            CIObject thumbnailCiObject = compressedCiObject;
            // 有生成缩略图，则获取缩略图信息
            if (objectList.size() > 1) {
                thumbnailCiObject = objectList.get(1);
            }
            // 5. 封装返回结果
            log.info("Compressed CIObject: {}", compressedCiObject);
            log.info("Thumbnail CIObject: {}", thumbnailCiObject);
            return buildResult(originFilename, compressedCiObject, thumbnailCiObject,imageInfo);
        }
        // 5. 封装返回结果
        return buildResult(originFilename, file, uploadPath, imageInfo);
    } catch (Exception e) {
        log.error("图片上传到对象存储失败", e);
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
    } finally {
        // 6. 清理临时文件
        deleteTempFile(file);
    }
}


    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject, CIObject thumbnailCiObject) {
    UploadPictureResult uploadPictureResult = new UploadPictureResult();
    int picWidth = compressedCiObject.getWidth();
    int picHeight = compressedCiObject.getHeight();
    double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
    uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
    uploadPictureResult.setPicWidth(picWidth);
    uploadPictureResult.setPicHeight(picHeight);
    uploadPictureResult.setPicScale(picScale);
    uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
    uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
    // 设置图片为压缩后的地址
    uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
    // 设置缩略图地址
    if (thumbnailCiObject != null) {
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
    } else {
        log.warn("Thumbnail CIObject is null");
    }
    return uploadPictureResult;
}



    /**
     * 校验输入源（本地文件或 URL）  
     */
    protected abstract void validPicture(Object inputSource);

    protected abstract <T> String getOriginFilename(T inputSource);

    /**
     * 处理输入源并生成本地临时文件  
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;


    /**
     * 封装返回结果
     *
     * @param originalFilename   原始文件名
     * @param compressedCiObject 压缩后的对象
     * @param thumbnailCiObject 缩略图对象
     * @param imageInfo 图片信息
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject,
                                            ImageInfo imageInfo) {
        // 计算宽高
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 设置压缩后的原图地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        // 设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        // 返回可访问的地址
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     *
     * @param originalFilename
     * @param file
     * @param uploadPath
     * @param imageInfo        对象存储返回的图片信息
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, File file, String uploadPath, ImageInfo imageInfo) {
        // 计算宽高
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        // 返回可访问的地址
        return uploadPictureResult;
    }


    /**
     * 删除临时文件  
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
