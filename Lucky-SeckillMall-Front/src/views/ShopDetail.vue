<template>
  <div class="page">
    <Header top-only />

    <main class="shop-detail-main">
      <div class="shop-detail-inner">
        <nav class="shop-detail-breadcrumb">
          <router-link to="/">首页</router-link>
          <span>&gt;</span>
          <router-link v-if="fromSearch" to="/search?type=shop">店铺搜索</router-link>
          <span v-if="fromSearch">&gt;</span>
          <span class="shop-detail-breadcrumb-current">{{ shopName }}</span>
        </nav>

        <section class="shop-detail-header">
          <div class="shop-detail-avatar-wrap">
            <img
              v-if="shopAvatar"
              :src="resolveFileUrl(shopAvatar)"
              :alt="shopName"
              class="shop-detail-avatar"
              referrerpolicy="no-referrer"
            />
            <div v-else class="shop-detail-avatar shop-detail-avatar--placeholder">
              <span>{{ shopName.slice(0, 1) }}</span>
            </div>
          </div>
          <div class="shop-detail-info">
            <h1 class="shop-detail-name">{{ shopName }}</h1>
            <p v-if="shopInfoLoading" class="shop-detail-desc shop-detail-desc--empty">店铺信息加载中...</p>
            <p v-else-if="shopDesc" class="shop-detail-desc">{{ shopDesc }}</p>
            <p v-else class="shop-detail-desc shop-detail-desc--empty">暂无店铺简介</p>
            <p v-if="shopInfoError" class="shop-detail-info-error">{{ shopInfoError }}</p>
            <div class="shop-detail-meta-row">
              <p v-if="total > 0" class="shop-detail-meta">共 {{ total }} 件在售商品</p>
              <p class="shop-detail-collect-count">{{ collectCount }} 人收藏</p>
            </div>
          </div>
          <div class="shop-detail-actions">
            <button
              type="button"
              class="shop-collect-btn"
              :disabled="collectLoading || !shopId"
              @click="handleToggleCollect"
            >
              <img :src="favoriteButtonIcon" alt="" class="shop-collect-icon" />
              {{ collected ? '已收藏' : '收藏店铺' }}
            </button>
          </div>
        </section>

        <section class="shop-detail-goods">
          <div class="shop-detail-goods-head">
            <h2 class="shop-detail-goods-title">店铺商品</h2>
          </div>

          <div v-if="!shopId" class="shop-detail-state shop-detail-state--error">
            <p>店铺 ID 无效</p>
            <router-link to="/search?type=shop" class="shop-detail-back-link">返回店铺搜索</router-link>
          </div>

          <div v-else-if="errorMsg && !goodsList.length" class="shop-detail-state shop-detail-state--error">
            <p>{{ errorMsg }}</p>
            <button type="button" class="shop-detail-retry" @click="retry">重新加载</button>
          </div>

          <div v-else-if="!loading && !goodsList.length && finished" class="shop-detail-state">
            <p>该店铺暂无上架商品</p>
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

          <div ref="sentinelRef" class="shop-detail-sentinel">
            <p v-if="loading" class="shop-detail-tip">加载中...</p>
            <p v-else-if="errorMsg" class="shop-detail-tip shop-detail-tip--error">
              {{ errorMsg }}
              <button type="button" class="shop-detail-retry-inline" @click="retry">重试</button>
            </p>
            <p v-else-if="finished && goodsList.length" class="shop-detail-tip">没有更多了</p>
          </div>
        </section>
      </div>
    </main>

    <div v-if="collectToastMsg" class="shop-collect-toast">{{ collectToastMsg }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import { getShopDetail } from '@/api/shop'
import { useShopGoodsFeed } from '@/composables/useShopGoodsFeed'
import { useShopCollect } from '@/composables/useShopCollect'
import { formatGoodsPrice, formatGoodsSalesNum } from '@/types/goods'
import type { ShopListVO } from '@/types/shop'
import { resolveFileUrl } from '@/utils/file'
import favoriteIcon from '@/assets/shoucang.png'
import collectedFavoriteIcon from '@/assets/yishoucang.png'

interface ShopDetailRouteState {
  shopName?: string
  shopAvatar?: string
  shopDesc?: string
  fromSearch?: string
}

const route = useRoute()
const routeState = (history.state ?? {}) as ShopDetailRouteState

const shopId = computed(() => String(route.params.id ?? '').trim())
const shopInfo = ref<Partial<ShopListVO>>({
  shopName: routeState.shopName,
  shopAvatar: routeState.shopAvatar || undefined,
  shopDesc: routeState.shopDesc || undefined,
})
const shopInfoLoading = ref(false)
const shopInfoError = ref('')
const fromSearch = computed(() => routeState.fromSearch === '1')

const shopName = computed(() => shopInfo.value.shopName?.trim() || '店铺')
const shopAvatar = computed(() => shopInfo.value.shopAvatar)
const shopDesc = computed(() => shopInfo.value.shopDesc)

const sentinelRef = ref<HTMLElement | null>(null)
const shopIdRef = computed(() => shopId.value)

const { goodsList, loading, finished, errorMsg, total, retry: retryGoods } = useShopGoodsFeed(
  shopIdRef,
  sentinelRef,
)

const {
  collected,
  collectCount,
  loading: collectLoading,
  toastMsg: collectToastMsg,
  loadCollectStatus,
  toggleCollect,
} = useShopCollect()

const favoriteButtonIcon = computed(() => (collected.value ? collectedFavoriteIcon : favoriteIcon))

function handleToggleCollect() {
  void toggleCollect(shopId.value)
}

async function loadShopInfo(id: string) {
  shopInfoLoading.value = true
  shopInfoError.value = ''
  try {
    shopInfo.value = await getShopDetail(id)
  } catch (err) {
    shopInfoError.value = err instanceof Error ? err.message : '加载店铺信息失败'
  } finally {
    shopInfoLoading.value = false
  }
}

function retry() {
  retryGoods()
  if (shopId.value) {
    void loadShopInfo(shopId.value)
  }
}

watch(
  shopId,
  (id) => {
    if (!id) return
    void loadShopInfo(id)
    void loadCollectStatus(id)
  },
  { immediate: true },
)

</script>

<style scoped>
.page {
  min-height: 100vh;
  background: var(--home-page-bg);
}

.shop-detail-main {
  padding: 24px 0 48px;
}

.shop-detail-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

.shop-detail-breadcrumb {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 12px;
  color: var(--text-muted);
}

.shop-detail-breadcrumb a {
  color: var(--text-muted);
  text-decoration: none;
}

.shop-detail-breadcrumb a:hover {
  color: var(--accent);
}

.shop-detail-breadcrumb-current {
  color: var(--text-primary);
}

.shop-detail-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 24px;
  margin-bottom: 24px;
  background: var(--home-surface);
  border: 1px solid var(--home-border);
  border-radius: var(--home-card-radius);
  box-shadow: var(--home-card-shadow);
}

.shop-detail-avatar-wrap {
  flex-shrink: 0;
  width: 88px;
  height: 88px;
  border-radius: 10px;
  overflow: hidden;
  background: #f5f5f5;
}

.shop-detail-avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.shop-detail-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
  color: #ccc;
}

.shop-detail-info {
  flex: 1;
  min-width: 0;
}

.shop-detail-name {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}

.shop-detail-desc {
  margin: 0 0 8px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--text-muted);
}

.shop-detail-desc--empty {
  color: #bbb;
}

.shop-detail-info-error {
  margin: -2px 0 8px;
  color: #d46b08;
  font-size: 12px;
}

.shop-detail-meta {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.shop-detail-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.shop-detail-collect-count {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.shop-detail-actions {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
}

.shop-collect-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #ffd6bf;
  border-radius: 999px;
  background: #fff;
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s;
}

.shop-collect-btn:hover:not(:disabled) {
  background: #fff7f2;
  border-color: var(--accent);
}

.shop-collect-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.shop-collect-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.shop-collect-toast {
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

.shop-detail-goods {
  padding: 24px 24px 12px;
  background: var(--home-surface);
  border: 1px solid var(--home-border);
  border-radius: var(--home-card-radius);
  box-shadow: var(--home-card-shadow);
}

.shop-detail-goods-head {
  margin-bottom: 20px;
}

.shop-detail-goods-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
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

.shop-detail-sentinel {
  padding: 32px 0 8px;
  text-align: center;
}

.shop-detail-tip {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.shop-detail-tip--error {
  color: #999;
}

.shop-detail-state {
  padding: 64px 0;
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
}

.shop-detail-state--error {
  color: var(--text-normal);
}

.shop-detail-retry,
.shop-detail-retry-inline {
  margin-left: 8px;
  padding: 0;
  border: none;
  background: none;
  color: var(--text-primary);
  font-size: inherit;
  cursor: pointer;
  text-decoration: underline;
}

.shop-detail-retry {
  display: block;
  margin: 12px auto 0;
}

.shop-detail-back-link {
  display: inline-block;
  margin-top: 12px;
  color: var(--accent);
  font-size: 14px;
}

@media (max-width: 992px) {
  .goods-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .shop-detail-header {
    flex-direction: column;
    padding: 16px;
  }

  .shop-detail-actions {
    width: 100%;
  }

  .shop-collect-btn {
    width: 100%;
    justify-content: center;
  }

  .shop-detail-goods {
    padding: 16px 14px 8px;
  }

  .goods-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 12px;
  }
}
</style>
