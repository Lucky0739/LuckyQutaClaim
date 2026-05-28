package com.lucky.seckill.goods.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CategoryUpdateRequest {
    @NotNull(message = "分类ID不能为空")
    private Long id;

    @Size(max = 100, message = "分类名称长度不能超过100个字符")
    private String categoryName;

    private Long parentId;

    private Integer sort;

    private Integer status;
}