package com.lucky.quota.common.service.impl;

import cn.hutool.core.util.StrUtil;
import com.lucky.quota.common.config.CosClientConfig;
import com.lucky.quota.common.enums.ImageCleanBusinessTypeEnum;
import com.lucky.quota.common.enums.ImageCleanStatusEnum;
import com.lucky.quota.common.manager.CosManager;
import com.lucky.quota.common.mapper.ImageCleanTaskMapper;
import com.lucky.quota.common.model.entity.ImageCleanTask;
import com.lucky.quota.common.service.ImageCleanTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ImageCleanTaskServiceImpl implements ImageCleanTaskService {

    private static final int DEFAULT_EXPIRE_MINUTE = 30;
    private static final int BATCH_LIMIT = 50;

    @Resource
    private ImageCleanTaskMapper imageCleanTaskMapper;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Override
    public void addCleanTask(String imageUrl, ImageCleanBusinessTypeEnum businessType) {
        if (StrUtil.isBlank(imageUrl) || businessType == null) {
            return;
        }
        if (!isManagedCosUrl(imageUrl)) {
            log.debug("==> 跳过非本系统 COS 图片清理登记, url: {}", imageUrl);
            return;
        }
        if (imageCleanTaskMapper.countPendingByImageUrl(imageUrl) > 0) {
            log.debug("==> 图片已存在待清理任务, url: {}", imageUrl);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        ImageCleanTask task = ImageCleanTask.builder()
                .imageUrl(imageUrl)
                .businessType(businessType.getCode())
                .status(ImageCleanStatusEnum.PENDING.getCode())
                .expireMinute(DEFAULT_EXPIRE_MINUTE)
                .createTime(now)
                .updateTime(now)
                .build();
        imageCleanTaskMapper.insertSelective(task);
        log.info("==> 登记待清理图片, businessType: {}, url: {}", businessType.getCode(), imageUrl);
    }

    @Override
    public void processExpiredTasks() {
        List<ImageCleanTask> tasks = imageCleanTaskMapper.selectExpiredPendingTasks(BATCH_LIMIT);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (ImageCleanTask task : tasks) {
            processOneTask(task);
        }
    }

    private void processOneTask(ImageCleanTask task) {
        LocalDateTime now = LocalDateTime.now();
        String objectKey = extractObjectKey(task.getImageUrl());
        if (StrUtil.isBlank(objectKey)) {
            markFailed(task, now, "无法解析 COS 对象 Key");
            return;
        }

        try {
            cosManager.deleteObject(cosClientConfig.getBucketName(), objectKey);
            ImageCleanTask update = ImageCleanTask.builder()
                    .id(task.getId())
                    .status(ImageCleanStatusEnum.CLEANED.getCode())
                    .deleteTime(now)
                    .updateTime(now)
                    .build();
            imageCleanTaskMapper.updateStatus(update);
            log.info("==> 图片清理成功, taskId: {}, url: {}", task.getId(), task.getImageUrl());
        } catch (Exception e) {
            markFailed(task, now, e.getMessage());
            log.error("==> 图片清理失败, taskId: {}, url: {}", task.getId(), task.getImageUrl(), e);
        }
    }

    private void markFailed(ImageCleanTask task, LocalDateTime now, String reason) {
        ImageCleanTask update = ImageCleanTask.builder()
                .id(task.getId())
                .status(ImageCleanStatusEnum.FAILED.getCode())
                .updateTime(now)
                .build();
        imageCleanTaskMapper.updateStatus(update);
        log.warn("==> 图片清理任务标记失败, taskId: {}, reason: {}", task.getId(), reason);
    }

    private boolean isManagedCosUrl(String imageUrl) {
        return StrUtil.isNotBlank(cosClientConfig.getHost())
                && imageUrl.startsWith(cosClientConfig.getHost());
    }

    private String extractObjectKey(String imageUrl) {
        if (!isManagedCosUrl(imageUrl)) {
            return null;
        }
        String key = imageUrl.substring(cosClientConfig.getHost().length());
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return StrUtil.isBlank(key) ? null : key;
    }
}
