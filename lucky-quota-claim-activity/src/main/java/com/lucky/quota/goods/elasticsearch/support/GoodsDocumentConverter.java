package com.lucky.quota.goods.elasticsearch.support;

import com.lucky.quota.goods.elasticsearch.document.GoodsDocument;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsCategory;
import com.lucky.quota.goods.model.entity.GoodsSku;
import com.lucky.quota.goods.model.vo.GoodsListVO;

import java.util.List;

public final class GoodsDocumentConverter {

    private static final long PARENT_CATEGORY_ROOT_ID = 0L;

    private GoodsDocumentConverter() {
    }

    public static boolean isSearchable(Goods goods) {
        if (goods == null || goods.getIsDelete() != null && goods.getIsDelete() == 1) {
            return false;
        }
        return Integer.valueOf(1).equals(goods.getStatus())
                && Integer.valueOf(2).equals(goods.getAuditStatus());
    }

    public static GoodsDocument toDocument(Goods goods,
                                           int salesNum,
                                           GoodsCategory category,
                                           GoodsCategory parentCategory) {
        GoodsDocument.GoodsDocumentBuilder builder = GoodsDocument.builder()
                .id(goods.getId())
                .goodsName(goods.getGoodsName())
                .goodsImg(goods.getGoodsImg())
                .goodsPrice(goods.getGoodsPrice())
                .salesNum(salesNum)
                .categoryId(goods.getCategoryId())
                .shopId(goods.getShopId())
                .createTime(goods.getCreateTime());

        if (category != null) {
            builder.categoryName(category.getCategoryName());
            if (isParentCategory(category)) {
                builder.parentCategoryId(category.getId());
                builder.parentCategoryName(category.getCategoryName());
            } else if (parentCategory != null) {
                builder.parentCategoryId(parentCategory.getId());
                builder.parentCategoryName(parentCategory.getCategoryName());
            } else if (category.getParentId() != null) {
                builder.parentCategoryId(category.getParentId());
            }
        }
        return builder.build();
    }

    public static int sumSalesNum(List<GoodsSku> skuList) {
        if (skuList == null || skuList.isEmpty()) {
            return 0;
        }
        return skuList.stream()
                .filter(sku -> sku.getIsDelete() == null || sku.getIsDelete() == 0)
                .mapToInt(sku -> sku.getSalesNum() == null ? 0 : sku.getSalesNum())
                .sum();
    }

    public static GoodsListVO toListVO(GoodsDocument document) {
        return GoodsListVO.builder()
                .id(document.getId())
                .goodsName(document.getGoodsName())
                .goodsImg(document.getGoodsImg())
                .goodsPrice(document.getGoodsPrice())
                .salesNum(document.getSalesNum())
                .categoryId(document.getCategoryId())
                .categoryName(document.getCategoryName())
                .parentCategoryId(document.getParentCategoryId())
                .parentCategoryName(document.getParentCategoryName())
                .status(1)
                .auditStatus(2)
                .shopId(document.getShopId())
                .createTime(document.getCreateTime())
                .build();
    }

    private static boolean isParentCategory(GoodsCategory category) {
        Long parentId = category.getParentId();
        return parentId == null || parentId == PARENT_CATEGORY_ROOT_ID;
    }
}
