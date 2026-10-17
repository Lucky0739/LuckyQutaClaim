package com.lucky.quota.goods.elasticsearch.service.impl;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.lucky.quota.common.model.entity.PageResult;
import com.lucky.quota.goods.config.GoodsElasticsearchProperties;
import com.lucky.quota.goods.elasticsearch.document.GoodsDocument;
import com.lucky.quota.goods.elasticsearch.service.GoodsElasticsearchService;
import com.lucky.quota.goods.elasticsearch.support.GoodsDocumentConverter;
import com.lucky.quota.goods.mapper.GoodsCategoryMapper;
import com.lucky.quota.goods.mapper.GoodsMapper;
import com.lucky.quota.goods.mapper.GoodsSkuMapper;
import com.lucky.quota.goods.model.dto.GoodsSearchRequest;
import com.lucky.quota.goods.model.entity.Goods;
import com.lucky.quota.goods.model.entity.GoodsCategory;
import com.lucky.quota.goods.model.entity.GoodsSku;
import com.lucky.quota.goods.model.vo.GoodsListVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class GoodsElasticsearchServiceImpl implements GoodsElasticsearchService {

    private static final long PARENT_CATEGORY_ROOT_ID = 0L;

    private final GoodsElasticsearchProperties properties;
    private final GoodsMapper goodsMapper;
    private final GoodsSkuMapper goodsSkuMapper;
    private final GoodsCategoryMapper goodsCategoryMapper;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    @Override
    public void syncGoods(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        ensureIndexExists();
        Goods goods = goodsMapper.selectByPrimaryKey(goodsId);
        if (!GoodsDocumentConverter.isSearchable(goods)) {
            removeGoods(goodsId);
            return;
        }
        elasticsearchOperations.save(buildDocument(goods), indexCoordinates());
        log.debug("==> 物资 ES 索引已同步, goodsId: {}", goodsId);
    }

    @Override
    public void removeGoods(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        elasticsearchOperations.delete(String.valueOf(goodsId), indexCoordinates());
        log.debug("==> 物资 ES 索引已删除, goodsId: {}", goodsId);
    }

    @Override
    public void rebuildIndex() {
        recreateIndex();
        List<Goods> goodsList = goodsMapper.selectSearchableGoods();
        if (goodsList == null || goodsList.isEmpty()) {
            log.info("==> 物资 ES 全量重建完成，无可搜索物资");
            return;
        }
        List<GoodsDocument> documents = goodsList.stream()
                .map(this::buildDocument)
                .collect(Collectors.toList());
        elasticsearchOperations.save(documents, indexCoordinates());
        log.info("==> 物资 ES 全量重建完成, count: {}, index: {}", documents.size(), properties.getIndexName());
    }

    @Override
    public PageResult<GoodsListVO> search(String keyword, List<Long> categoryIds, GoodsSearchRequest request) {
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : request.getPageSize();

        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (StringUtils.hasText(keyword)) {
            String trimmed = keyword.trim();
            bool.should(Query.of(q -> q.match(m -> m.field("goodsName").query(trimmed))));
            String wildcardValue = "*" + escapeWildcard(trimmed) + "*";
            bool.should(Query.of(q -> q.wildcard(w -> w.field("goodsName.keyword").value(wildcardValue))));
            bool.minimumShouldMatch("1");
        }
        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<FieldValue> values = categoryIds.stream().map(FieldValue::of).toList();
            bool.filter(Query.of(q -> q.terms(t -> t.field("categoryId")
                    .terms(tv -> tv.value(values)))));
        } else if (request.getCategoryId() != null) {
            bool.filter(Query.of(q -> q.term(t -> t.field("categoryId").value(request.getCategoryId()))));
        }
        if (request.getShopId() != null) {
            bool.filter(Query.of(q -> q.term(t -> t.field("shopId").value(request.getShopId()))));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(bool.build()))
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(s -> s.field(f -> f.field("createTime").order(SortOrder.Desc)))
                .build();

        SearchHits<GoodsDocument> hits = elasticsearchOperations.search(query, GoodsDocument.class, indexCoordinates());
        List<GoodsListVO> records = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(GoodsDocumentConverter::toListVO)
                .collect(Collectors.toList());

        long total = hits.getTotalHits();
        int totalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return PageResult.<GoodsListVO>builder()
                .records(records)
                .total(total)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .build();
    }

    private void ensureIndexExists() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(indexCoordinates());
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(GoodsDocument.class);
            log.info("==> 物资 ES 索引已创建, index: {}", properties.getIndexName());
        }
    }

    private void recreateIndex() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(indexCoordinates());
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
        indexOps.putMapping(GoodsDocument.class);
        log.info("==> 物资 ES 索引已重建, index: {}", properties.getIndexName());
    }

    private IndexCoordinates indexCoordinates() {
        return IndexCoordinates.of(properties.getIndexName());
    }

    private GoodsDocument buildDocument(Goods goods) {
        List<GoodsSku> skuList = goodsSkuMapper.selectByGoodsId(goods.getId());
        int salesNum = GoodsDocumentConverter.sumSalesNum(skuList);
        GoodsCategory category = goods.getCategoryId() == null
                ? null
                : goodsCategoryMapper.selectByPrimaryKey(goods.getCategoryId());
        GoodsCategory parentCategory = null;
        if (category != null && !isParentCategory(category) && category.getParentId() != null) {
            parentCategory = goodsCategoryMapper.selectByPrimaryKey(category.getParentId());
        }
        return GoodsDocumentConverter.toDocument(goods, salesNum, category, parentCategory);
    }

    private boolean isParentCategory(GoodsCategory category) {
        Long parentId = category.getParentId();
        return parentId == null || parentId == PARENT_CATEGORY_ROOT_ID;
    }

    private static String escapeWildcard(String keyword) {
        StringBuilder sb = new StringBuilder(keyword.length());
        for (char ch : keyword.toCharArray()) {
            if (ch == '\\' || ch == '*' || ch == '?' || ch == '[' || ch == ']') {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}
