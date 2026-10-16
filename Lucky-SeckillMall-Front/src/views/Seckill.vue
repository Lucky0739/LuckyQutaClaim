<template>
  <div class="page">
    <Header />

    <main class="seckill-main">
      <div class="seckill-inner">
        <nav class="seckill-breadcrumb">
          <router-link to="/">首页</router-link>
          <span>&gt;</span>
          <span>限时秒杀</span>
        </nav>

        <section v-if="loading" class="seckill-state">加载秒杀商品...</section>
        <section v-else-if="errorMsg" class="seckill-state seckill-state--error">
          <p>{{ errorMsg }}</p>
          <button type="button" class="seckill-retry" @click="loadPage">重新加载</button>
        </section>

        <template v-else-if="activity">
          <header class="seckill-hero">
            <div class="seckill-hero-main">
              <div class="seckill-hero-badge">限时秒杀</div>
              <h1 class="seckill-hero-title">{{ activity.activityName }}</h1>
              <p v-if="activity.description" class="seckill-hero-desc">{{ activity.description }}</p>
              <div class="seckill-hero-meta">
                <span>开始 {{ formatDateTime(activity.beginTime) }}</span>
                <span>结束 {{ formatDateTime(activity.endTime) }}</span>
              </div>
            </div>
            <div class="seckill-hero-side">
              <span
                class="seckill-status-tag"
                :class="{
                  'seckill-status-tag--active': activity.status === SeckillActivityStatus.IN_PROGRESS,
                  'seckill-status-tag--pending': activity.status === SeckillActivityStatus.NOT_STARTED,
                  'seckill-status-tag--ended': activity.status === SeckillActivityStatus.ENDED,
                }"
              >
                {{ formatActivityStatus(activity.status) }}
              </span>
              <div v-if="activity.status !== SeckillActivityStatus.ENDED" class="seckill-hero-countdown">
                <span class="countdown-label">{{ countdownInfo.label }}</span>
                <div class="countdown-blocks">
                  <span class="countdown-block">{{ countdownParts.h }}</span>
                  <span class="countdown-colon">:</span>
                  <span class="countdown-block">{{ countdownParts.m }}</span>
                  <span class="countdown-colon">:</span>
                  <span class="countdown-block">{{ countdownParts.s }}</span>
                </div>
              </div>
            </div>
          </header>

          <div v-if="!goodsList.length" class="seckill-state">
            <p>该活动暂无秒杀商品</p>
          </div>

          <ul v-else class="seckill-goods-grid">
            <li
              v-for="item in goodsList"
              :key="item.id"
              :ref="(el) => setGoodsRef(item.goodsId, el)"
              class="seckill-goods-card"
              :class="{ 'seckill-goods-card--highlight': highlightGoodsId === item.goodsId }"
            >
              <RouterLink
                :to="buildGoodsDetailLink(item)"
                class="seckill-goods-link"
              >
                <div class="seckill-goods-image-wrap">
                  <img
                    v-if="item.seckillImg && !imageErrors[item.id]"
                    :src="resolveFileUrl(item.seckillImg)"
                    :alt="item.seckillTitle"
                    class="seckill-goods-image"
                    referrerpolicy="no-referrer"
                    @error="imageErrors[item.id] = true"
                  />
                  <div v-else class="seckill-goods-image seckill-goods-image--placeholder">
                    {{ (item.seckillTitle || '秒').slice(0, 1) }}
                  </div>
                  <span v-if="item.seckillStock != null && item.seckillStock <= 0" class="seckill-sold-out">
                    已抢光
                  </span>
                </div>
                <div class="seckill-goods-body">
                  <p class="seckill-goods-name" :title="item.seckillTitle">
                    {{ item.seckillTitle || '秒杀商品' }}
                  </p>
                  <div class="seckill-goods-meta">
                    <span class="seckill-goods-price">¥{{ formatSeckillPrice(item.seckillPrice) }}</span>
                    <span v-if="item.seckillStock != null" class="seckill-goods-stock">
                      库存 {{ item.seckillStock }}
                    </span>
                  </div>
                  <span v-if="item.seckillLimit" class="seckill-goods-limit">限购 {{ item.seckillLimit }} 件</span>
                </div>
              </RouterLink>
              <button
                type="button"
                class="seckill-card-buy-btn"
                :disabled="!canBuySeckillItem(item)"
                @click="handleSeckillBuyClick(item)"
              >
                {{ getSeckillItemButtonText(item) }}
              </button>
            </li>
          </ul>
        </template>
      </div>
    </main>

    <HomeFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import HomeFooter from '@/components/home/HomeFooter.vue'
import { useSeckillCountdown } from '@/composables/useHomeSeckill'
import { getActivityDetail, getActivityGoods, resolveSeckillDisplayPage } from '@/api/activity'
import { formatDateTime } from '@/composables/useAdminUserList'
import type { SeckillActivityVO, SeckillGoodsVO } from '@/types/activity'
import {
  canPurchaseSeckillGoods,
  formatActivityStatus,
  formatSeckillPrice,
  getSeckillPurchaseButtonText,
  SeckillActivityStatus,
} from '@/types/activity'
import { resolveFileUrl } from '@/utils/file'
import { SECKILL_DISPLAY_CHANGED } from '@/utils/seckillEvents'

const route = useRoute()
const router = useRouter()
const GOODS_POLL_INTERVAL_MS = 10_000

const loading = ref(false)
const errorMsg = ref('')
const activity = ref<SeckillActivityVO | null>(null)
const goodsList = ref<SeckillGoodsVO[]>([])
const imageErrors = reactive<Record<string, boolean>>({})
const goodsRefs = new Map<string, HTMLElement>()

const highlightGoodsId = computed(() => {
  const goodsId = route.query.goodsId
  return typeof goodsId === 'string' ? goodsId : ''
})

const { countdownInfo, countdownParts, startCountdown, stopCountdown } = useSeckillCountdown(
  () => activity.value,
)

const seckillPurchaseEnabled = computed(() => canPurchaseSeckillGoods(activity.value))

function isSeckillItemOutOfStock(item: SeckillGoodsVO) {
  return item.seckillStock != null && item.seckillStock <= 0
}

function canBuySeckillItem(item: SeckillGoodsVO) {
  return seckillPurchaseEnabled.value && !isSeckillItemOutOfStock(item)
}

function getSeckillItemButtonText(item: SeckillGoodsVO) {
  return getSeckillPurchaseButtonText(activity.value, {
    outOfStock: isSeckillItemOutOfStock(item),
  })
}

function buildGoodsDetailLink(item: SeckillGoodsVO) {
  return { name: 'GoodsDetail' as const, params: { id: item.goodsId } }
}

function handleSeckillBuyClick(item: SeckillGoodsVO) {
  if (!canBuySeckillItem(item)) return
  void router.push({
    name: 'GoodsDetail',
    params: { id: item.goodsId },
    query: {
      seckillGoodsId: item.id,
      skuId: item.skuId,
    },
  })
}

function setGoodsRef(goodsId: string, el: unknown) {
  if (el instanceof HTMLElement) {
    goodsRefs.set(goodsId, el)
  } else {
    goodsRefs.delete(goodsId)
  }
}

async function scrollToHighlight() {
  const goodsId = highlightGoodsId.value
  if (!goodsId) return
  await nextTick()
  goodsRefs.get(goodsId)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

async function loadPage() {
  loading.value = true
  errorMsg.value = ''
  stopCountdown()
  stopGoodsPoll()
  try {
    const param = route.params.activityId
    const explicitId = typeof param === 'string' && param.trim() ? param.trim() : null

    if (explicitId) {
      const [detail, goods] = await Promise.all([
        getActivityDetail(explicitId),
        getActivityGoods(explicitId),
      ])
      activity.value = detail
      goodsList.value = goods
      startCountdown()
      startGoodsPoll(explicitId)
    } else {
      const resolved = await resolveSeckillDisplayPage()
      if (!resolved) {
        throw new Error('暂无秒杀活动')
      }
      const detail = await getActivityDetail(resolved.activity.id)
      activity.value = detail
      goodsList.value = resolved.goods
      startCountdown()
      startGoodsPoll(resolved.activity.id)
    }

    await scrollToHighlight()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载秒杀页面失败'
    activity.value = null
    goodsList.value = []
  } finally {
    loading.value = false
  }
}

function handleSeckillDisplayChanged() {
  if (loading.value) return
  void loadPage()
}

let goodsPollTimer: ReturnType<typeof setInterval> | null = null

async function refreshGoods(activityId: string) {
  try {
    goodsList.value = await getActivityGoods(activityId)
    await scrollToHighlight()
  } catch {
    /* 轮询刷新失败时保留当前列表 */
  }
}

function stopGoodsPoll() {
  if (goodsPollTimer) {
    clearInterval(goodsPollTimer)
    goodsPollTimer = null
  }
}

function startGoodsPoll(activityId: string) {
  stopGoodsPoll()
  goodsPollTimer = setInterval(() => {
    if (document.visibilityState !== 'visible' || loading.value) return
    void refreshGoods(activityId)
  }, GOODS_POLL_INTERVAL_MS)
}

function handleVisibilityChange() {
  if (document.visibilityState !== 'visible' || loading.value) return
  const activityId = activity.value?.id
  if (!activityId) return
  void refreshGoods(activityId)
}

watch(
  () => [route.params.activityId, route.query.goodsId],
  () => {
    void loadPage()
  },
  { immediate: true },
)

onUnmounted(() => {
  stopCountdown()
  stopGoodsPoll()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener(SECKILL_DISPLAY_CHANGED, handleSeckillDisplayChanged)
})

if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', handleVisibilityChange)
}

if (typeof window !== 'undefined') {
  window.addEventListener(SECKILL_DISPLAY_CHANGED, handleSeckillDisplayChanged)
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f6f6f6;
}

.seckill-main {
  padding: 16px 0 64px;
}

.seckill-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

.seckill-breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 12px;
  color: #999;
}

.seckill-breadcrumb a {
  color: #666;
  text-decoration: none;
}

.seckill-breadcrumb a:hover {
  color: var(--accent);
}

.seckill-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 280px;
  padding: 32px;
  border-radius: 14px;
  background: #fff;
  color: #999;
  text-align: center;
}

.seckill-state--error {
  color: #f5222d;
}

.seckill-retry {
  padding: 8px 18px;
  border: 1px solid #ffd6bf;
  border-radius: 999px;
  background: #fff;
  color: var(--accent);
  cursor: pointer;
}

.seckill-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff7f2 0%, #ffffff 58%, #fffaf7 100%);
  border: 1px solid rgba(255, 80, 0, 0.12);
  box-shadow: 0 8px 24px rgba(255, 80, 0, 0.06);
}

.seckill-hero-badge {
  display: inline-flex;
  margin-bottom: 10px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 22px;
}

.seckill-hero-title {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 700;
  color: #222;
}

.seckill-hero-desc {
  margin: 0 0 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #777;
}

.seckill-hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 12px;
  color: #999;
}

.seckill-hero-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
}

.seckill-status-tag {
  padding: 0 12px;
  border-radius: 999px;
  line-height: 28px;
  font-size: 12px;
  font-weight: 600;
}

.seckill-status-tag--pending {
  color: #d46b08;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.seckill-status-tag--active {
  color: var(--accent);
  background: #fff7f2;
  border: 1px solid #ffd6bf;
}

.seckill-status-tag--ended {
  color: #8c8c8c;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
}

.seckill-hero-countdown {
  display: flex;
  align-items: center;
  gap: 10px;
}

.countdown-label {
  font-size: 13px;
  color: #888;
}

.countdown-blocks {
  display: flex;
  align-items: center;
  gap: 4px;
}

.countdown-block {
  min-width: 32px;
  height: 32px;
  padding: 0 6px;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 32px;
  text-align: center;
}

.countdown-colon {
  font-size: 14px;
  font-weight: 700;
  color: #999;
}

.seckill-goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.seckill-goods-card {
  border-radius: 12px;
  background: #fff;
  border: 1px solid #eee;
  overflow: hidden;
  transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s;
}

.seckill-goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.seckill-goods-card--highlight {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(255, 80, 0, 0.15);
}

.seckill-goods-link {
  display: block;
  color: inherit;
  text-decoration: none;
}

.seckill-goods-image-wrap {
  position: relative;
  aspect-ratio: 1;
  background: #f5f5f5;
}

.seckill-goods-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.seckill-goods-image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 700;
  color: #ccc;
}

.seckill-sold-out {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.seckill-goods-body {
  padding: 12px 14px 14px;
}

.seckill-goods-name {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.seckill-goods-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.seckill-goods-price {
  font-size: 20px;
  font-weight: 700;
  color: var(--accent);
}

.seckill-goods-stock {
  font-size: 12px;
  color: #999;
}

.seckill-goods-limit {
  display: inline-block;
  margin-top: 8px;
  padding: 0 8px;
  border-radius: 999px;
  background: #fff7f2;
  color: var(--accent);
  font-size: 11px;
  line-height: 22px;
}

.seckill-card-buy-btn {
  display: block;
  width: calc(100% - 28px);
  margin: 0 14px 14px;
  padding: 10px 0;
  border: none;
  border-radius: 999px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.18s;
}

.seckill-card-buy-btn:disabled {
  background: #f0f0f0;
  color: #999;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .seckill-inner {
    padding: 0 12px;
  }

  .seckill-hero {
    flex-direction: column;
  }

  .seckill-hero-side {
    align-items: flex-start;
  }

  .seckill-goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}
</style>
