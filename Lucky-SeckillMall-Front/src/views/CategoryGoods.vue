<template>
  <div class="page">
    <Header />

    <main class="category-main">
      <div class="category-main-inner">
        <nav class="category-breadcrumb" aria-label="面包屑">
          <router-link to="/" class="category-breadcrumb-link">首页</router-link>
          <span class="category-breadcrumb-sep">/</span>
          <span class="category-breadcrumb-current">{{ pageTitle }}</span>
        </nav>

        <section class="category-panel">
          <header class="category-panel-head">
            <h1 class="category-panel-title">{{ pageTitle }}</h1>
            <p v-if="categorySubtitle" class="category-panel-subtitle">{{ categorySubtitle }}</p>
          </header>

          <GoodsFeed
            v-if="categoryReady"
            :category-filter="categoryFilter"
            @clear-category="goHome"
          />
          <div v-else class="category-panel-state">
            <p>未找到该分类，请返回首页重试。</p>
            <router-link to="/" class="category-panel-back">返回首页</router-link>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import GoodsFeed from '@/components/home/GoodsFeed.vue'
import {
  getCategoryFeedTitle,
  isCategoryFilterActive,
  type GoodsCategorySelection,
} from '@/types/goods'
import { parseCategoryFilterFromRoute } from '@/utils/categoryNavigation'

const route = useRoute()
const router = useRouter()

const categoryFilter = ref<GoodsCategorySelection | null>(null)

const categoryReady = computed(() => isCategoryFilterActive(categoryFilter.value))

const pageTitle = computed(() => {
  if (!categoryReady.value) return '分类商品'
  return getCategoryFeedTitle(categoryFilter.value)
})

const categorySubtitle = computed(() => {
  const filter = categoryFilter.value
  if (!filter?.categoryName || !filter.parentName) return ''
  if (filter.categoryName === filter.parentName) return ''
  return `${filter.parentName} · ${filter.categoryName}`
})

function readCategoryFromRoute() {
  categoryFilter.value = parseCategoryFilterFromRoute(route.query)
}

function goHome() {
  router.push('/')
}

watch(
  () => [
    route.query.parentCategoryId,
    route.query.categoryId,
    route.query.parentName,
    route.query.categoryName,
  ],
  () => {
    readCategoryFromRoute()
    const title = pageTitle.value
    document.title = `${title} - 秒杀商城`
  },
  { immediate: true },
)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--home-page-bg);
}

.category-main {
  padding: 24px 0 48px;
}

.category-main-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

.category-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: var(--text-muted);
}

.category-breadcrumb-link {
  color: var(--text-muted);
  text-decoration: none;
  transition: color 0.18s;
}

.category-breadcrumb-link:hover {
  color: var(--accent);
}

.category-breadcrumb-sep {
  color: #ccc;
}

.category-breadcrumb-current {
  color: var(--text-primary);
  font-weight: 500;
}

.category-panel {
  padding: 24px 24px 12px;
  background: var(--home-surface);
  border-radius: var(--home-card-radius);
  border: 1px solid var(--home-border);
  box-shadow: var(--home-card-shadow);
}

.category-panel-head {
  margin-bottom: 20px;
}

.category-panel-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.category-panel-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--text-muted);
}

.category-panel-state {
  padding: 64px 0;
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
}

.category-panel-back {
  display: inline-block;
  margin-top: 12px;
  color: var(--accent);
  text-decoration: none;
}

.category-panel-back:hover {
  text-decoration: underline;
}

@media (max-width: 768px) {
  .category-panel {
    padding: 16px 14px 8px;
  }
}
</style>
