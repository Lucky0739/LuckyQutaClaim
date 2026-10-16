<template>
  <section class="goods-feed">
    <div class="goods-feed-head">
      <div class="goods-feed-head-main">
        <h2 class="goods-feed-title">{{ feedTitle }}</h2>
        <span v-if="feedHint" class="goods-feed-hint">{{ feedHint }}</span>
      </div>
      <button
        v-if="showClearCategory"
        type="button"
        class="goods-feed-clear"
        @click="emit('clear-category')"
      >
        查看全部
      </button>
    </div>

    <div v-if="showHomeSort" class="goods-feed-sort" role="tablist" aria-label="商品排序">
      <button
        v-for="item in GOODS_HOME_SORT_OPTIONS"
        :key="item.value"
        type="button"
        class="goods-feed-sort-btn"
        :class="{ 'goods-feed-sort-btn--active': homeSortType === item.value }"
        role="tab"
        :aria-selected="homeSortType === item.value"
        @click="setSortType(item.value)"
      >
        {{ item.label }}
      </button>
    </div>

    <div v-if="errorMsg && !goodsList.length" class="goods-feed-state goods-feed-state--error">
      <p>{{ errorMsg }}</p>
      <button type="button" class="goods-feed-retry" @click="retry">重新加载</button>
    </div>

    <div v-else-if="!loading && !goodsList.length && finished" class="goods-feed-state">
      <p>{{ emptyText }}</p>
      <p v-if="feedMode === 'home'" class="goods-feed-state-hint">
        仅展示已上架且审核通过的商品；请在商家中心提交上架申请并等待审核。
      </p>
      <button
        v-if="showClearCategory"
        type="button"
        class="goods-feed-retry"
        @click="emit('clear-category')"
      >
        查看全部商品
      </button>
    </div>

    <ul v-else class="goods-grid">
      <li v-for="item in goodsList" :key="item.id" class="goods-card">
        <router-link
          :to="{ name: 'GoodsDetail', params: { id: item.id } }"
          class="goods-card-link"
        >
          <div class="goods-card-image-wrap">
            <img
              v-if="item.goodsImg"
              :src="resolveFileUrl(item.goodsImg)"
              :alt="item.goodsName"
              class="goods-card-image"
              loading="lazy"
              referrerpolicy="no-referrer"
            />
            <div v-else class="goods-card-image goods-card-image--placeholder">
              <span>{{ item.goodsName.slice(0, 1) }}</span>
            </div>
          </div>
          <div class="goods-card-body">
            <p class="goods-card-name" :title="item.goodsName">{{ item.goodsName }}</p>
            <div class="goods-card-meta">
              <p class="goods-card-price">¥{{ formatGoodsPrice(item.goodsPrice) }}</p>
              <p class="goods-card-sales">{{ formatGoodsSalesNum(item.salesNum) }}</p>
            </div>
          </div>
        </router-link>
      </li>
    </ul>

    <div ref="sentinelRef" class="goods-feed-sentinel">
      <p v-if="loading" class="goods-feed-tip">加载中...</p>
      <p v-else-if="errorMsg" class="goods-feed-tip goods-feed-tip--error">
        {{ errorMsg }}
        <button type="button" class="goods-feed-retry-inline" @click="retry">重试</button>
      </p>
      <p v-else-if="finished && goodsList.length" class="goods-feed-tip">没有更多了</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, toRef } from 'vue'
import { useHomeGoodsFeed } from '@/composables/useHomeGoodsFeed'
import {
  formatGoodsPrice,
  formatGoodsSalesNum,
  getCategoryFeedTitle,
  getHomeSortLabel,
  GOODS_HOME_SORT_OPTIONS,
  isCategoryFilterActive,
  type GoodsCategorySelection,
  type GoodsHomeSortType,
} from '@/types/goods'
import { resolveFileUrl } from '@/utils/file'

const props = withDefaults(
  defineProps<{
    keyword?: string
    categoryFilter?: GoodsCategorySelection | null
  }>(),
  { keyword: '', categoryFilter: null },
)

const emit = defineEmits<{
  'clear-category': []
}>()

const keywordRef = toRef(props, 'keyword')
const categoryFilterRef = toRef(props, 'categoryFilter')
const sentinelRef = ref<HTMLElement | null>(null)
const homeSortType = ref<GoodsHomeSortType>('weight')

const {
  goodsList,
  loading,
  finished,
  errorMsg,
  feedMode,
  showHomeSort,
  setSortType,
  retry,
} = useHomeGoodsFeed(sentinelRef, {
  goodsName: keywordRef,
  categoryFilter: categoryFilterRef,
  sortType: homeSortType,
})

const showClearCategory = computed(() => isCategoryFilterActive(props.categoryFilter))

const feedTitle = computed(() => {
  if (feedMode.value === 'search') return '搜索结果'
  if (feedMode.value === 'category') return getCategoryFeedTitle(props.categoryFilter)
  return '推荐商品'
})

const feedHint = computed(() => {
  if (feedMode.value === 'search') return `「${props.keyword.trim()}」`
  if (feedMode.value === 'category') {
    const filter = props.categoryFilter
    if (filter?.categoryName && filter.parentName && filter.categoryName !== filter.parentName) {
      return `${filter.parentName} · ${filter.categoryName}`
    }
    return ''
  }
  return getHomeSortLabel(homeSortType.value)
})

const emptyText = computed(() => {
  if (feedMode.value === 'search') return '未找到相关商品'
  if (feedMode.value === 'category') return '该分类下暂无上架商品'
  return '暂无上架商品'
})
</script>

<style scoped>
.goods-feed-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 22px;
}

.goods-feed-head-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.goods-feed-title {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 7px 14px 7px 12px;
  border-radius: 999px;
  background: linear-gradient(90deg, #fff1e8 0%, #fff 100%);
  font-size: 20px;
  font-weight: 800;
  line-height: 1;
  color: #ff5000;
  letter-spacing: 0.5px;
}

.goods-feed-title::before {
  content: '';
  width: 8px;
  height: 18px;
  border-radius: 999px;
  background: linear-gradient(180deg, #ff8a00 0%, #ff5000 100%);
  box-shadow: 0 3px 8px rgba(255, 80, 0, 0.22);
}

.goods-feed-hint {
  font-size: 13px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-feed-clear {
  flex-shrink: 0;
  height: 32px;
  padding: 0 14px;
  border: 1px solid #ffd6c2;
  border-radius: 999px;
  background: #fff;
  color: #ff5000;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s;
}

.goods-feed-clear:hover {
  background: #fff7f2;
  border-color: #ff5000;
}

.goods-feed-sort {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 18px;
}

.goods-feed-sort-btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid #eee;
  border-radius: 999px;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s, color 0.18s;
}

.goods-feed-sort-btn:hover {
  border-color: #ffd6c2;
  color: var(--accent);
}

.goods-feed-sort-btn--active {
  border-color: var(--accent);
  background: #fff3eb;
  color: var(--accent);
  font-weight: 600;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  column-gap: 16px;
  row-gap: 28px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.goods-card-link {
  display: block;
  background: #fff;
  border-radius: 10px;
  overflow: visible;
  transition: transform 0.2s;
}

.goods-card-link:hover {
  transform: translateY(-2px);
}

.goods-card-image-wrap {
  aspect-ratio: 1;
  background: #f5f5f5;
  overflow: hidden;
  border-radius: 10px;
}

.goods-card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.goods-card-image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 500;
  color: #ccc;
}

.goods-card-body {
  padding: 8px 2px 0;
}

.goods-card-name {
  margin: 0 0 6px;
  height: 38px;
  font-size: 14px;
  line-height: 19px;
  color: #333;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.goods-card-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.goods-card-price {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: #ff5000;
}

.goods-card-sales {
  margin: 0;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.goods-feed-sentinel {
  padding: 32px 0 8px;
  text-align: center;
}

.goods-feed-tip {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.goods-feed-tip--error {
  color: #999;
}

.goods-feed-state {
  padding: 64px 0;
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
}

.goods-feed-state-hint {
  margin: 10px auto 0;
  max-width: 360px;
  font-size: 12px;
  line-height: 1.6;
  color: #999;
}

.goods-feed-state--error {
  color: var(--text-normal);
}

.goods-feed-retry,
.goods-feed-retry-inline {
  margin-left: 8px;
  padding: 0;
  border: none;
  background: none;
  color: var(--text-primary);
  font-size: inherit;
  cursor: pointer;
  text-decoration: underline;
}

.goods-feed-retry {
  display: block;
  margin: 12px auto 0;
}

@media (max-width: 992px) {
  .goods-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 12px;
  }

  .goods-feed-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
