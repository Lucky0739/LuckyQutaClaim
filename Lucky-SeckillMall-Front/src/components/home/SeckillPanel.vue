<template>
  <aside class="seckill-panel" @click="handlePanelClick">
    <div class="seckill-header">
      <div class="seckill-title">
        <span class="title-main">限时秒杀</span>
        <span class="title-badge">抢</span>
      </div>
      <div v-if="activityId && !isEnded" class="seckill-countdown">
        <span class="countdown-label">{{ countdownInfo.label }}</span>
        <div class="countdown-blocks">
          <span class="countdown-block">{{ countdownParts.h }}</span>
          <span class="countdown-colon">:</span>
          <span class="countdown-block">{{ countdownParts.m }}</span>
          <span class="countdown-colon">:</span>
          <span class="countdown-block">{{ countdownParts.s }}</span>
        </div>
      </div>
      <p v-else-if="isEnded" class="seckill-ended-tip">活动已结束</p>
    </div>

    <div v-if="loading" class="seckill-state">加载中...</div>
    <div v-else-if="errorMsg" class="seckill-state seckill-state--error">
      <p>{{ errorMsg }}</p>
      <button type="button" class="seckill-retry" @click.stop="loadData">重试</button>
    </div>
    <div v-else-if="!hasGoods" class="seckill-state">
      <p>暂无秒杀商品</p>
    </div>

    <ul v-else class="seckill-list">
      <li
        v-for="item in panelGoods"
        :key="item.id"
        class="seckill-item"
      >
        <RouterLink
          :to="buildGoodsLink(item)"
          class="seckill-link"
          @click.stop
        >
          <div class="item-thumb">
            <img
              v-if="item.seckillImg && !imageErrors[item.id]"
              :src="resolveFileUrl(item.seckillImg)"
              alt=""
              class="item-thumb-img"
              referrerpolicy="no-referrer"
              @error="imageErrors[item.id] = true"
            />
            <span v-else class="item-thumb-text">
              {{ (item.seckillTitle || '秒杀').slice(0, 2) }}
            </span>
          </div>
          <div class="item-info">
            <div class="item-name" :title="item.seckillTitle">
              {{ item.seckillTitle || '秒杀商品' }}
            </div>
            <div class="item-price">
              <span class="price-seckill">¥{{ formatSeckillPrice(item.seckillPrice) }}</span>
              <span v-if="item.seckillStock != null" class="price-stock">
                剩{{ item.seckillStock }}
              </span>
            </div>
          </div>
        </RouterLink>
      </li>
    </ul>

    <RouterLink
      v-if="activityId"
      :to="{ name: 'Seckill', params: { activityId: String(activityId) } }"
      class="seckill-more"
      @click.stop
    >
      查看更多 ›
    </RouterLink>
    <span v-else class="seckill-more seckill-more--disabled">查看更多 ›</span>
  </aside>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useHomeSeckill } from '@/composables/useHomeSeckill'
import { formatSeckillPrice } from '@/types/activity'
import type { SeckillGoodsVO } from '@/types/activity'
import { resolveFileUrl } from '@/utils/file'

const router = useRouter()
const imageErrors = reactive<Record<string, boolean>>({})

const {
  loading,
  errorMsg,
  panelGoods,
  hasGoods,
  isEnded,
  seckillPurchaseEnabled,
  activityId,
  countdownInfo,
  countdownParts,
  loadData,
} = useHomeSeckill()

function isItemOutOfStock(item: SeckillGoodsVO) {
  return item.seckillStock != null && item.seckillStock <= 0
}

function buildGoodsLink(item: SeckillGoodsVO) {
  if (seckillPurchaseEnabled.value && !isItemOutOfStock(item)) {
    return {
      name: 'GoodsDetail' as const,
      params: { id: item.goodsId },
      query: {
        seckillGoodsId: item.id,
        skuId: item.skuId,
      },
    }
  }
  return {
    name: 'Seckill' as const,
    params: { activityId: item.activityId },
    query: { goodsId: item.goodsId },
  }
}

function handlePanelClick(event: MouseEvent) {
  const target = event.target as HTMLElement | null
  if (target?.closest('a, button')) return
  if (!activityId.value) return
  void router.push({ name: 'Seckill', params: { activityId: String(activityId.value) } })
}
</script>

<style scoped>
.seckill-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--home-surface);
  border-radius: var(--home-card-radius);
  border: 1px solid var(--home-border);
  overflow: hidden;
  box-shadow: var(--home-card-shadow);
  cursor: pointer;
}

.seckill-header {
  flex-shrink: 0;
  padding: 16px 16px 14px;
  background: transparent;
  border-bottom: 1px solid var(--border-color);
}

.seckill-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.title-main {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.title-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.seckill-countdown {
  display: flex;
  align-items: center;
  gap: 8px;
}

.countdown-label {
  font-size: 12px;
  color: var(--text-muted);
}

.countdown-blocks {
  display: flex;
  align-items: center;
  gap: 3px;
}

.countdown-block {
  min-width: 24px;
  height: 24px;
  padding: 0 5px;
  border-radius: 6px;
  background: var(--accent);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  line-height: 24px;
  text-align: center;
}

.countdown-colon {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
}

.seckill-ended-tip {
  margin: 0;
  font-size: 12px;
  color: var(--text-muted);
}

.seckill-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
}

.seckill-state--error {
  color: #f5222d;
}

.seckill-retry {
  padding: 0;
  border: none;
  background: none;
  color: var(--accent);
  font-size: 12px;
  cursor: pointer;
}

.seckill-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 8px;
  list-style: none;
  min-height: 0;
}

.seckill-item {
  flex: 1;
  min-height: 0;
}

.seckill-link {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 100%;
  padding: 6px 10px;
  border-radius: 10px;
  transition: background 0.18s;
  text-decoration: none;
  color: inherit;
}

.seckill-link:hover {
  background: var(--accent-soft);
}

.item-thumb {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: #f5f5f5;
  overflow: hidden;
}

.item-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-thumb-text {
  font-size: 14px;
  font-weight: 600;
  color: #999;
}

.item-info {
  min-width: 0;
  flex: 1;
}

.item-name {
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.price-seckill {
  font-size: 15px;
  font-weight: 700;
  color: var(--accent);
}

.price-stock {
  font-size: 11px;
  color: var(--text-muted);
}

.seckill-more {
  flex-shrink: 0;
  display: block;
  padding: 12px 14px;
  border-top: 1px solid var(--border-color);
  background: transparent;
  font-size: 12px;
  color: var(--text-muted);
  text-align: center;
  text-decoration: none;
  transition: color 0.18s, background 0.18s;
}

.seckill-more:hover {
  color: var(--accent);
  background: var(--accent-soft);
}

.seckill-more--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
