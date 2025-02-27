package com.zzm.picturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zzm.picturebackend.config.CosClientConfig;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.exception.ThrowUtils;
import com.zzm.picturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class FileManager {  
  
    @Resource
    private CosClientConfig cosClientConfig;
  
    @Resource  
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传图片的结果
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 生成UUID，用于文件命名
        String uuid = RandomUtil.randomString(16);
        // 获取原始文件名
        String originFilename = multipartFile.getOriginalFilename();
        // 生成上传文件名，格式为：日期_UUID.文件后缀
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        // 生成上传路径
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 将上传的文件内容写入临时文件
            multipartFile.transferTo(file);
            // 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            // 获取图片宽度
            int picWidth = imageInfo.getWidth();
            // 获取图片高度
            int picHeight = imageInfo.getHeight();
            // 计算图片宽高比，保留两位小数
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            // 设置图片名称
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            // 设置图片宽度
            uploadPictureResult.setPicWidth(picWidth);
            // 设置图片高度
            uploadPictureResult.setPicHeight(picHeight);
            // 设置图片宽高比
            uploadPictureResult.setPicScale(picScale);
            // 设置图片格式
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            // 设置图片大小
            uploadPictureResult.setPicSize(FileUtil.size(file));
            // 设置图片访问URL
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            // 返回上传结果
            return uploadPictureResult;
        } catch (Exception e) {
            // 记录上传失败日志
            log.error("图片上传到对象存储失败", e);
            // 抛出业务异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile multipart 文件
     */
    public void validPicture(MultipartFile multipartFile) {
        // 如果文件为空，抛出参数错误异常
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 获取文件大小
        long fileSize = multipartFile.getSize();
        // 定义1M的字节数
        final long ONE_M = 1024 * 1024L;
        // 如果文件大小超过2M，抛出参数错误异常
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        // 获取文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 定义允许上传的文件后缀列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        // 如果文件后缀不在允许列表中，抛出参数错误异常
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        // 如果文件为空，直接返回
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        // 如果删除失败，记录错误日志
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
