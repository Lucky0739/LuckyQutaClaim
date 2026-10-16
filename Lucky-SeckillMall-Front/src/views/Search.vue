<template>
  <div class="page">
    <Header />

    <main class="search-main">
      <div class="search-main-inner">
        <div class="search-panel">
          <div class="search-panel-head">
            <h1 class="search-panel-title">
              {{ searchType === 'shop' ? '店铺搜索' : '商品搜索' }}
            </h1>
            <p v-if="keyword" class="search-panel-keyword">
              关键词：<strong>{{ keyword }}</strong>
            </p>
          </div>

          <GoodsFeed v-if="searchType === 'goods'" :keyword="keyword" />
          <ShopFeed v-else :keyword="keyword" />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import GoodsFeed from '@/components/home/GoodsFeed.vue'
import ShopFeed from '@/components/search/ShopFeed.vue'
import { isCategoryFilterActive } from '@/types/goods'
import {
  buildCategoryGoodsRoute,
  parseCategoryFilterFromRoute,
  parseRouteQueryString,
} from '@/utils/categoryNavigation'

const route = useRoute()
const router = useRouter()

const searchType = computed(() => (route.query.type === 'shop' ? 'shop' : 'goods'))

const keyword = computed(() => parseRouteQueryString(route.query.q) ?? '')

/** 旧链接带分类参数时，重定向到分类商品页 */
function redirectCategoryQueryIfNeeded() {
  if (searchType.value !== 'goods') return
  const filter = parseCategoryFilterFromRoute(route.query)
  if (!filter || !isCategoryFilterActive(filter)) return
  router.replace(buildCategoryGoodsRoute(filter))
}

watch(
  () => [
    route.query.type,
    route.query.q,
    route.query.parentCategoryId,
    route.query.categoryId,
  ],
  () => {
    redirectCategoryQueryIfNeeded()
    const title = searchType.value === 'shop' ? '店铺搜索' : '商品搜索'
    const suffix = keyword.value
    document.title = suffix ? `${title} - ${suffix} - 秒杀商城` : `${title} - 秒杀商城`
  },
  { immediate: true },
)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--home-page-bg);
}

.search-main {
  padding: 24px 0 48px;
}

.search-main-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

.search-panel {
  padding: 24px 24px 12px;
  background: var(--home-surface);
  border-radius: var(--home-card-radius);
  border: 1px solid var(--home-border);
  box-shadow: var(--home-card-shadow);
}

.search-panel-head {
  margin-bottom: 20px;
}

.search-panel-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.search-panel-keyword {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.search-panel-keyword strong {
  color: var(--accent);
  font-weight: 600;
}

@media (max-width: 768px) {
  .search-panel {
    padding: 16px 14px 8px;
  }
}
</style>
