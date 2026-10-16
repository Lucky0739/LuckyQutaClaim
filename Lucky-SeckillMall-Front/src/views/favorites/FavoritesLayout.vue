<template>
  <div class="page">
    <Header top-only />

    <main class="favorites-page">
      <div class="favorites-page-inner">
        <nav class="favorites-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="favorites-breadcrumb-sep">&gt;</span>
          <span>我的收藏</span>
          <span class="favorites-breadcrumb-sep">&gt;</span>
          <span class="favorites-breadcrumb-current">{{ pageTitle }}</span>
        </nav>

        <div class="favorites-card">
          <FavoritesNav :goods-count="goodsList.length" />
          <router-view />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import FavoritesNav from '@/components/favorites/FavoritesNav.vue'
import { useMyGoodsCollectList } from '@/composables/useMyGoodsCollectList'

const route = useRoute()
const { goodsList } = useMyGoodsCollectList()

const pageTitle = computed(() => (route.meta.title as string) || '我的收藏')
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.favorites-page {
  padding: 24px 0 56px;
}

.favorites-page-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

.favorites-breadcrumb {
  margin-bottom: 16px;
  font-size: 13px;
  color: #999;
}

.favorites-breadcrumb a {
  color: #666;
  text-decoration: none;
}

.favorites-breadcrumb a:hover {
  color: var(--accent);
}

.favorites-breadcrumb-sep {
  margin: 0 8px;
  color: #ccc;
}

.favorites-breadcrumb-current {
  color: #333;
}

.favorites-card {
  padding: 24px 28px 28px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.05);
}

@media (max-width: 768px) {
  .favorites-page-inner {
    padding: 0 12px;
  }

  .favorites-card {
    padding: 16px;
  }
}
</style>
