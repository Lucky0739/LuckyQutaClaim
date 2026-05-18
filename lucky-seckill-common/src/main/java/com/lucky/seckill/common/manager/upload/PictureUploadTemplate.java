package com.lucky.seckill.common.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.lucky.seckill.common.config.CosClientConfig;
import com.lucky.seckill.common.enums.ResponseCodeEnum;
import com.lucky.seckill.common.exception.BizException;
import com.lucky.seckill.common.exception.GlobalExceptionHandler;
import com.lucky.seckill.common.manager.CosManager;
import com.lucky.seckill.common.model.file.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.squareup.okhttp.internal.framed.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传图片（流式上传，无本地临时文件）
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix, boolean isAvatar) {
        // 1.校验图片
        validPicture(inputSource);
        // 2.拼接上传路径
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originFilename));
        String projectName = "lucky-seckill_project";
        String uploadPath = String.format(projectName + "/%s/%s", uploadPathPrefix, uploadFilename);
        try (
                // 核心：获取输入流，自动关闭（try-with-resources）
                InputStream inputStream = getInputStream(inputSource);
        ) {
            // 3.获取文件大小
            long fileSize = getFileSize(inputSource);
            PutObjectResult putObjectResult;
            // 4.流式上传
            if (isAvatar) {
                // 上传【用户/店铺logo】
                putObjectResult = cosManager.userAvatarObject(
                        uploadPath,
                        inputStream,
                        fileSize
                );
            } else {
                // 上传【商品图片】
                putObjectResult = cosManager.putPictureObject(
                        uploadPath,
                        inputStream,
                        fileSize
                );
            }
            // 5.解析返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject compressCiObject = objectList.get(0);
                CIObject thumbnailCiObject = compressCiObject;
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 核心规则
                if (isAvatar) {
                    // 头像：只返回 webp 压缩图，不需要缩略图
                    return buildResult(originFilename, compressCiObject, null, imageInfo);
                } else {
                    // 帖子图片：压缩图 + 缩略图 全都返回
                    return buildResult(originFilename, compressCiObject, thumbnailCiObject, imageInfo);
                }
            }
            return buildResult(originFilename, null, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BizException(ResponseCodeEnum.SYSTEM_ERROR, "图片上传失败");
        }
    }

    //  子类必须实现的 3 个抽象方法
    /**
     * 获取输入流（核心：子类返回 URL/文件/MultipartFile 对应的流）
     */
    protected abstract InputStream getInputStream(Object inputSource) throws Exception;

    /**
     * 获取文件大小
     */
    protected abstract long getFileSize(Object inputSource);

    /**
     * 获取原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 校验输入源（本地文件或URL）
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 封装返回结果
     * @param originFilename 压缩文件名
     * @param compressCiObject 压缩后的对象
     * @param thumbnailCiObject 缩略图
     * @param imageInfo 图片信息
     * @return
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressCiObject,
                                                CIObject thumbnailCiObject,
                                                ImageInfo imageInfo) {
        // 计算宽高
        int picWidth=compressCiObject.getWidth();
        int picHeight=compressCiObject.getHeight();
        double picScale= NumberUtil.round(picWidth * 1.000/picHeight,2).doubleValue();
        // 设置压缩后的原图地址
        UploadPictureResult uploadPictureResult=new UploadPictureResult();
        uploadPictureResult.setImageUrl(cosClientConfig.getHost()+"/"+compressCiObject.getKey());
        uploadPictureResult.setPicSize(compressCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressCiObject.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        // 设置缩略图地址
        if (thumbnailCiObject != null) {
            uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost()+"/"+thumbnailCiObject.getKey());
        }
        // 返回可访问的地址
        return uploadPictureResult;
    }


    /**
     * 封装返回结果
     *
     * @param imageInfo  //对象存储返回的图片信息
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, File file, String uploadPath, ImageInfo imageInfo){
        // 计算宽高
        int picWidth=imageInfo.getWidth();
        int picHeight=imageInfo.getHeight();
        double picScale= NumberUtil.round(picWidth * 1.000/picHeight,2).doubleValue();
        // 封装返回结果
        UploadPictureResult uploadPictureResult=new UploadPictureResult();
        uploadPictureResult.setImageUrl(cosClientConfig.getHost()+"/"+uploadPath);
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }

}
