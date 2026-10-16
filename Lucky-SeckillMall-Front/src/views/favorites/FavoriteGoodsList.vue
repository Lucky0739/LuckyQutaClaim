<template>
  <section class="favorite-goods-panel">
    <div class="favorite-goods-head">
      <div>
        <h1 class="favorite-goods-title">收藏商品</h1>
        <p class="favorite-goods-desc">查看与管理你收藏的商品，点击卡片进入详情继续购买。</p>
        <p v-if="goodsList.length" class="favorite-goods-count">共 {{ goodsList.length }} 件</p>
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

    <div v-if="loading && !goodsList.length" class="favorite-goods-state">加载收藏商品中...</div>
    <div v-else-if="errorMsg" class="favorite-goods-state favorite-goods-state--error">
      <p>{{ errorMsg }}</p>
      <button type="button" class="retry-btn" @click="loadList">重新加载</button>
    </div>
    <div v-else-if="!goodsList.length" class="favorite-goods-state">
      <p>还没有收藏任何商品</p>
      <router-link to="/" class="go-home-link">去首页逛逛</router-link>
    </div>

    <ul v-else class="favorite-goods-grid">
      <li
        v-for="item in goodsList"
        :key="item.goodsId || item.id"
        class="favorite-goods-card"
      >
        <router-link
          :to="{ name: 'GoodsDetail', params: { id: item.goodsId || item.id } }"
          class="favorite-goods-link"
        >
          <div class="favorite-goods-img-wrap">
            <span
              v-if="item.status === GoodsStatus.OFF_SHELF"
              class="favorite-goods-tag favorite-goods-tag--off"
            >
              已下架
            </span>
            <img
              v-if="item.goodsImg"
              :src="resolveFileUrl(item.goodsImg)"
              :alt="item.goodsName"
              class="favorite-goods-img"
              referrerpolicy="no-referrer"
            />
            <span v-else class="favorite-goods-img favorite-goods-img--empty">
              {{ (item.goodsName || '商').slice(0, 1) }}
            </span>
          </div>
          <div class="favorite-goods-info">
            <h2>{{ item.goodsName }}</h2>
            <p class="favorite-goods-price">¥{{ formatGoodsPrice(item.goodsPrice) }}</p>
            <p class="favorite-goods-meta">
              <span v-if="item.salesNum != null">{{ formatGoodsSalesNum(item.salesNum) }}</span>
              <span v-if="item.collectNum != null">{{ item.collectNum }} 人收藏</span>
            </p>
            <p v-if="item.collectTime" class="favorite-goods-time">
              收藏于 {{ formatCollectTime(item.collectTime) }}
            </p>
          </div>
        </router-link>
        <button
          type="button"
          class="favorite-cancel-btn"
          :disabled="cancelingId === (item.goodsId || item.id) || collectLoading"
          @click="handleCancelCollect(item)"
        >
          {{ cancelingId === (item.goodsId || item.id) ? '取消中...' : '取消收藏' }}
        </button>
      </li>
    </ul>

    <div v-if="toastMsg" class="favorite-goods-toast">{{ toastMsg }}</div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { formatCollectTime, useGoodsCollect } from '@/composables/useGoodsCollect'
import { useMyGoodsCollectList } from '@/composables/useMyGoodsCollectList'
import {
  formatGoodsPrice,
  formatGoodsSalesNum,
  GoodsStatus,
  type GoodsCollectVO,
} from '@/types/goods'
import { resolveFileUrl } from '@/utils/file'

const { goodsList, loading, errorMsg, loadList, removeByGoodsId } = useMyGoodsCollectList()
const { toastMsg, loading: collectLoading, cancelCollect } = useGoodsCollect()
const cancelingId = ref('')

async function handleCancelCollect(item: GoodsCollectVO) {
  const goodsId = item.goodsId || item.id
  if (!goodsId) return
  cancelingId.value = goodsId
  const ok = await cancelCollect(goodsId)
  cancelingId.value = ''
  if (ok) {
    removeByGoodsId(goodsId)
  }
}
</script>

<style scoped>
.favorite-goods-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.favorite-goods-title {
  margin: 0 0 8px;
  font-size: 18px;
  color: #333;
}

.favorite-goods-desc {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.favorite-goods-count {
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

.favorite-goods-state {
  padding: 48px 0;
  text-align: center;
  color: #999;
  font-size: 14px;
}

.favorite-goods-state--error {
  color: #f5222d;
}

.favorite-goods-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.favorite-goods-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  background: #fff;
}

.favorite-goods-link {
  display: block;
  flex: 1;
  transition: transform 0.18s;
}

.favorite-goods-link:hover {
  transform: translateY(-2px);
}

.favorite-goods-img-wrap {
  position: relative;
  aspect-ratio: 1;
  background: #f7f7f7;
}

.favorite-goods-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 1;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #fff;
}

.favorite-goods-tag--off {
  background: rgba(0, 0, 0, 0.55);
}

.favorite-goods-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-goods-img--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 28px;
  font-weight: 700;
}

.favorite-goods-info {
  padding: 12px;
}

.favorite-goods-info h2 {
  display: -webkit-box;
  height: 40px;
  margin: 0 0 8px;
  overflow: hidden;
  color: #333;
  font-size: 14px;
  line-height: 20px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.favorite-goods-price {
  margin: 0;
  color: var(--accent);
  font-size: 18px;
  font-weight: 700;
}

.favorite-goods-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 6px 0 0;
  color: #999;
  font-size: 12px;
}

.favorite-goods-time {
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

.favorite-goods-toast {
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
  .favorite-goods-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .favorite-goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
