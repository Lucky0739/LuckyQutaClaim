package com.lucky.seckill.common.manager;

import cn.hutool.core.io.FileUtil;
import com.lucky.seckill.common.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {
    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    // 流式上传
    public PutObjectResult putObject(String key, InputStream inputStream) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosClientConfig.getBucketName(),
                key,
                inputStream,
                null // 元数据，不传也行
        );
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 上传用户头像
     * @param key
     * @return
     */
    public PutObjectResult userAvatarObject(String key, InputStream inputStream, long fileSize) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosClientConfig.getBucketName(),
                key,
                inputStream,
                null
        );
        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1); // 返回图片信息
        List<PicOperations.Rule> rules = new ArrayList<>();
        //  生成 WebP 压缩图
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucketName());
        compressRule.setRule("imageMogr2/format/webp"); // 转webp压缩
        rules.add(compressRule);
        // 组装规则
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }


    /** 流式上传 + 图片压缩/缩略
     * @param key
     * @param inputStream
     * @param fileSize
     * @return
     */
    public PutObjectResult putPictureObject(String key, InputStream inputStream, long fileSize) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                cosClientConfig.getBucketName(),
                key,
                inputStream,
                null
        );

        PicOperations picOperations = new PicOperations();
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();

        // 1. 转 webp 压缩
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucketName());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);

        // 2. 大于 2KB 生成缩略图
        if (fileSize > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucketName());
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     */
    public void deleteObject(String bucketName,String key){
        cosClient.deleteObject(cosClientConfig.getBucketName(),key);
    }
}
