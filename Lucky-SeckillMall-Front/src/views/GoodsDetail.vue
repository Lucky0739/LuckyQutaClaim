<template>
  <div class="page">
    <Header top-only />

    <main class="detail-main">
      <div class="detail-inner">
        <nav class="detail-breadcrumb">
          <router-link to="/">首页</router-link>
          <span>&gt;</span>
          <span>商品详情</span>
        </nav>

        <section v-if="loading" class="detail-state">加载商品详情...</section>
        <section v-else-if="errorMsg" class="detail-state detail-state--error">
          <p>{{ errorMsg }}</p>
          <button type="button" class="detail-retry" @click="loadDetail">重新加载</button>
        </section>

        <template v-else-if="goods">
          <section class="detail-card">
            <div class="detail-gallery">
              <div class="thumb-list">
                <button
                  v-for="(img, index) in baseImageList"
                  :key="img"
                  type="button"
                  class="thumb-btn"
                  :class="{ active: !showingSkuPreview && activeImageIndex === index }"
                  @click="selectMainImage(index)"
                >
                  <img :src="resolveFileUrl(img)" alt="" class="thumb-img" referrerpolicy="no-referrer" />
                </button>
                <span v-if="!baseImageList.length" class="thumb-btn thumb-btn--empty">图</span>
              </div>

              <div class="gallery-main">
                <div
                  class="main-image-wrap"
                  @mouseenter="handleCarouselEnter"
                  @mousemove="handleZoomMove"
                  @mouseleave="handleCarouselLeave"
                >
                  <img
                    v-if="currentImage"
                    :src="resolveFileUrl(currentImage)"
                    :alt="goods.goodsName"
                    class="main-image"
                    referrerpolicy="no-referrer"
                  />
                  <div v-else class="main-image main-image--placeholder">暂无图片</div>
                  <div v-if="zoomActive && currentImage" class="zoom-lens" :style="zoomLensStyle"></div>
                  <div v-if="zoomActive && currentImage" class="zoom-preview" :style="zoomPreviewStyle"></div>
                  <button
                    v-if="!showingSkuPreview && baseImageList.length > 1"
                    type="button"
                    class="image-arrow image-arrow--prev"
                    aria-label="上一张"
                    @click.stop="showPrevImage"
                  >
                    ‹
                  </button>
                  <button
                    v-if="!showingSkuPreview && baseImageList.length > 1"
                    type="button"
                    class="image-arrow image-arrow--next"
                    aria-label="下一张"
                    @click.stop="showNextImage"
                  >
                    ›
                  </button>
                  <div v-if="!showingSkuPreview && baseImageList.length > 1" class="image-indicator">
                    {{ activeImageIndex + 1 }} / {{ baseImageList.length }}
                  </div>
                  <div v-if="skuImageLoading" class="gallery-loading">规格图片加载中...</div>
                </div>
              </div>
            </div>

            <div class="detail-info">
              <h1 class="goods-title">{{ goods.goodsName }}</h1>

              <div class="stock-line">库存 {{ totalStock }}</div>

              <div v-if="seckillContext" class="seckill-price-banner">
                <span class="seckill-price-label">秒杀价</span>
                <p class="seckill-price-value">
                  <span class="price-symbol">¥</span>{{ formatSeckillPrice(seckillContext.seckillPrice) }}
                </p>
                <span v-if="seckillContext.seckillLimit" class="seckill-price-limit">
                  限购 {{ seckillContext.seckillLimit }} 件
                </span>
              </div>

              <div class="price-block" :class="{ 'price-block--secondary': !!seckillContext }">
                <span class="price-label">{{ seckillContext ? '原价' : '售价' }}</span>
                <p class="price"><span class="price-symbol">¥</span>{{ formatGoodsPrice(displayPrice) }}</p>
                <span class="sold-count">已售 {{ goods.salesNum ?? 0 }}+</span>
              </div>

              <div class="info-tags">
                <span>技术不符包退</span>
                <span>24小时发货</span>
                <span>全国通用</span>
              </div>

              <div class="buy-panel">
                <section class="sku-section">
                  <div class="section-head">
                    <h2>规格</h2>
                  </div>
                  <div v-if="skuList.length" class="sku-list">
                    <button
                    v-for="(sku, index) in skuList"
                    :key="getSkuKey(sku, index)"
                      type="button"
                      class="sku-item"
                    :class="{ active: selectedSkuKey === getSkuKey(sku, index) }"
                    @click="selectSku(sku, index)"
                    >
                      <img
                        v-if="getSkuThumb(sku)"
                        :src="resolveFileUrl(getSkuThumb(sku))"
                        alt=""
                        class="sku-img"
                        referrerpolicy="no-referrer"
                      />
                      <span class="sku-name">{{ sku.skuSpecName || '默认规格' }}</span>
                      <span class="sku-price">¥{{ formatGoodsPrice(sku.salePrice ?? goods.goodsPrice) }}</span>
                    </button>
                  </div>
                  <p v-else class="sku-empty">默认规格</p>
                </section>

                <div class="quantity-row">
                  <span>数量</span>
                  <div class="quantity-stepper">
                    <button type="button" @click="changeQuantity(-1)">-</button>
                    <strong>{{ quantity }}</strong>
                    <button type="button" @click="changeQuantity(1)">+</button>
                  </div>
                </div>

                <div class="detail-actions">
                  <template v-if="isSeckillPurchaseChannel">
                    <button
                      type="button"
                      class="buy-btn seckill-buy-btn"
                      :disabled="!canSubmitSeckill || seckillSubmitting"
                      @click="handleSeckillBuy"
                    >
                      {{ seckillSubmitting ? '抢购中...' : seckillBuyButtonText }}
                    </button>
                  </template>
                  <template v-else>
                    <button
                      type="button"
                      class="cart-btn"
                      :disabled="cartAdding || !selectedSku?.id"
                      @click="handleAddToCart"
                    >
                      {{ cartAdding ? '加入中...' : '加入购物车' }}
                    </button>
                    <button type="button" class="buy-btn" :disabled="orderSubmitting" @click="openOrderModal">
                      {{ orderSubmitting ? '下单中...' : '立即购买' }}
                    </button>
                  </template>
                  <button
                    type="button"
                    class="fav-btn"
                    :disabled="collectLoading"
                    @click="handleToggleCollect"
                  >
                    <img :src="favoriteButtonIcon" alt="" class="fav-icon" />
                    {{ collected ? '已收藏' : '收藏' }}
                  </button>
                </div>
                <p v-if="isSeckillPurchaseChannel && seckillContext" class="seckill-buy-tip">
                  秒杀活动进行中，请通过秒杀通道下单
                </p>
                <p class="collect-count">{{ collectCount }} 人收藏</p>
              </div>

              <dl class="service-list">
                <div>
                  <dt>优惠</dt>
                  <dd>领券立减 · 下单享秒杀价</dd>
                </div>
                <div>
                  <dt>发货</dt>
                  <dd>默认快递免运费，付款后 24 小时内发出</dd>
                </div>
                <div>
                  <dt>保障</dt>
                  <dd>7天无理由退货 · 正品保障 · 极速退款</dd>
                </div>
              </dl>
            </div>
          </section>
        </template>

        <section v-if="goods" class="detail-extra">
          <h2>商品详情</h2>
          <p v-if="goods.detailContent" class="detail-content">{{ goods.detailContent }}</p>
          <p v-else class="detail-content detail-content--empty">商家暂未填写商品详情。</p>

          <div v-if="detailMainImages.length" class="detail-gallery-block">
            <h3>商品主图</h3>
            <div class="detail-image-list">
              <figure v-for="item in detailMainImages" :key="item.key" class="detail-image-item">
                <img
                  :src="resolveFileUrl(item.url)"
                  :alt="item.label"
                  class="detail-image"
                  referrerpolicy="no-referrer"
                />
                <figcaption v-if="detailMainImages.length > 1" class="detail-image-caption">{{ item.label }}</figcaption>
              </figure>
            </div>
          </div>

          <div v-if="detailImagesLoading || detailSkuImages.length" class="detail-gallery-block">
            <h3>规格图片</h3>
            <p v-if="detailImagesLoading" class="detail-gallery-loading">规格图片加载中...</p>
            <div v-else class="detail-image-list">
              <figure v-for="item in detailSkuImages" :key="item.key" class="detail-image-item">
                <img
                  :src="resolveFileUrl(item.url)"
                  :alt="item.label"
                  class="detail-image"
                  referrerpolicy="no-referrer"
                />
                <figcaption class="detail-image-caption">{{ item.label }}</figcaption>
              </figure>
            </div>
          </div>
        </section>
      </div>
    </main>

    <Teleport to="body">
      <div v-if="showOrderModal" class="order-modal-mask" @click.self="closeOrderModal">
        <div class="order-modal">
          <div class="order-modal-header">
            <h2>确认订单</h2>
            <button type="button" :disabled="orderSubmitting" @click="closeOrderModal">×</button>
          </div>

          <div class="order-modal-body">
            <p v-if="orderErrorMsg" class="order-error">{{ orderErrorMsg }}</p>

            <div class="order-checkout">
              <div class="order-checkout-main">
                <section class="order-address-section order-panel">
                  <div class="order-section-head">
                    <h3>确认收货地址</h3>
                    <div class="order-address-links">
                      <router-link :to="{ name: 'AccountAddress' }" @click="closeOrderModal">使用新地址</router-link>
                      <router-link :to="{ name: 'AccountAddress' }" @click="closeOrderModal">地址管理</router-link>
                    </div>
                  </div>
                  <div v-if="addressLoading" class="order-address-state">加载地址中...</div>
                  <div v-else-if="!addressList.length" class="order-address-state">
                    暂无收货地址，请先添加地址
                  </div>
                  <div v-else class="order-address-list">
                    <button
                      v-for="address in addressList"
                      :key="address.id"
                      type="button"
                      class="order-address-item"
                      :class="{ active: selectedAddressId === address.id }"
                      @click="selectedAddressId = address.id"
                    >
                      <strong>{{ address.receiver }} {{ maskAddressMobile(address.mobile) }}</strong>
                      <span>{{ formatFullAddress(address) }}</span>
                    </button>
                  </div>
                </section>

                <section class="order-goods-panel order-panel">
                  <div class="order-section-head order-section-head--table">
                    <h3>确认订单信息</h3>
                    <div class="order-table-labels">
                      <span>商品属性</span>
                      <span>数量</span>
                      <span>价格</span>
                    </div>
                  </div>

                  <div class="order-shop-row">
                    <span class="order-shop-badge">店铺</span>
                    <strong>{{ goods?.shopName || '秒杀商城自营' }}</strong>
                  </div>

                  <div class="order-goods-row">
                    <div class="order-goods-base">
                      <img
                        v-if="currentImage"
                        :src="resolveFileUrl(currentImage)"
                        :alt="goods?.goodsName"
                        class="order-summary-img"
                        referrerpolicy="no-referrer"
                      />
                      <div v-else class="order-summary-img order-summary-img--empty">图</div>
                      <div>
                        <h4>{{ goods?.goodsName || '商品信息' }}</h4>
                        <p>如实描述</p>
                      </div>
                    </div>
                    <div class="order-goods-props">
                      <span>{{ selectedSku?.skuSpecName || '默认规格' }}</span>
                      <span>单价 ¥{{ formatGoodsPrice(orderUnitPrice) }}</span>
                    </div>
                    <div class="order-goods-qty">
                      <button type="button" :disabled="quantity <= 1 || orderSubmitting" @click="changeQuantity(-1)">-</button>
                      <strong>{{ quantity }}</strong>
                      <button type="button" :disabled="orderSubmitting" @click="changeQuantity(1)">+</button>
                    </div>
                    <div class="order-goods-price">
                      <strong>¥{{ formatGoodsPrice(orderTotalAmount) }}</strong>
                      <del v-if="selectedSku?.marketPrice">¥{{ formatGoodsPrice(selectedSku.marketPrice * quantity) }}</del>
                    </div>
                  </div>
                </section>
              </div>

              <aside class="order-pay-panel">
                <section class="pay-detail-card">
                  <h3>付款详情 <span>共 1 件商品</span></h3>
                  <div class="pay-line pay-line--total">
                    <span>商品总价</span>
                    <strong>¥{{ formatGoodsPrice(orderTotalAmount) }}</strong>
                  </div>
                  <div class="pay-line">
                    <span>店铺优惠</span>
                    <strong>- ¥0.00</strong>
                  </div>
                  <div class="pay-line">
                    <span>运费</span>
                    <strong>¥0.00</strong>
                  </div>
                  <div class="pay-summary">
                    <span>合计</span>
                    <strong>¥{{ formatGoodsPrice(orderTotalAmount) }}</strong>
                  </div>
                </section>

                <section class="pay-method-card">
                  <h3>支付方式</h3>
                  <button
                    type="button"
                    class="pay-method-item"
                    :class="{ active: selectedPaymentMethod === 'alipay' }"
                    :disabled="orderSubmitting"
                    @click="selectedPaymentMethod = 'alipay'"
                  >
                    <span class="pay-method-check" />
                    <span class="pay-method-icon pay-method-icon--alipay">支</span>
                    <span>支付宝支付</span>
                  </button>
                  <button
                    type="button"
                    class="pay-method-item"
                    :class="{ active: selectedPaymentMethod === 'balance' }"
                    :disabled="orderSubmitting"
                    @click="selectedPaymentMethod = 'balance'"
                  >
                    <span class="pay-method-check" />
                    <span class="pay-method-icon pay-method-icon--balance">余</span>
                    <span>余额支付</span>
                    <em v-if="balanceLoading">加载中...</em>
                    <em v-else>可用 ¥{{ balanceText }}</em>
                  </button>
                  <p
                    v-if="selectedPaymentMethod === 'balance' && balanceInsufficient"
                    class="pay-balance-warn"
                  >
                    余额不足，请先
                    <router-link to="/account/balance" @click="closeOrderModal">充值</router-link>
                    或选择支付宝支付
                  </p>
                </section>

                <button
                  type="button"
                  class="order-submit-btn"
                  :disabled="orderSubmitting || !addressList.length || balancePayBlocked"
                  @click="submitOrder"
                >
                  {{ orderSubmitText }}
                </button>
              </aside>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <div v-if="collectToastMsg || cartToastMsg || seckillToastMsg" class="collect-toast">
      {{ collectToastMsg || cartToastMsg || seckillToastMsg }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import {
  doSeckill,
  getActivityDetail,
  getSeckillGoodsDetail,
  getSeckillResult,
} from '@/api/activity'
import { getGoodsDetail, getSkuImage } from '@/api/goods'
import { useAddToCart } from '@/composables/useAddToCart'
import { useGoodsCollect } from '@/composables/useGoodsCollect'
import { addOrder, getOrderById } from '@/api/order'
import { listAddress } from '@/api/address'
import { useUserBalance } from '@/composables/useUserBalance'
import { useOrderPay } from '@/composables/useOrderPay'
import { formatFullAddress, maskAddressMobile, type UserAddressVO } from '@/types/address'
import {
  canPurchaseSeckillGoods,
  formatSeckillPrice,
  getSeckillPurchaseButtonText,
  type SeckillActivityVO,
  type SeckillGoodsApplyVO,
} from '@/types/activity'
import { formatGoodsPrice, type GoodsDetailVO, type GoodsSkuVO } from '@/types/goods'
import { isLoggedIn as checkLoggedIn } from '@/utils/auth'
import { resolveFileUrl } from '@/utils/file'
import favoriteIcon from '@/assets/shoucang.png'
import collectedFavoriteIcon from '@/assets/yishoucang.png'

const route = useRoute()
const router = useRouter()
const goods = ref<GoodsDetailVO | null>(null)
const loading = ref(false)
const errorMsg = ref('')
const {
  collected,
  collectCount,
  loading: collectLoading,
  toastMsg: collectToastMsg,
  loadCollectStatus,
  toggleCollect,
} = useGoodsCollect()
const { loading: cartAdding, toastMsg: cartToastMsg, addToCart, showToast: showCartToast } = useAddToCart()
const activeImageIndex = ref(0)
const selectedSkuKey = ref('')
const quantity = ref(1)
const zoomActive = ref(false)
const zoomPosition = ref({ x: 0.5, y: 0.5 })
const showOrderModal = ref(false)
const addressLoading = ref(false)
const orderSubmitting = ref(false)
const orderErrorMsg = ref('')
const addressList = ref<UserAddressVO[]>([])
const selectedAddressId = ref('')
const selectedPaymentMethod = ref<'alipay' | 'balance'>('alipay')
const { balanceText, balanceInfo, loading: balanceLoading, loadBalance } = useUserBalance()
const { payWithBalance } = useOrderPay()
const skuImageCache = ref<Record<string, string>>({})
const skuImageLoading = ref(false)
const detailImagesLoading = ref(false)
const showingSkuPreview = ref(false)
let carouselTimer: number | null = null

interface DetailGalleryImage {
  key: string
  url: string
  label: string
}

const goodsId = computed(() => String(route.params.id || ''))
const seckillGoodsId = computed(() => String(route.query.seckillGoodsId ?? '').trim())
const seckillContext = ref<SeckillGoodsApplyVO | null>(null)
const seckillActivity = ref<SeckillActivityVO | null>(null)
const seckillSubmitting = ref(false)
const seckillToastMsg = ref('')
let seckillToastTimer: ReturnType<typeof setTimeout> | null = null

const isSeckillMode = computed(() => !!seckillGoodsId.value)
const isSeckillPurchaseChannel = computed(
  () => isSeckillMode.value && canPurchaseSeckillGoods(seckillActivity.value),
)
const isSeckillItemOutOfStock = computed(
  () =>
    seckillContext.value?.seckillStock != null && (seckillContext.value.seckillStock ?? 0) <= 0,
)
const canSubmitSeckill = computed(() => {
  if (!seckillContext.value || !seckillGoodsId.value) return false
  if (!canPurchaseSeckillGoods(seckillActivity.value)) return false
  if (seckillContext.value.seckillPurchasable === false) return false
  if (isSeckillItemOutOfStock.value) return false
  return true
})
const seckillBuyButtonText = computed(() =>
  getSeckillPurchaseButtonText(seckillActivity.value, {
    outOfStock: isSeckillItemOutOfStock.value,
    purchasable: seckillContext.value?.seckillPurchasable,
  }),
)
const baseImageList = computed(() => goods.value?.goodsImgList?.filter(Boolean) ?? [])
const selectedSkuImage = computed(() => {
  const skuId = selectedSku.value?.id
  if (!skuId) return ''
  return skuImageCache.value[skuId] ?? selectedSku.value?.skuImg ?? ''
})
const currentImage = computed(() => {
  if (showingSkuPreview.value && selectedSkuImage.value) return selectedSkuImage.value
  if (baseImageList.value.length) return baseImageList.value[activeImageIndex.value] ?? ''
  return goods.value?.goodsImg ?? ''
})
const skuList = computed(() => goods.value?.skuList ?? [])
const selectedSku = computed(() =>
  skuList.value.find((sku, index) => getSkuKey(sku, index) === selectedSkuKey.value),
)
const displayPrice = computed(() => selectedSku.value?.salePrice ?? goods.value?.goodsPrice)
const orderUnitPrice = computed(() => displayPrice.value ?? 0)
const orderTotalAmount = computed(() => orderUnitPrice.value * quantity.value)
const balanceInsufficient = computed(() => {
  const balance = balanceInfo.value?.balance
  if (balance == null || !Number.isFinite(balance)) return false
  return balance < orderTotalAmount.value
})
const balancePayBlocked = computed(
  () => selectedPaymentMethod.value === 'balance' && balanceInsufficient.value,
)
const orderSubmitText = computed(() => {
  if (orderSubmitting.value) {
    return selectedPaymentMethod.value === 'balance' ? '支付中...' : '提交中...'
  }
  const amount = formatGoodsPrice(orderTotalAmount.value)
  return selectedPaymentMethod.value === 'balance'
    ? `余额支付 ¥${amount}`
    : `提交订单 ¥${amount}`
})
const totalStock = computed(() => skuList.value.reduce((sum, sku) => sum + (sku.stockNum ?? 0), 0))
const favoriteButtonIcon = computed(() => (collected.value ? collectedFavoriteIcon : favoriteIcon))
const zoomLensStyle = computed(() => ({
  left: `${zoomPosition.value.x * 100}%`,
  top: `${zoomPosition.value.y * 100}%`,
}))
const zoomPreviewStyle = computed(() => ({
  backgroundImage: currentImage.value ? `url("${resolveFileUrl(currentImage.value)}")` : '',
  backgroundPosition: `${zoomPosition.value.x * 100}% ${zoomPosition.value.y * 100}%`,
}))
const detailMainImages = computed<DetailGalleryImage[]>(() => {
  const images = baseImageList.value.length
    ? baseImageList.value
    : goods.value?.goodsImg
      ? [goods.value.goodsImg]
      : []
  return images.map((url, index) => ({
    key: `main-${index}-${url}`,
    url,
    label: images.length > 1 ? `主图 ${index + 1}` : '商品主图',
  }))
})
const mainImageUrlSet = computed(() => new Set(detailMainImages.value.map((item) => item.url)))
const detailSkuImages = computed<DetailGalleryImage[]>(() => {
  const result: DetailGalleryImage[] = []
  const seen = new Set<string>()
  skuList.value.forEach((sku, index) => {
    const url = sku.id ? skuImageCache.value[sku.id] ?? sku.skuImg : sku.skuImg
    if (!url || seen.has(url) || mainImageUrlSet.value.has(url)) return
    seen.add(url)
    result.push({
      key: `sku-${sku.id ?? index}-${url}`,
      url,
      label: sku.skuSpecName || `规格 ${index + 1}`,
    })
  })
  return result
})

function getSkuKey(sku: GoodsSkuVO, index: number) {
  return sku.id || `${sku.skuSpecName || 'default'}-${index}`
}

function getSkuThumb(sku: GoodsSkuVO) {
  if (sku.id && skuImageCache.value[sku.id]) return skuImageCache.value[sku.id]
  return sku.skuImg ?? ''
}

function activateSkuImage(skuImg: string) {
  if (!skuImg) return
  stopCarousel()
  showingSkuPreview.value = true
}

function selectMainImage(index: number) {
  showingSkuPreview.value = false
  activeImageIndex.value = index
  stopCarousel()
}

async function fetchSkuImageUrl(sku: GoodsSkuVO): Promise<string> {
  if (!sku.id) return sku.skuImg ?? ''
  const cached = skuImageCache.value[sku.id]
  if (cached) return cached
  if (sku.skuImg) {
    skuImageCache.value = { ...skuImageCache.value, [sku.id]: sku.skuImg }
    return sku.skuImg
  }
  try {
    const result = await getSkuImage(sku.id)
    if (result.skuImg) {
      skuImageCache.value = { ...skuImageCache.value, [sku.id]: result.skuImg }
      return result.skuImg
    }
  } catch {
    // 规格无独立图片时保持商品主图展示
  }
  return ''
}

async function loadSkuImage(sku: GoodsSkuVO) {
  skuImageLoading.value = true
  try {
    const skuImg = await fetchSkuImageUrl(sku)
    if (skuImg) activateSkuImage(skuImg)
  } finally {
    skuImageLoading.value = false
  }
}

async function preloadAllSkuImages() {
  const skus = goods.value?.skuList ?? []
  if (!skus.length) return
  detailImagesLoading.value = true
  try {
    await Promise.all(skus.map((sku) => fetchSkuImageUrl(sku)))
  } finally {
    detailImagesLoading.value = false
  }
}

async function selectSku(sku: GoodsSkuVO, index: number) {
  selectedSkuKey.value = getSkuKey(sku, index)
  await loadSkuImage(sku)
}

function stopCarousel() {
  if (!carouselTimer) return
  window.clearInterval(carouselTimer)
  carouselTimer = null
}

function startCarousel() {
  stopCarousel()
  if (showingSkuPreview.value || baseImageList.value.length <= 1) return
  carouselTimer = window.setInterval(showNextImage, 4000)
}

function showPrevImage() {
  if (baseImageList.value.length <= 1) return
  showingSkuPreview.value = false
  activeImageIndex.value =
    (activeImageIndex.value - 1 + baseImageList.value.length) % baseImageList.value.length
}

function showNextImage() {
  if (baseImageList.value.length <= 1) return
  showingSkuPreview.value = false
  activeImageIndex.value = (activeImageIndex.value + 1) % baseImageList.value.length
}

function changeQuantity(delta: number) {
  quantity.value = Math.max(1, quantity.value + delta)
}

function showZoom() {
  if (currentImage.value) zoomActive.value = true
}

function hideZoom() {
  zoomActive.value = false
}

function handleCarouselEnter() {
  stopCarousel()
  showZoom()
}

function handleCarouselLeave() {
  hideZoom()
  startCarousel()
}

function handleZoomMove(event: MouseEvent) {
  const rect = (event.currentTarget as HTMLElement).getBoundingClientRect()
  const x = Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  const y = Math.min(1, Math.max(0, (event.clientY - rect.top) / rect.height))
  zoomPosition.value = { x, y }
}

function handleToggleCollect() {
  void toggleCollect(goodsId.value)
}

async function handleAddToCart() {
  const skuId = selectedSku.value?.id
  if (!goods.value?.id) return
  if (!skuId) {
    showCartToast(
      skuList.value.length ? '请选择商品规格' : '该商品暂无可用规格，无法加入购物车',
    )
    return
  }
  await addToCart({
    goodsId: goods.value.id,
    skuId,
    goodsCount: quantity.value,
  })
}

function applyRouteCartQuery() {
  const skuId =
    (typeof route.query.skuId === 'string' && route.query.skuId)
      || seckillContext.value?.skuId
      || ''
  const quantityQuery = route.query.quantity
  const buyQuery = route.query.buy
  if (skuId && goods.value?.skuList?.length) {
    const index = goods.value.skuList.findIndex((sku) => sku.id === skuId)
    if (index >= 0) {
      const sku = goods.value.skuList[index]
      selectedSkuKey.value = getSkuKey(sku, index)
      void loadSkuImage(sku)
    }
  }
  if (typeof quantityQuery === 'string') {
    const parsed = Number.parseInt(quantityQuery, 10)
    if (Number.isFinite(parsed) && parsed > 0) {
      quantity.value = Math.min(parsed, 999)
    }
  }
  if (buyQuery === '1' && checkLoggedIn() && !isSeckillPurchaseChannel.value) {
    void openOrderModal()
  }
  if (route.query.seckillBuy === '1' && checkLoggedIn() && canSubmitSeckill.value) {
    void handleSeckillBuy()
  }
}

watch(
  () => route.query.seckillGoodsId,
  () => {
    if (!goodsId.value || loading.value) return
    void loadSeckillContext()
      .then(() => applyRouteCartQuery())
      .catch(() => {
        seckillContext.value = null
        seckillActivity.value = null
      })
  },
)

function showSeckillToast(message: string) {
  seckillToastMsg.value = message
  if (seckillToastTimer) clearTimeout(seckillToastTimer)
  seckillToastTimer = setTimeout(() => {
    seckillToastMsg.value = ''
    seckillToastTimer = null
  }, 2200)
}

async function loadSeckillContext() {
  seckillContext.value = null
  seckillActivity.value = null
  if (!seckillGoodsId.value) return

  const detail = await getSeckillGoodsDetail(seckillGoodsId.value)
  if (detail.goodsId !== goodsId.value) {
    throw new Error('秒杀商品与当前商品不匹配')
  }
  seckillContext.value = detail
  seckillActivity.value = await getActivityDetail(detail.activityId)
}

async function handleSeckillBuy() {
  if (!checkLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!canSubmitSeckill.value || !seckillGoodsId.value) {
    showSeckillToast(seckillBuyButtonText.value)
    return
  }

  seckillSubmitting.value = true
  try {
    let result = await doSeckill({ seckillGoodsId: seckillGoodsId.value })
    showSeckillToast(result.message || '秒杀请求已提交，订单创建中')

    if (result.orderNo && result.status === -1) {
      const orderNo = result.orderNo
      for (let attempt = 0; attempt < 20 && result.status === -1; attempt += 1) {
        await new Promise((resolve) => window.setTimeout(resolve, 500))
        result = await getSeckillResult(orderNo)
        showSeckillToast(result.message || '订单创建中，请稍候...')
      }
    }

    if (result.status === -2) {
      throw new Error(result.message || '订单创建失败，请重新参与秒杀')
    }

    if (result.orderNo && result.status === 0) {
      router.push({
        path: '/orders',
        query: { orderNo: result.orderNo, pay: '1' },
      })
    } else if (result.status === -1) {
      showSeckillToast('订单仍在创建中，请稍后到订单列表查看')
    } else if (result.status == null) {
      showSeckillToast(result.message || '订单结果已过期，请到订单列表确认')
    }
  } catch (err) {
    showSeckillToast(err instanceof Error ? err.message : '秒杀失败，请稍后重试')
  } finally {
    seckillSubmitting.value = false
  }
}

async function loadDetail() {
  if (!goodsId.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    const [detail] = await Promise.all([
      getGoodsDetail(goodsId.value),
      loadSeckillContext().catch(() => {
        seckillContext.value = null
        seckillActivity.value = null
      }),
    ])
    goods.value = detail
    skuImageCache.value = {}
    showingSkuPreview.value = false
    activeImageIndex.value = 0
    const firstSku = goods.value.skuList?.[0]
    selectedSkuKey.value = firstSku ? getSkuKey(firstSku, 0) : ''
    quantity.value = 1
    if (firstSku) {
      await loadSkuImage(firstSku)
    } else {
      startCarousel()
    }
    await Promise.all([loadCollectStatus(goodsId.value), preloadAllSkuImages()])
    applyRouteCartQuery()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载商品详情失败'
    goods.value = null
    stopCarousel()
  } finally {
    loading.value = false
  }
}

async function loadAddressList() {
  addressLoading.value = true
  orderErrorMsg.value = ''
  try {
    addressList.value = await listAddress()
    selectedAddressId.value = addressList.value[0]?.id ?? ''
  } catch (err) {
    orderErrorMsg.value = err instanceof Error ? err.message : '加载收货地址失败'
    addressList.value = []
    selectedAddressId.value = ''
  } finally {
    addressLoading.value = false
  }
}

async function openOrderModal() {
  if (!checkLoggedIn()) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (!goods.value) return
  selectedPaymentMethod.value = 'alipay'
  showOrderModal.value = true
  await Promise.allSettled([loadAddressList(), loadBalance()])
}

function closeOrderModal() {
  if (orderSubmitting.value) return
  showOrderModal.value = false
  orderErrorMsg.value = ''
}

async function submitOrder() {
  if (!goods.value) return
  if (!selectedAddressId.value) {
    orderErrorMsg.value = '请选择收货地址'
    return
  }
  if (balancePayBlocked.value) {
    orderErrorMsg.value = '账户余额不足，请先充值或选择支付宝支付'
    return
  }
  orderSubmitting.value = true
  orderErrorMsg.value = ''
  try {
    const orderId = await addOrder({
      goodsId: goods.value.id,
      skuId: selectedSku.value?.id,
      userAddressId: selectedAddressId.value,
      buyGoodsNum: quantity.value,
    })
    if (selectedPaymentMethod.value === 'balance') {
      const order = await getOrderById(orderId)
      if (!order.orderNo) {
        throw new Error('订单号获取失败')
      }
      await payWithBalance(order)
      showOrderModal.value = false
      router.push({
        path: '/pay/result',
        query: { orderNo: order.orderNo, method: 'balance' },
      })
      return
    }
    showOrderModal.value = false
    router.push({
      path: '/orders',
      query: orderId ? { orderId, pay: '1' } : undefined,
    })
  } catch (err) {
    orderErrorMsg.value = err instanceof Error ? err.message : '提交订单失败'
  } finally {
    orderSubmitting.value = false
  }
}

onMounted(loadDetail)

onUnmounted(() => {
  stopCarousel()
  if (seckillToastTimer) clearTimeout(seckillToastTimer)
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff;
}

.detail-main {
  padding: 12px 0 56px;
}

.detail-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

.detail-breadcrumb {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  color: #999;
  font-size: 12px;
}

.detail-breadcrumb a:hover {
  color: var(--accent);
}

.detail-card {
  display: grid;
  grid-template-columns: 520px minmax(0, 1fr);
  gap: 100px;
  align-items: start;
  padding: 12px 0 24px;
  background: #fff;
}

.detail-gallery {
  display: grid;
  grid-template-columns: 58px 1fr;
  gap: 14px;
  min-width: 0;
}

.thumb-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
  max-height: 460px;
  overflow-y: auto;
  padding-right: 2px;
}

.thumb-btn {
  width: 56px;
  height: 56px;
  padding: 2px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s, box-shadow 0.18s, transform 0.18s;
}

.thumb-btn.active {
  border-color: var(--accent);
  box-shadow: 0 0 0 1px rgba(255, 80, 0, 0.12);
}

.thumb-btn:hover {
  border-color: var(--accent);
  transform: translateY(-1px);
}

.thumb-img {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  object-fit: cover;
}

.thumb-btn--empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
}

.gallery-main {
  min-width: 0;
}

.main-image-wrap {
  position: relative;
  height: 460px;
  border-radius: 14px;
  overflow: visible;
  background: #f7f7f7;
  cursor: zoom-in;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
  border-radius: 14px;
}

.main-image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  border-radius: 12px;
}

.zoom-lens {
  position: absolute;
  z-index: 2;
  width: 180px;
  height: 180px;
  border: 1px solid rgba(255, 80, 0, 0.5);
  border-radius: 6px;
  background: rgba(255, 247, 240, 0.44);
  pointer-events: none;
  transform: translate(-50%, -50%);
}

.zoom-preview {
  position: absolute;
  left: calc(100% + 18px);
  top: 0;
  z-index: 30;
  width: 420px;
  height: 420px;
  border: 2px solid #fff;
  border-radius: 12px;
  background-color: #fff;
  background-repeat: no-repeat;
  background-size: 220%;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.16);
  pointer-events: none;
}

.image-arrow {
  position: absolute;
  top: 50%;
  z-index: 4;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.86);
  color: #666;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transform: translateY(-50%);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
  transition: opacity 0.18s, color 0.18s, background 0.18s;
}

.main-image-wrap:hover .image-arrow {
  opacity: 1;
}

.image-arrow:hover {
  background: #fff;
  color: var(--accent);
}

.image-arrow--prev {
  left: 14px;
}

.image-arrow--next {
  right: 14px;
}

.image-indicator {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 4;
  min-width: 48px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.46);
  color: #fff;
  font-size: 12px;
  line-height: 24px;
  text-align: center;
}

.gallery-loading {
  position: absolute;
  left: 50%;
  bottom: 14px;
  z-index: 4;
  transform: translateX(-50%);
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.46);
  color: #fff;
  font-size: 12px;
  line-height: 1.4;
}

.detail-info {
  min-width: 0;
  padding: 8px 0 0;
}

.goods-title {
  margin: 0;
  color: #222;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.45;
}

.stock-line {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
}

.info-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.info-tags span {
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f7fbff;
  color: #6d8aa2;
  font-size: 12px;
  line-height: 24px;
}

.buy-panel {
  margin-top: 18px;
  padding: 14px;
  border-radius: 14px;
  background: #fafafa;
}

.seckill-price-banner {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  margin-top: 18px;
  padding: 14px 18px;
  border-radius: 12px;
  background: linear-gradient(90deg, #ffe8d9 0%, #fff7f2 100%);
  border: 1px solid #ffd6bf;
}

.seckill-price-label {
  color: var(--accent);
  font-size: 13px;
  font-weight: 700;
}

.seckill-price-value {
  margin: 0;
  color: var(--accent);
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
}

.seckill-price-limit {
  color: #999;
  font-size: 12px;
}

.price-block {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 4px 10px;
  margin: 18px 0 0;
  padding: 16px 18px;
  border-radius: 12px;
  background: linear-gradient(90deg, #fff3e8 0%, #fff 100%);
}

.price-block--secondary {
  margin-top: 10px;
  background: #fafafa;
}

.price-block--secondary .price {
  font-size: 24px;
  color: #666;
}

.price-label {
  display: block;
  color: #999;
  font-size: 12px;
}

.price {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--accent);
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
}

.price-symbol {
  margin-right: 2px;
  font-size: 16px;
}

.sold-count {
  color: #999;
  font-size: 12px;
}

.service-list {
  display: grid;
  gap: 12px;
  margin: 18px 0 0;
  padding-top: 18px;
  border-top: 1px solid #f4f4f4;
}

.service-list div {
  display: flex;
  gap: 12px;
  color: #666;
  font-size: 13px;
}

.service-list dt {
  width: 42px;
  color: #999;
}

.service-list dd {
  margin: 0;
}

.section-head {
  margin-bottom: 10px;
  padding-top: 4px;
}

.section-head h2 {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.sku-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sku-item {
  display: inline-grid;
  grid-template-columns: 34px minmax(70px, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 44px;
  max-width: 210px;
  padding: 6px 10px;
  border: 1px solid #eeeeee;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
}

.sku-item.active {
  border-color: var(--accent);
  background: #fff7f0;
}

.sku-img {
  width: 34px;
  height: 34px;
  border-radius: 4px;
  object-fit: cover;
}

.sku-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #333;
  font-size: 13px;
}

.sku-price {
  grid-column: 1 / -1;
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}

.sku-empty {
  margin: 0;
  color: #999;
  font-size: 13px;
}

.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  color: #666;
  font-size: 13px;
}

.quantity-stepper {
  display: inline-flex;
  align-items: center;
  height: 32px;
  border: 1px solid #ededed;
  border-radius: 999px;
  overflow: hidden;
}

.quantity-stepper button {
  width: 34px;
  height: 32px;
  border: none;
  background: #fafafa;
  color: #666;
  cursor: pointer;
}

.quantity-stepper strong {
  min-width: 36px;
  text-align: center;
  font-size: 13px;
}

.detail-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 92px;
  gap: 8px;
  margin-top: 18px;
}

.detail-actions button {
  height: 44px;
  border: none;
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.cart-btn {
  background: linear-gradient(90deg, #ffd51f 0%, #ff9f00 100%);
}

.seckill-buy-btn {
  min-width: 180px;
}

.seckill-buy-tip {
  margin: 10px 0 0;
  color: #999;
  font-size: 12px;
}

.buy-btn {
  background: linear-gradient(90deg, #ff7a00 0%, #ff5000 100%);
}

.fav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 92px;
  background: linear-gradient(180deg, #ffffff 0%, #fff7f2 100%);
  color: #ff5000 !important;
  border: 1px solid rgba(255, 80, 0, 0.28) !important;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.08);
  transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s, background 0.18s;
}

.fav-btn:hover {
  border-color: rgba(255, 80, 0, 0.55) !important;
  background: linear-gradient(180deg, #fffaf7 0%, #fff1e8 100%);
  box-shadow: 0 6px 16px rgba(255, 80, 0, 0.16);
  transform: translateY(-1px);
}

.fav-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
  transform: none;
}

.fav-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.collect-count {
  margin: 10px 0 0;
  text-align: right;
  color: #999;
  font-size: 12px;
}

.collect-toast {
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

.detail-extra {
  margin-top: 16px;
  padding: 22px 26px;
  border-radius: 12px;
  background: #fff;
}

.detail-extra h2 {
  margin: 0 0 12px;
  font-size: 18px;
}

.detail-content {
  margin: 0;
  color: #555;
  line-height: 1.9;
  white-space: pre-wrap;
}

.detail-content--empty {
  color: #999;
}

.detail-gallery-block {
  width: 50%;
  max-width: 50%;
  margin: 28px auto 0;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.detail-gallery-block h3 {
  margin: 0 0 14px;
  color: #333;
  font-size: 15px;
  font-weight: 600;
}

.detail-gallery-loading {
  margin: 0;
  padding: 24px 0;
  color: #999;
  font-size: 13px;
  text-align: center;
}

.detail-image-list {
  display: grid;
  gap: 12px;
}

.detail-image-item {
  margin: 0;
  overflow: hidden;
  border-radius: 10px;
  background: #fafafa;
}

.detail-image {
  display: block;
  width: 100%;
  height: auto;
}

.detail-image-caption {
  margin: 0;
  padding: 8px 12px;
  color: #888;
  font-size: 12px;
  text-align: center;
}

.detail-state {
  padding: 80px 0;
  border-radius: 12px;
  background: #fff;
  text-align: center;
  color: #999;
}

.detail-state--error {
  color: #f5222d;
}

.detail-retry {
  border: none;
  background: none;
  color: var(--accent);
  cursor: pointer;
}

.order-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 12px;
  background: rgba(0, 0, 0, 0.42);
}

.order-modal {
  width: min(1180px, 100%);
  max-height: calc(100vh - 48px);
  overflow: auto;
  border-radius: 18px;
  background: #f6f6f6;
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.18);
}

.order-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid #eeeeee;
}

.order-modal-header h2 {
  margin: 0;
  font-size: 18px;
}

.order-modal-header button {
  border: none;
  background: none;
  color: #999;
  font-size: 24px;
  cursor: pointer;
}

.order-modal-body {
  padding: 16px;
}

.order-error {
  margin: 0 0 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff2f0;
  color: #f5222d;
  font-size: 13px;
}

.order-checkout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 306px;
  gap: 16px;
}

.order-checkout-main {
  display: grid;
  gap: 14px;
}

.order-panel,
.order-pay-panel > section {
  border-radius: 14px;
  background: #fff;
}

.order-panel {
  padding: 18px 20px;
}

.order-summary-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  background: #f5f5f5;
}

.order-summary-img--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 20px;
  font-weight: 700;
}

.order-address-section {
  min-height: 130px;
}

.order-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.order-section-head h3 {
  margin: 0;
  color: #222;
  font-size: 16px;
}

.order-address-links {
  display: flex;
  gap: 18px;
}

.order-address-links a {
  color: var(--accent);
  font-size: 13px;
  text-decoration: none;
}

.order-section-head--table {
  align-items: flex-end;
}

.order-table-labels {
  display: grid;
  grid-template-columns: 160px 90px 110px;
  gap: 18px;
  color: #777;
  font-size: 13px;
}

.order-address-state {
  padding: 26px 0;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.order-address-list {
  display: grid;
  gap: 8px;
}

.order-address-item {
  display: grid;
  gap: 5px;
  padding: 12px 14px;
  border: 1px solid #eeeeee;
  border-radius: 12px;
  background: #fff;
  color: #555;
  text-align: left;
  cursor: pointer;
}

.order-address-item.active {
  border-color: var(--accent);
  background: #fff8f2;
  box-shadow: inset 0 0 0 1px rgba(255, 80, 0, 0.12);
}

.order-address-item strong {
  color: #222;
}

.order-address-item span {
  color: #777;
  font-size: 13px;
}

.order-shop-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fafafa;
  color: #333;
  font-size: 14px;
}

.order-shop-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 20px;
  padding: 0 6px;
  border-radius: 4px;
  background: #ffefe6;
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}

.order-goods-row {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 160px 90px 110px;
  gap: 18px;
  align-items: center;
  padding: 14px 12px;
  border-radius: 12px;
  background: #fff;
}

.order-goods-base {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.order-goods-base h4 {
  display: -webkit-box;
  margin: 0 0 6px;
  overflow: hidden;
  color: #222;
  font-size: 14px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.order-goods-base p,
.order-goods-props span {
  margin: 0;
  color: #888;
  font-size: 12px;
}

.order-goods-props {
  display: grid;
  gap: 6px;
}

.order-goods-qty {
  display: inline-flex;
  align-items: center;
  justify-self: start;
  height: 32px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  overflow: hidden;
}

.order-goods-qty button {
  width: 30px;
  height: 32px;
  border: none;
  background: #fafafa;
  color: #666;
  cursor: pointer;
}

.order-goods-qty button:disabled {
  color: #ccc;
  cursor: not-allowed;
}

.order-goods-qty strong {
  min-width: 34px;
  text-align: center;
  color: #333;
  font-size: 13px;
}

.order-goods-price {
  display: grid;
  gap: 4px;
  justify-items: end;
}

.order-goods-price strong {
  color: var(--accent);
  font-size: 16px;
}

.order-goods-price del {
  color: #999;
  font-size: 12px;
}

.order-pay-panel {
  display: grid;
  align-content: start;
  gap: 14px;
}

.pay-detail-card,
.pay-method-card {
  padding: 18px 16px;
}

.pay-detail-card h3,
.pay-method-card h3 {
  display: flex;
  justify-content: space-between;
  margin: 0 0 16px;
  color: #222;
  font-size: 16px;
}

.pay-detail-card h3 span {
  color: #777;
  font-size: 13px;
  font-weight: 400;
}

.pay-line,
.pay-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  color: #666;
  font-size: 14px;
}

.pay-line strong {
  color: var(--accent);
}

.pay-line--total {
  margin-top: 0;
}

.pay-summary {
  align-items: baseline;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f0f0;
  color: #222;
  font-weight: 700;
}

.pay-summary strong {
  color: #222;
  font-size: 18px;
}

.pay-method-card {
  display: grid;
  gap: 10px;
}

.pay-method-item {
  display: grid;
  grid-template-columns: 18px 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-height: 40px;
  border: none;
  background: #fff;
  color: #333;
  text-align: left;
  cursor: pointer;
}

.pay-method-check {
  width: 14px;
  height: 14px;
  border: 1px solid #d8d8d8;
  border-radius: 4px;
  background: #f0f0f0;
}

.pay-method-item.active .pay-method-check {
  border-color: var(--accent);
  background: var(--accent);
  box-shadow: inset 0 0 0 3px #fff;
}

.pay-method-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.pay-method-icon--alipay {
  background: #1677ff;
}

.pay-method-icon--balance {
  background: #ff8a00;
}

.pay-method-item em {
  color: #999;
  font-size: 12px;
  font-style: normal;
}

.pay-balance-warn {
  margin: 8px 0 0;
  padding: 8px 10px;
  font-size: 12px;
  color: #cf1322;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  line-height: 1.6;
}

.pay-balance-warn a {
  color: var(--accent);
  text-decoration: underline;
}

.order-submit-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(90deg, #ff7a00 0%, #ff5000 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.order-submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 992px) {
  .detail-card {
    grid-template-columns: 1fr;
  }

  .zoom-preview {
    display: none;
  }

  .order-checkout {
    grid-template-columns: 1fr;
  }

  .order-goods-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .order-goods-price {
    justify-items: start;
  }

  .order-table-labels {
    display: none;
  }
}

@media (max-width: 640px) {
  .detail-gallery {
    grid-template-columns: 1fr;
  }

  .detail-gallery-block {
    width: 100%;
    max-width: 100%;
  }

  .thumb-list {
    order: 2;
    flex-direction: row;
    overflow-x: auto;
  }

  .detail-actions {
    grid-template-columns: 1fr;
  }

  .fav-btn {
    width: 100%;
  }
}
</style>
