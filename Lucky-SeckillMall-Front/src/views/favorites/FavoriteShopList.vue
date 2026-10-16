<template>
  <section class="favorite-shops-panel">
    <div class="favorite-shops-head">
      <div>
        <h1 class="favorite-shops-title">收藏店铺</h1>
        <p class="favorite-shops-desc">查看与管理你收藏的店铺，点击卡片进入店铺继续逛商品。</p>
        <p v-if="shopList.length" class="favorite-shops-count">共 {{ shopList.length }} 家</p>
      </div>
      <button
        type="button"
        class="refresh-btn"
        :disabled="loading"
        @click="loadList"
      >
        {{ loading ? '加载中...' : '刷新' }}
      </button>
    </div>

    <div v-if="loading && !shopList.length" class="favorite-shops-state">加载收藏店铺中...</div>
    <div v-else-if="errorMsg" class="favorite-shops-state favorite-shops-state--error">
      <p>{{ errorMsg }}</p>
      <button type="button" class="retry-btn" @click="loadList">重新加载</button>
    </div>
    <div v-else-if="!shopList.length" class="favorite-shops-state">
      <p>还没有收藏任何店铺</p>
      <router-link to="/search?type=shop" class="go-home-link">去逛逛店铺</router-link>
    </div>

    <ul v-else class="favorite-shops-grid">
      <li
        v-for="item in shopList"
        :key="item.shopId"
        class="favorite-shops-card"
      >
        <router-link
          :to="shopDetailRoute(item)"
          class="favorite-shops-link"
        >
          <div class="favorite-shops-avatar-wrap">
            <img
              v-if="item.shopAvatar"
              :src="resolveFileUrl(item.shopAvatar)"
              :alt="item.shopName"
              class="favorite-shops-avatar"
              referrerpolicy="no-referrer"
            />
            <span v-else class="favorite-shops-avatar favorite-shops-avatar--empty">
              {{ (item.shopName || '店').slice(0, 1) }}
            </span>
          </div>
          <div class="favorite-shops-info">
            <h2>{{ item.shopName }}</h2>
            <p v-if="item.shopDesc" class="favorite-shops-desc-text">{{ item.shopDesc }}</p>
            <p v-else class="favorite-shops-desc-text favorite-shops-desc-text--empty">暂无店铺简介</p>
            <p class="favorite-shops-meta">
              <span v-if="item.collectNum != null">{{ item.collectNum }} 人收藏</span>
            </p>
            <p v-if="item.collectTime" class="favorite-shops-time">
              收藏于 {{ formatCollectTime(item.collectTime) }}
            </p>
          </div>
        </router-link>
        <button
          type="button"
          class="favorite-cancel-btn"
          :disabled="cancelingId === item.shopId || collectLoading"
          @click="handleCancelCollect(item)"
        >
          {{ cancelingId === item.shopId ? '取消中...' : '取消收藏' }}
        </button>
      </li>
    </ul>

    <div v-if="toastMsg" class="favorite-shops-toast">{{ toastMsg }}</div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { formatCollectTime } from '@/composables/useGoodsCollect'
import { useMyShopCollectList } from '@/composables/useMyShopCollectList'
import { useShopCollect } from '@/composables/useShopCollect'
import type { ShopCollectVO } from '@/types/shop'
import { resolveFileUrl } from '@/utils/file'

const { shopList, loading, errorMsg, loadList, removeByShopId } = useMyShopCollectList()
const { toastMsg, loading: collectLoading, cancelCollect } = useShopCollect()
const cancelingId = ref('')

function shopDetailRoute(item: ShopCollectVO) {
  return {
    name: 'ShopDetail' as const,
    params: { id: item.shopId },
    state: {
      shopName: item.shopName,
      shopAvatar: item.shopAvatar ?? '',
      shopDesc: item.shopDesc ?? '',
    },
  }
}

async function handleCancelCollect(item: ShopCollectVO) {
  if (!item.shopId) return
  cancelingId.value = item.shopId
  const ok = await cancelCollect(item.shopId)
  cancelingId.value = ''
  if (ok) {
    removeByShopId(item.shopId)
  }
}
</script>

<style scoped>
.favorite-shops-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.favorite-shops-title {
  margin: 0 0 8px;
  font-size: 18px;
  color: #333;
}

.favorite-shops-desc {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.favorite-shops-count {
  margin: 8px 0 0;
  font-size: 13px;
  color: #666;
}

.refresh-btn,
.retry-btn,
.go-home-link {
  flex-shrink: 0;
  border: none;
  border-radius: 999px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  text-decoration: none;
}

.refresh-btn {
  height: 34px;
  padding: 0 18px;
}

.refresh-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.retry-btn {
  display: inline-block;
  height: 30px;
  margin-top: 10px;
  padding: 0 16px;
  line-height: 30px;
}

.go-home-link {
  display: inline-block;
  height: 34px;
  margin-top: 14px;
  padding: 0 20px;
  line-height: 34px;
}

.favorite-shops-state {
  padding: 48px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.favorite-shops-state--error {
  color: #f5222d;
}

.favorite-shops-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.favorite-shops-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fff;
}

.favorite-shops-link {
  display: flex;
  gap: 14px;
  flex: 1;
  padding: 16px;
  transition: transform 0.18s;
}

.favorite-shops-link:hover {
  transform: translateY(-2px);
}

.favorite-shops-avatar-wrap {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  background: #f7f7f7;
}

.favorite-shops-avatar {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-shops-avatar--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 28px;
  font-weight: 700;
}

.favorite-shops-info {
  flex: 1;
  min-width: 0;
}

.favorite-shops-info h2 {
  margin: 0 0 8px;
  overflow: hidden;
  color: #333;
  font-size: 16px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favorite-shops-desc-text {
  display: -webkit-box;
  margin: 0 0 8px;
  overflow: hidden;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.favorite-shops-desc-text--empty {
  color: #bbb;
}

.favorite-shops-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  color: #999;
  font-size: 12px;
}

.favorite-shops-time {
  margin: 4px 0 0;
  color: #bbb;
  font-size: 12px;
}

.favorite-cancel-btn {
  width: 100%;
  height: 36px;
  border: none;
  border-top: 1px solid #f0f0f0;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.18s, color 0.18s;
}

.favorite-cancel-btn:hover:not(:disabled) {
  background: #fff5f7;
  color: var(--accent);
}

.favorite-cancel-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.favorite-shops-toast {
  position: fixed;
  left: 50%;
  bottom: 72px;
  z-index: 1200;
  transform: translateX(-50%);
  padding: 10px 18px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.78);
  color: #fff;
  font-size: 14px;
  pointer-events: none;
}

@media (max-width: 992px) {
  .favorite-shops-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .favorite-shops-grid {
    grid-template-columns: 1fr;
  }
}
</style>
