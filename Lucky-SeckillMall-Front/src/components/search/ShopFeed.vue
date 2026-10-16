<template>
  <section class="shop-feed">
    <div class="shop-feed-head">
      <h2 class="shop-feed-title">{{ feedTitle }}</h2>
      <span v-if="feedHint" class="shop-feed-hint">{{ feedHint }}</span>
    </div>

    <div v-if="errorMsg && !shopList.length" class="shop-feed-state shop-feed-state--error">
      <p>{{ errorMsg }}</p>
      <button type="button" class="shop-feed-retry" @click="retry">重新加载</button>
    </div>

    <div v-else-if="!loading && !shopList.length && finished" class="shop-feed-state">
      <p>{{ emptyText }}</p>
    </div>

    <ul v-else class="shop-grid">
      <li v-for="item in shopList" :key="item.id" class="shop-card">
        <div class="shop-card-inner">
          <div class="shop-card-avatar-wrap">
            <img
              v-if="item.shopAvatar"
              :src="resolveFileUrl(item.shopAvatar)"
              :alt="item.shopName"
              class="shop-card-avatar"
              loading="lazy"
              referrerpolicy="no-referrer"
            />
            <div v-else class="shop-card-avatar shop-card-avatar--placeholder">
              <span>{{ item.shopName.slice(0, 1) }}</span>
            </div>
          </div>
          <div class="shop-card-body">
            <h3 class="shop-card-name" :title="item.shopName">{{ item.shopName }}</h3>
            <p v-if="item.shopDesc" class="shop-card-desc" :title="item.shopDesc">{{ item.shopDesc }}</p>
            <p v-else class="shop-card-desc shop-card-desc--empty">暂无店铺简介</p>
          </div>
          <router-link :to="shopDetailRoute(item)" class="shop-enter-btn">
            进入店铺
          </router-link>
        </div>
      </li>
    </ul>

    <div ref="sentinelRef" class="shop-feed-sentinel">
      <p v-if="loading" class="shop-feed-tip">加载中...</p>
      <p v-else-if="errorMsg" class="shop-feed-tip shop-feed-tip--error">
        {{ errorMsg }}
        <button type="button" class="shop-feed-retry-inline" @click="retry">重试</button>
      </p>
      <p v-else-if="finished && shopList.length" class="shop-feed-tip">没有更多了</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref, toRef } from 'vue'
import { useRoute } from 'vue-router'
import { useShopSearchFeed } from '@/composables/useShopSearchFeed'
import type { ShopListVO } from '@/types/shop'
import { resolveFileUrl } from '@/utils/file'

const route = useRoute()

const props = withDefaults(defineProps<{ keyword?: string }>(), { keyword: '' })

const keywordRef = toRef(props, 'keyword')
const feedTitle = computed(() => (props.keyword.trim() ? '搜索结果' : '全部店铺'))
const feedHint = computed(() => (props.keyword.trim() ? `「${props.keyword.trim()}」` : ''))
const emptyText = computed(() =>
  props.keyword.trim() ? '未找到相关店铺' : '暂无营业中的店铺',
)

const sentinelRef = ref<HTMLElement | null>(null)
const { shopList, loading, finished, errorMsg, retry } = useShopSearchFeed(sentinelRef, keywordRef)

function shopDetailRoute(item: ShopListVO) {
  return {
    name: 'ShopDetail' as const,
    params: { id: item.id },
    state: {
      shopName: item.shopName,
      shopAvatar: item.shopAvatar ?? '',
      shopDesc: item.shopDesc ?? '',
      fromSearch: route.path === '/search' ? '1' : '',
    },
  }
}
</script>

<style scoped>
.shop-feed-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.shop-feed-title {
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

.shop-feed-title::before {
  content: '';
  width: 8px;
  height: 18px;
  border-radius: 999px;
  background: linear-gradient(180deg, #ff8a00 0%, #ff5000 100%);
  box-shadow: 0 3px 8px rgba(255, 80, 0, 0.22);
}

.shop-feed-hint {
  font-size: 13px;
  color: var(--text-muted);
}

.shop-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.shop-card-inner {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--home-border);
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.shop-card-inner:hover {
  border-color: #ffd6bf;
  box-shadow: 0 4px 16px rgba(255, 80, 0, 0.1);
}

.shop-enter-btn {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--accent);
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: var(--accent);
  text-decoration: none;
  white-space: nowrap;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.shop-enter-btn:hover {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.shop-card-avatar-wrap {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.shop-card-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.shop-card-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 600;
  color: #ccc;
}

.shop-card-body {
  flex: 1;
  min-width: 0;
}

.shop-card-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-card-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.shop-card-desc--empty {
  color: #bbb;
}

.shop-feed-sentinel {
  padding: 32px 0 8px;
  text-align: center;
}

.shop-feed-tip {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.shop-feed-tip--error {
  color: #999;
}

.shop-feed-state {
  padding: 64px 0;
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
}

.shop-feed-state--error {
  color: var(--text-normal);
}

.shop-feed-retry,
.shop-feed-retry-inline {
  margin-left: 8px;
  padding: 0;
  border: none;
  background: none;
  color: var(--text-primary);
  font-size: inherit;
  cursor: pointer;
  text-decoration: underline;
}

.shop-feed-retry {
  display: block;
  margin: 12px auto 0;
}

@media (max-width: 992px) {
  .shop-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .shop-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .shop-card-inner {
    flex-wrap: wrap;
  }

  .shop-enter-btn {
    width: 100%;
    margin-top: 4px;
  }
}
</style>
