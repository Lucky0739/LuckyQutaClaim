package com.lucky.quota.goods.elasticsearch.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 可搜索物资文档（仅上架且审核通过的物资会写入索引）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "#{@goodsElasticsearchProperties.indexName}", createIndex = false)
@Setting(shards = 1, replicas = 0)
public class GoodsDocument {

    @Id
    private Long id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "standard"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String goodsName;

    @Field(type = FieldType.Keyword)
    private String goodsImg;

    @Field(type = FieldType.Double)
    private BigDecimal goodsPrice;

    @Field(type = FieldType.Integer)
    private Integer salesNum;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Long)
    private Long parentCategoryId;

    @Field(type = FieldType.Keyword)
    private String parentCategoryName;

    @Field(type = FieldType.Long)
    private Long shopId;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;
}
