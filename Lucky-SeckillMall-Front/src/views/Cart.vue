<template>
  <div class="page">
    <Header top-only />

    <main class="cart-main">
      <section class="cart-card">
        <div class="cart-head">
          <div>
            <h1>购物车</h1>
            <p v-if="summary.totalQuantity">共 {{ summary.totalQuantity }} 件商品</p>
            <p v-else>管理待购商品，选中后可去结算</p>
          </div>
          <button
            type="button"
            class="cart-refresh-btn"
            :disabled="loading"
            @click="loadCart"
          >
            {{ loading ? '加载中...' : '刷新' }}
          </button>
        </div>

        <p v-if="errorMsg" class="cart-error">{{ errorMsg }}</p>

        <div v-if="loading && !items.length" class="cart-state">加载购物车中...</div>
        <div v-else-if="!items.length" class="cart-state">
          <p>购物车还是空的</p>
          <router-link to="/" class="cart-go-home">去首页逛逛</router-link>
        </div>

        <template v-else>
          <div class="cart-toolbar">
            <label class="cart-check-all">
              <input
                type="checkbox"
                :checked="summary.allSelected"
                :disabled="actionLoading"
                @change="handleToggleSelectAll"
              />
              <span>全选</span>
            </label>
            <button
              type="button"
              class="cart-link-btn"
              :disabled="!summary.selectedLines || actionLoading"
              @click="handleDeleteSelected"
            >
              删除选中
            </button>
            <button
              type="button"
              class="cart-link-btn"
              :disabled="actionLoading"
              @click="handleClearCart"
            >
              清空购物车
            </button>
          </div>

          <section
            v-for="group in shopGroups"
            :key="group.shopId"
            class="cart-shop-group"
          >
            <header class="cart-shop-head">
              <router-link
                v-if="group.shopId && group.shopId !== 'default'"
                :to="{ name: 'ShopDetail', params: { id: group.shopId } }"
                class="cart-shop-name"
              >
                {{ group.shopName }}
              </router-link>
              <span v-else class="cart-shop-name">{{ group.shopName }}</span>
            </header>

            <ul class="cart-item-list">
              <li
                v-for="item in group.items"
                :key="item.id"
                class="cart-item"
                :class="{ 'cart-item--invalid': item.valid === false }"
              >
                <label class="cart-item-check">
                  <input
                    type="checkbox"
                    :checked="isCartItemSelected(item)"
                    :disabled="item.valid === false || actionLoading"
                    @change="handleToggleItem(item)"
                  />
                </label>

                <router-link
                  :to="{ name: 'GoodsDetail', params: { id: item.goodsId } }"
                  class="cart-item-thumb"
                >
                  <img
                    v-if="item.goodsImg"
                    :src="resolveFileUrl(item.goodsImg)"
                    :alt="item.goodsName"
                    class="cart-item-img"
                    referrerpolicy="no-referrer"
                  />
                  <span v-else class="cart-item-img cart-item-img--empty">
                    {{ (item.goodsName || '商').slice(0, 1) }}
                  </span>
                </router-link>

                <div class="cart-item-main">
                  <router-link
                    :to="{ name: 'GoodsDetail', params: { id: item.goodsId } }"
                    class="cart-item-title"
                  >
                    {{ item.goodsName || '商品' }}
                  </router-link>
                  <p v-if="item.skuSpecName" class="cart-item-spec">{{ item.skuSpecName }}</p>
                  <p v-if="item.valid === false && item.invalidReason" class="cart-item-invalid">
                    {{ item.invalidReason }}
                  </p>
                  <p v-else-if="item.stockNum != null && item.goodsCount > item.stockNum" class="cart-item-invalid">
                    库存不足，仅剩 {{ item.stockNum }} 件
                  </p>
                </div>

                <div class="cart-item-price">
                  <strong>¥{{ formatCartAmount(item.salePrice) }}</strong>
                </div>

                <div class="cart-item-qty">
                  <button
                    type="button"
                    :disabled="item.goodsCount <= 1 || actionLoading"
                    @click="changeItemQuantity(item, -1)"
                  >
                    -
                  </button>
                  <strong>{{ item.goodsCount }}</strong>
                  <button
                    type="button"
                    :disabled="item.goodsCount >= CART_MAX_GOODS_COUNT || actionLoading"
                    @click="changeItemQuantity(item, 1)"
                  >
                    +
                  </button>
                </div>

                <div class="cart-item-subtotal">
                  <span>小计</span>
                  <strong>¥{{ formatCartAmount(item.subtotal ?? (item.salePrice ?? 0) * item.goodsCount) }}</strong>
                </div>

                <button
                  type="button"
                  class="cart-item-delete"
                  :disabled="actionLoading"
                  @click="handleDeleteItem(item)"
                >
                  删除
                </button>
              </li>
            </ul>
          </section>
        </template>
      </section>

      <footer v-if="items.length" class="cart-footer">
        <label class="cart-footer-check">
          <input
            type="checkbox"
            :checked="summary.allSelected"
            :disabled="actionLoading"
            @change="handleToggleSelectAll"
          />
          <span>全选</span>
        </label>
        <p class="cart-footer-summary">
          已选 <strong>{{ summary.selectedQuantity }}</strong> 件，
          合计
          <em>¥{{ formatCartAmount(summary.selectedAmount) }}</em>
        </p>
        <button
          type="button"
          class="cart-settle-btn"
          :disabled="!canCheckout || actionLoading"
          @click="handleCheckout"
        >
          去结算{{ summary.selectedQuantity ? `(${summary.selectedQuantity})` : '' }}
        </button>
      </footer>
    </main>

    <div v-if="toastMsg" class="cart-toast">{{ toastMsg }}</div>

    <Teleport to="body">
      <div
        v-if="showClearConfirm"
        class="cart-confirm-mask"
        @click.self="closeClearConfirm"
      >
        <div class="cart-confirm-modal">
          <div class="cart-confirm-head">
            <h2>清空购物车</h2>
            <button
              type="button"
              class="cart-confirm-close"
              aria-label="关闭"
              :disabled="actionLoading"
              @click="closeClearConfirm"
            >
              ×
            </button>
          </div>

          <p class="cart-confirm-tip">确定要清空购物车吗？清空后所有商品将被移除，且无法恢复。</p>

          <div class="cart-confirm-preview">
            <p class="cart-confirm-line">
              <span>商品件数</span>
              <strong>{{ summary.totalQuantity }} 件</strong>
            </p>
            <p class="cart-confirm-line">
              <span>商品种类</span>
              <strong>{{ summary.totalLines }} 种</strong>
            </p>
          </div>

          <div class="cart-confirm-actions">
            <button
              type="button"
              class="cart-confirm-btn"
              :disabled="actionLoading"
              @click="closeClearConfirm"
            >
              取消
            </button>
            <button
              type="button"
              class="cart-confirm-btn cart-confirm-btn--danger"
              :disabled="actionLoading"
              @click="confirmClearCart"
            >
              {{ actionLoading ? '清空中...' : '确认清空' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import {
  batchDeleteCart,
  clearCart,
  deleteCart,
  getCartList,
  selectAllCart,
  selectCartItem,
  updateCartQuantity,
} from '@/api/cart'
import {
  CART_MAX_GOODS_COUNT,
  CartSelected,
  formatCartAmount,
  groupCartByShop,
  isCartItemSelected,
  type CartListVO,
  type CartSummaryVO,
  type CartVO,
} from '@/types/cart'
import { dispatchCartChanged } from '@/utils/cartEvent'
import { resolveFileUrl } from '@/utils/file'
import { isLoggedIn } from '@/utils/auth'

const router = useRouter()
const loading = ref(false)
const actionLoading = ref(false)
const showClearConfirm = ref(false)
const errorMsg = ref('')
const toastMsg = ref('')
const cartData = ref<CartListVO>({ items: [], summary: emptySummary() })

const items = computed(() => cartData.value.items)
const summary = computed(() => cartData.value.summary)
const shopGroups = computed(() => groupCartByShop(items.value))

const selectedValidItems = computed(() =>
  items.value.filter((item) => isCartItemSelected(item) && item.valid !== false),
)

const canCheckout = computed(() => {
  const selected = selectedValidItems.value
  return selected.length === 1
})

function emptySummary(): CartSummaryVO {
  return {
    totalLines: 0,
    totalQuantity: 0,
    selectedLines: 0,
    selectedQuantity: 0,
    selectedAmount: 0,
    allSelected: false,
  }
}

function showToast(message: string) {
  toastMsg.value = message
  window.setTimeout(() => {
    if (toastMsg.value === message) toastMsg.value = ''
  }, 2000)
}

function isAuthErrorMessage(message: string): boolean {
  return message.includes('未登录') || message.includes('请先登录')
}

async function loadCart() {
  if (!isLoggedIn()) {
    router.replace({ path: '/login', query: { redirect: '/cart' } })
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    cartData.value = await getCartList()
  } catch (err) {
    const message = err instanceof Error ? err.message : '加载购物车失败'
    errorMsg.value = message
    cartData.value = { items: [], summary: emptySummary() }
    if (isAuthErrorMessage(message)) {
      router.replace({ path: '/login', query: { redirect: '/cart' } })
    }
  } finally {
    loading.value = false
  }
}

async function runCartAction(action: () => Promise<void>, successMsg?: string) {
  actionLoading.value = true
  try {
    await action()
    await loadCart()
    dispatchCartChanged()
    if (successMsg) showToast(successMsg)
  } catch (err) {
    showToast(err instanceof Error ? err.message : '操作失败')
  } finally {
    actionLoading.value = false
  }
}

function handleToggleSelectAll(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  const selected = checked ? CartSelected.YES : CartSelected.NO
  void runCartAction(() => selectAllCart(selected))
}

function handleToggleItem(item: CartVO) {
  const next = isCartItemSelected(item) ? CartSelected.NO : CartSelected.YES
  void runCartAction(() => selectCartItem(item.id, next))
}

function changeItemQuantity(item: CartVO, delta: number) {
  const next = Math.min(CART_MAX_GOODS_COUNT, Math.max(1, item.goodsCount + delta))
  if (next === item.goodsCount) return
  void runCartAction(() => updateCartQuantity(item.id, next))
}

function handleDeleteItem(item: CartVO) {
  if (!window.confirm('确定删除该商品吗？')) return
  void runCartAction(() => deleteCart(item.id), '已删除')
}

function handleDeleteSelected() {
  const selectedIds = items.value.filter(isCartItemSelected).map((item) => item.id)
  if (!selectedIds.length) return
  if (!window.confirm('确定删除已选商品吗？')) return
  void runCartAction(() => batchDeleteCart(selectedIds), '已删除选中商品')
}

function handleClearCart() {
  showClearConfirm.value = true
}

function closeClearConfirm() {
  if (actionLoading.value) return
  showClearConfirm.value = false
}

async function confirmClearCart() {
  await runCartAction(() => clearCart(), '购物车已清空')
  showClearConfirm.value = false
}

function handleCheckout() {
  const selected = selectedValidItems.value
  if (!selected.length) {
    showToast('请先勾选要结算的商品')
    return
  }
  if (selected.length > 1) {
    showToast('暂不支持多商品合并结算，请只勾选一件')
    return
  }
  const item = selected[0]
  router.push({
    name: 'GoodsDetail',
    params: { id: item.goodsId },
    query: {
      skuId: item.skuId,
      quantity: String(item.goodsCount),
      buy: '1',
    },
  })
}

onMounted(() => {
  void loadCart()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.cart-main {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 24px 0 100px;
}

.cart-card {
  padding: 24px 28px 28px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.05);
}

.cart-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.cart-head h1 {
  margin: 0 0 6px;
  font-size: 22px;
  color: #222;
}

.cart-head p {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.cart-refresh-btn {
  padding: 8px 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.cart-refresh-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.cart-refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cart-error {
  margin: 0 0 16px;
  font-size: 13px;
  color: #cf1322;
}

.cart-state {
  padding: 48px 0;
  text-align: center;
  font-size: 14px;
  color: #999;
}

.cart-go-home {
  display: inline-block;
  margin-top: 12px;
  color: var(--accent);
}

.cart-toolbar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13px;
}

.cart-check-all,
.cart-footer-check {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #666;
}

.cart-link-btn {
  border: none;
  background: none;
  font-size: 13px;
  color: #666;
  cursor: pointer;
}

.cart-link-btn:hover:not(:disabled) {
  color: var(--accent);
}

.cart-link-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cart-shop-group {
  border-bottom: 1px solid #f5f5f5;
}

.cart-shop-head {
  padding: 14px 0 8px;
}

.cart-shop-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.cart-shop-name:hover {
  color: var(--accent);
}

.cart-item-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.cart-item {
  display: grid;
  grid-template-columns: 32px 80px 1fr 90px 110px 100px 48px;
  gap: 12px;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #f8f8f8;
}

.cart-item--invalid {
  opacity: 0.72;
}

.cart-item-check input {
  width: 16px;
  height: 16px;
  accent-color: var(--accent);
  cursor: pointer;
}

.cart-item-thumb {
  display: block;
}

.cart-item-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  background: #f5f5f5;
}

.cart-item-img--empty {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #bbb;
}

.cart-item-title {
  display: block;
  font-size: 14px;
  line-height: 1.5;
  color: #333;
}

.cart-item-title:hover {
  color: var(--accent);
}

.cart-item-spec {
  margin: 6px 0 0;
  font-size: 12px;
  color: #999;
}

.cart-item-invalid {
  margin: 6px 0 0;
  font-size: 12px;
  color: #cf1322;
}

.cart-item-price strong {
  font-size: 14px;
  color: var(--accent);
}

.cart-item-qty {
  display: inline-flex;
  align-items: center;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  overflow: hidden;
}

.cart-item-qty button {
  width: 28px;
  height: 28px;
  border: none;
  background: #fafafa;
  cursor: pointer;
  font-size: 14px;
}

.cart-item-qty button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cart-item-qty strong {
  min-width: 36px;
  text-align: center;
  font-size: 13px;
}

.cart-item-subtotal {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.cart-item-subtotal strong {
  display: block;
  margin-top: 4px;
  font-size: 14px;
  color: #333;
}

.cart-item-delete {
  border: none;
  background: none;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.cart-item-delete:hover:not(:disabled) {
  color: var(--accent);
}

.cart-footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 20px;
  height: 64px;
  padding: 0 max(12px, calc((100vw - var(--layout-width)) / 2 + 12px));
  background: #fff;
  border-top: 1px solid #eee;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.06);
}

.cart-footer-summary {
  flex: 1;
  margin: 0;
  font-size: 13px;
  color: #666;
  text-align: right;
}

.cart-footer-summary em {
  font-style: normal;
  font-size: 20px;
  font-weight: 700;
  color: var(--accent);
}

.cart-settle-btn {
  min-width: 120px;
  height: 44px;
  padding: 0 24px;
  border: none;
  border-radius: 22px;
  background: var(--accent);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
}

.cart-settle-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cart-toast {
  position: fixed;
  left: 50%;
  bottom: 88px;
  z-index: 40;
  transform: translateX(-50%);
  padding: 10px 20px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  font-size: 14px;
}

.cart-confirm-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
}

.cart-confirm-modal {
  width: 420px;
  max-width: 100%;
  padding: 20px 22px 22px;
  border-radius: 12px;
  background: #fff;
}

.cart-confirm-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.cart-confirm-head h2 {
  margin: 0;
  font-size: 18px;
  color: #222;
}

.cart-confirm-close {
  border: none;
  background: none;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
}

.cart-confirm-close:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cart-confirm-tip {
  margin: 0 0 14px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.cart-confirm-preview {
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.cart-confirm-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.5;
}

.cart-confirm-line:last-child {
  margin-bottom: 0;
}

.cart-confirm-line span {
  flex-shrink: 0;
  color: #999;
}

.cart-confirm-line strong {
  text-align: right;
  color: #333;
  font-weight: 500;
}

.cart-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.cart-confirm-btn {
  min-width: 88px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.cart-confirm-btn--danger {
  border-color: #ff4d4f;
  background: #ff4d4f;
  color: #fff;
}

.cart-confirm-btn--danger:disabled {
  opacity: 0.7;
}

.cart-confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .cart-item {
    grid-template-columns: 28px 72px 1fr;
    grid-template-rows: auto auto;
  }

  .cart-item-price,
  .cart-item-qty,
  .cart-item-subtotal,
  .cart-item-delete {
    grid-column: 3;
  }

  .cart-footer {
    flex-wrap: wrap;
    height: auto;
    padding-top: 10px;
    padding-bottom: 10px;
  }
}
</style>
