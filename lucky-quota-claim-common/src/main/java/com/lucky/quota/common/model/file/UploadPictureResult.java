package com.lucky.quota.common.model.file;

import lombok.Data;

@Data
public class UploadPictureResult {

    /**
     * 图片 url
     */
    private String imageUrl;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

}
