package com.lucky.seckill.common.service;

import com.lucky.seckill.common.enums.ImageCleanBusinessTypeEnum;

/**
 * 待清理图片任务服务
 */
public interface ImageCleanTaskService {

    /**
     * 登记待清理图片（替换图片时调用）
     */
    void addCleanTask(String imageUrl, ImageCleanBusinessTypeEnum businessType);

    /**
     * 处理已到期的待清理任务（定时任务调用）
     */
    void processExpiredTasks();
}
