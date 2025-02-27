package com.zzm.picturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.zzm.picturebackend.annotation.AuthCheck;
import com.zzm.picturebackend.common.BaseResponse;
import com.zzm.picturebackend.common.ResultUtils;
import com.zzm.picturebackend.constant.UserConstant;
import com.zzm.picturebackend.exception.BusinessException;
import com.zzm.picturebackend.exception.ErrorCode;
import com.zzm.picturebackend.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
* @author zhou
* @Description:
* @createDate 2025/2/26下午6:19
*/
@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * @param multipartFile 上传的文件
     * @return 上传结果
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {

        // 获取文件名
        String filename = multipartFile.getOriginalFilename();
        // 构建文件路径
        String filepath = String.format("/test/%s", filename);

        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(filepath, null);
            // 将上传的文件内容写入临时文件
            multipartFile.transferTo(file);
            // 上传文件到对象存储
            cosManager.putObject(filepath, file);
            // 返回上传文件的路径
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            // 记录上传失败日志
            log.error("file upload error, filepath = " + filepath, e);
            // 抛出业务异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                // 如果删除失败，记录错误日志
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }

    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            // 从对象存储获取文件对象
            COSObject cosObject = cosManager.getObject(filepath);
            // 获取文件输入流
            cosObjectInput = cosObject.getObjectContent();
            // 将输入流转换为字节数组
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应内容类型
            response.setContentType("application/octet-stream;charset=UTF-8");
            // 设置响应头，指定文件名
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 将字节数组写入响应输出流
            response.getOutputStream().write(bytes);
            // 刷新响应输出流
            response.getOutputStream().flush();
        } catch (Exception e) {
            // 记录下载失败日志
            log.error("file download error, filepath = " + filepath, e);
            // 抛出业务异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                // 关闭输入流
                cosObjectInput.close();
            }
        }
    }
}
