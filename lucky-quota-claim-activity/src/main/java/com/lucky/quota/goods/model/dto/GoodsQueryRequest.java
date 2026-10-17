package com.lucky.quota.goods.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoodsQueryRequest{

    /**
     * 物资名称（模糊查询）
     */
    private String goodsName;

    /**
     * 物资状态 0-下架 1-上架
     */
    private Integer status;

    /**
     * 审核状态：0=未申请 1=待审核 2=通过 3=拒绝
     */
    private Integer auditStatus;

    /**
     * 审核状态 IN 查询（管理员已审核列表等，服务端注入）
     */
    @JsonIgnore
    private List<Integer> auditStatusIn;

    /**
     * 子分类ID（叶子分类，精确匹配）
     */
    private Long categoryId;

    /**
     * 父分类ID（查询该父分类下所有子分类的物资）
     */
    private Long parentCategoryId;

    /**
     * 子分类ID集合（由服务层根据父分类解析，无需前端传入）
     */
    @JsonIgnore
    private List<Long> categoryIds;

    /**
     * 发放单位ID
     */
    private Long shopId;

    /**
     * 发布者用户ID（商户「我的物资」查询由服务端注入，前端勿传）
     */
    @JsonIgnore
    private Long userId;

    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 页码（默认第1页）
     */
    private Integer pageNum = 1;

    /**
     * 每页大小（默认10条）
     */
    private Integer pageSize = 10;
}
