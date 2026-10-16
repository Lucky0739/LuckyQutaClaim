<template>
  <div class="page">
    <Header top-only />

    <main class="orders-main">
      <section class="orders-card">
        <div class="orders-head">
          <div>
            <h1>我的订单</h1>
            <p>查看当前账号的订单记录</p>
          </div>
        </div>

        <form class="orders-filter" @submit.prevent="loadOrders(1)">
          <input
            v-model.trim="filterOrderNo"
            type="text"
            class="orders-input"
            placeholder="输入订单号查询"
          />
          <select v-model="filterStatus" class="orders-input">
            <option value="">全部状态</option>
            <option :value="OrderStatus.PENDING_PAYMENT">待支付</option>
            <option :value="OrderStatus.PAID">待发货</option>
            <option :value="OrderStatus.SHIPPED">已发货</option>
            <option :value="OrderStatus.RECEIVED">已收货</option>
            <option :value="OrderStatus.CANCELLED">已取消</option>
            <option :value="OrderStatus.CLOSED">已过期</option>
          </select>
          <button type="submit" class="orders-btn orders-btn--primary" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button type="button" class="orders-btn" :disabled="loading" @click="resetFilter">
            重置
          </button>
        </form>

        <p v-if="errorMsg" class="orders-error">{{ errorMsg }}</p>

        <div v-if="loading" class="orders-state">加载订单中...</div>
        <div v-else-if="!orders.length" class="orders-state">暂无订单</div>

        <ul v-else class="orders-list">
          <li v-for="order in orders" :key="order.orderNo || order.id" class="order-card">
            <div class="order-goods-row">
              <router-link
                v-if="order.goodsId"
                :to="{ name: 'GoodsDetail', params: { id: order.goodsId } }"
                class="order-goods-thumb"
              >
                <img
                  v-if="order.goodsImg"
                  :src="resolveFileUrl(order.goodsImg)"
                  :alt="order.goodsName"
                  class="order-goods-thumb-img"
                  referrerpolicy="no-referrer"
                />
                <span v-else class="order-goods-thumb-img order-goods-thumb-img--empty">
                  {{ (order.goodsName || '商').slice(0, 1) }}
                </span>
              </router-link>
              <div v-else class="order-goods-thumb">
                <span class="order-goods-thumb-img order-goods-thumb-img--empty">
                  {{ (order.goodsName || '商').slice(0, 1) }}
                </span>
              </div>

              <div class="order-goods-main">
                <router-link
                  v-if="order.goodsId"
                  :to="{ name: 'GoodsDetail', params: { id: order.goodsId } }"
                  class="order-goods-title"
                >
                  {{ order.goodsName || '商品信息' }}
                </router-link>
                <h2 v-else class="order-goods-title">{{ order.goodsName || '商品信息' }}</h2>
                <p v-if="order.skuSpecName" class="order-goods-spec">{{ order.skuSpecName }}</p>
                <p class="order-goods-service">7天无理由退货</p>
              </div>

              <div class="order-goods-price">
                <strong>¥{{ formatOrderAmount(getUnitPrice(order)) }}</strong>
                <span>x{{ order.buyGoodsNum || order.goodsNum || 1 }}</span>
              </div>

              <div class="order-goods-pay">
                <p class="order-pay-total">
                  实付款
                  <strong>¥{{ formatOrderAmount(getOrderAmount(order)) }}</strong>
                </p>
                <p class="order-pay-freight">含运费：¥0.00</p>
                <p
                  v-if="isPendingPaymentOrder(order)"
                  class="order-pay-countdown"
                  :class="{ 'order-pay-countdown--expired': isOrderPayExpired(order) }"
                >
                  {{ getPayCountdownText(order) }}
                </p>
                <button
                  v-if="canPayOrder(order)"
                  type="button"
                  class="order-action-btn"
                  :disabled="payingOrderNo === order.orderNo"
                  @click="handlePay(order)"
                >
                  {{ payingOrderNo === order.orderNo ? '支付中...' : '去支付' }}
                </button>
                <button
                  v-else-if="canConfirmReceipt(order)"
                  type="button"
                  class="order-action-btn"
                  :disabled="receivingOrderNo === order.orderNo"
                  @click="openReceiveConfirm(order)"
                >
                  {{ receivingOrderNo === order.orderNo ? '处理中...' : '确认收货' }}
                </button>
                <router-link
                  v-else-if="order.goodsId"
                  :to="{ name: 'GoodsDetail', params: { id: order.goodsId } }"
                  class="order-action-btn"
                >
                  再买一单
                </router-link>
                <button
                  v-else-if="canCancelOrder(order)"
                  type="button"
                  class="order-action-btn order-action-btn--ghost"
                  :disabled="cancellingOrderNo === order.orderNo"
                  @click="openCancelConfirm(order)"
                >
                  {{ cancellingOrderNo === order.orderNo ? '取消中...' : '取消订单' }}
                </button>
              </div>
            </div>

            <div class="order-meta-bar">
              <div class="order-meta-left">
                <span>{{ formatOrderDate(order.createTime) }}</span>
                <span class="order-meta-divider">|</span>
                <span>订单号：{{ order.orderNo || '--' }}</span>
              </div>

              <div class="order-meta-center">
                <span class="order-shop-tag">秒杀商城</span>
                <span class="order-shop-name">官方自营店</span>
                <router-link
                  v-if="order.goodsId"
                  :to="{ name: 'GoodsDetail', params: { id: order.goodsId } }"
                  class="order-detail-link"
                >
                  订单详情 &gt;
                </router-link>
              </div>

              <div class="order-meta-right">
                <span class="order-status-text">{{ formatOrderStatusText(order) }}</span>
                <button
                  v-if="canCancelOrder(order)"
                  type="button"
                  class="order-delete-btn"
                  aria-label="取消订单"
                  :disabled="cancellingOrderNo === order.orderNo"
                  @click="openCancelConfirm(order)"
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      d="M9 3h6l1 2h4v2H4V5h4l1-2zm1 6h2v9h-2V9zm4 0h2v9h-2V9zM7 9h2v9H7V9z"
                      fill="currentColor"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </li>
        </ul>

        <div v-if="totalPages > 1" class="orders-pagination">
          <button type="button" class="orders-btn" :disabled="pageNum <= 1 || loading" @click="loadOrders(pageNum - 1)">
            上一页
          </button>
          <span>第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条</span>
          <button
            type="button"
            class="orders-btn"
            :disabled="pageNum >= totalPages || loading"
            @click="loadOrders(pageNum + 1)"
          >
            下一页
          </button>
        </div>
      </section>
    </main>

    <PayQrCodeModal
      v-model:visible="payQrVisible"
      :order-no="payQrOrderNo"
      :qr-code="payQrContent"
      :pay-url="payQrPayUrl"
      :amount="payQrAmount"
      :expire-time="payQrExpireTime"
      :create-time="payQrCreateTime"
      @success="handlePayQrSuccess"
      @expired="handlePayQrExpired"
    />

    <Teleport to="body">
      <div v-if="payMethodVisible && payMethodOrder" class="pay-method-mask" @click.self="closePayMethodModal">
        <div class="pay-method-modal">
          <div class="pay-method-modal-head">
            <h2>选择支付方式</h2>
            <button type="button" class="pay-method-modal-close" aria-label="关闭" @click="closePayMethodModal">×</button>
          </div>

          <p class="pay-method-order-amount">
            应付金额：<strong>¥{{ formatOrderAmount(getOrderAmount(payMethodOrder)) }}</strong>
          </p>

          <div class="pay-method-options">
            <button
              type="button"
              class="pay-method-option"
              :class="{ active: selectedPayMethod === 'alipay' }"
              :disabled="paying"
              @click="selectedPayMethod = 'alipay'"
            >
              <span class="pay-method-option-icon pay-method-option-icon--alipay">支</span>
              <span class="pay-method-option-text">
                <strong>支付宝支付</strong>
                <em>跳转支付宝沙箱收银台</em>
              </span>
            </button>

            <button
              type="button"
              class="pay-method-option"
              :class="{ active: selectedPayMethod === 'balance' }"
              :disabled="paying"
              @click="selectedPayMethod = 'balance'"
            >
              <span class="pay-method-option-icon pay-method-option-icon--balance">余</span>
              <span class="pay-method-option-text">
                <strong>余额支付</strong>
                <em>可用余额 ¥{{ balanceText }}</em>
              </span>
            </button>
          </div>

          <p v-if="selectedPayMethod === 'balance' && isBalanceInsufficient(payMethodOrder)" class="pay-method-warn">
            余额不足，请先到账户余额页充值
          </p>

          <div class="pay-method-modal-actions">
            <button type="button" class="orders-btn" :disabled="paying" @click="closePayMethodModal">
              取消
            </button>
            <button
              type="button"
              class="orders-btn orders-btn--primary"
              :disabled="paying || (selectedPayMethod === 'balance' && isBalanceInsufficient(payMethodOrder))"
              @click="confirmPayMethod"
            >
              {{ paying ? '支付中...' : '确认支付' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="cancelConfirmVisible && pendingCancelOrder"
        class="pay-method-mask"
        @click.self="closeCancelConfirm"
      >
        <div class="pay-method-modal cancel-order-modal">
          <div class="pay-method-modal-head">
            <h2>取消订单</h2>
            <button
              type="button"
              class="pay-method-modal-close"
              aria-label="关闭"
              :disabled="!!cancellingOrderNo"
              @click="closeCancelConfirm"
            >
              ×
            </button>
          </div>

          <p class="cancel-order-tip">确定要取消以下订单吗？取消后将释放预占库存，且无法恢复。</p>

          <div class="cancel-order-preview">
            <p class="cancel-order-line">
              <span>订单号</span>
              <strong>{{ pendingCancelOrder.orderNo }}</strong>
            </p>
            <p class="cancel-order-line">
              <span>商品</span>
              <strong>{{ pendingCancelOrder.goodsName || '商品信息' }}</strong>
            </p>
            <p v-if="pendingCancelOrder.skuSpecName" class="cancel-order-line">
              <span>规格</span>
              <strong>{{ pendingCancelOrder.skuSpecName }}</strong>
            </p>
            <p class="cancel-order-line">
              <span>应付金额</span>
              <strong class="cancel-order-amount">
                ¥{{ formatOrderAmount(getOrderAmount(pendingCancelOrder)) }}
              </strong>
            </p>
          </div>

          <div class="pay-method-modal-actions">
            <button
              type="button"
              class="orders-btn"
              :disabled="!!cancellingOrderNo"
              @click="closeCancelConfirm"
            >
              再想想
            </button>
            <button
              type="button"
              class="orders-btn orders-btn--danger"
              :disabled="!!cancellingOrderNo"
              @click="confirmCancelOrder"
            >
              {{ cancellingOrderNo ? '取消中...' : '确认取消' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="receiveConfirmVisible && pendingReceiveOrder"
        class="pay-method-mask"
        @click.self="closeReceiveConfirm"
      >
        <div class="pay-method-modal cancel-order-modal">
          <div class="pay-method-modal-head">
            <h2>确认收货</h2>
            <button
              type="button"
              class="pay-method-modal-close"
              aria-label="关闭"
              :disabled="!!receivingOrderNo"
              @click="closeReceiveConfirm"
            >
              ×
            </button>
          </div>

          <p class="cancel-order-tip">请确认您已收到商品且商品无误，确认后将完成交易。</p>

          <div class="cancel-order-preview">
            <p class="cancel-order-line">
              <span>订单号</span>
              <strong>{{ pendingReceiveOrder.orderNo }}</strong>
            </p>
            <p class="cancel-order-line">
              <span>商品</span>
              <strong>{{ pendingReceiveOrder.goodsName || '商品信息' }}</strong>
            </p>
            <p class="cancel-order-line">
              <span>实付金额</span>
              <strong class="cancel-order-amount">
                ¥{{ formatOrderAmount(getOrderAmount(pendingReceiveOrder)) }}
              </strong>
            </p>
          </div>

          <div class="pay-method-modal-actions">
            <button
              type="button"
              class="orders-btn"
              :disabled="!!receivingOrderNo"
              @click="closeReceiveConfirm"
            >
              再想想
            </button>
            <button
              type="button"
              class="orders-btn orders-btn--primary"
              :disabled="!!receivingOrderNo"
              @click="confirmReceiveOrder"
            >
              {{ receivingOrderNo ? '处理中...' : '确认收货' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Header from '@/components/Header.vue'
import PayQrCodeModal from '@/components/PayQrCodeModal.vue'
import { cancelOrder, confirmReceipt, getOrderById, getOrderList } from '@/api/order'
import { closeOrderIfExpired, scanAndCloseExpiredOrders } from '@/composables/useOrderExpire'
import { useOrderPay } from '@/composables/useOrderPay'
import { useUserBalance } from '@/composables/useUserBalance'
import { ORDER_PAY_TIMEOUT_MINUTES } from '@/constants/order'
import {
  canCancelOrder,
  canConfirmReceipt,
  canPayOrder,
  formatOrderAmount,
  formatOrderStatus,
  getOrderAmount,
  OrderStatus,
  type OrderVO,
} from '@/types/order'
import type { PaymentMethod } from '@/types/pay'
import { resolveFileUrl } from '@/utils/file'
import {
  formatPayCountdown,
  getOrderPayRemainMs,
  isOrderPayExpired,
  isPendingPaymentOrder,
} from '@/utils/orderExpire'

const route = useRoute()
const router = useRouter()
const orders = ref<OrderVO[]>([])
const loading = ref(false)
const errorMsg = ref('')
const initialOrderNo = typeof route.query.orderNo === 'string' ? route.query.orderNo : ''
const initialOrderId = typeof route.query.orderId === 'string' ? route.query.orderId : ''
const filterOrderNo = ref(initialOrderNo)
const filterStatus = ref<string | number>('')
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)
const totalPages = ref(0)
const cancellingOrderNo = ref('')
const payingOrderNo = ref('')
const shouldAutoPay = route.query.pay === '1'
const autoPayMethod = (route.query.payMethod === 'balance' ? 'balance' : 'alipay') as PaymentMethod
const payMethodVisible = ref(false)
const payMethodOrder = ref<OrderVO | null>(null)
const selectedPayMethod = ref<PaymentMethod>('alipay')
const { paying, payWithAlipay, payWithBalance } = useOrderPay()
const { balanceText, balanceInfo, loadBalance } = useUserBalance()
const payQrVisible = ref(false)
const payQrOrderNo = ref('')
const payQrContent = ref('')
const payQrPayUrl = ref('')
const payQrAmount = ref<number | undefined>(undefined)
const payQrExpireTime = ref('')
const payQrCreateTime = ref('')
const nowTick = ref(0)
let expireTickTimer = 0
const cancelConfirmVisible = ref(false)
const pendingCancelOrder = ref<OrderVO | null>(null)
const receivingOrderNo = ref('')
const receiveConfirmVisible = ref(false)
const pendingReceiveOrder = ref<OrderVO | null>(null)

function formatOrderDate(value?: string) {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 10)
}

function getUnitPrice(order: OrderVO) {
  if (order.goodsPrice != null) return order.goodsPrice
  const amount = getOrderAmount(order)
  const qty = order.buyGoodsNum || order.goodsNum || 1
  if (amount != null && qty > 0) return amount / qty
  return undefined
}

function formatOrderStatusText(order: OrderVO) {
  if (order.status === OrderStatus.RECEIVED) return '交易成功'
  if (order.status === OrderStatus.PAID) return '待发货'
  if (order.status === OrderStatus.SHIPPED) return '卖家已发货'
  if (isOrderPayExpired(order)) return '已过期'
  return formatOrderStatus(order)
}

function getPayCountdownText(order: OrderVO) {
  void nowTick.value
  if (!isPendingPaymentOrder(order)) return ''
  if (isOrderPayExpired(order)) return '支付已超时，订单将自动关闭'
  const remain = formatPayCountdown(getOrderPayRemainMs(order))
  return `请在 ${remain} 内完成支付（${ORDER_PAY_TIMEOUT_MINUTES} 分钟超时）`
}

async function loadOrders(targetPage = pageNum.value) {
  loading.value = true
  errorMsg.value = ''
  try {
    if (initialOrderId && targetPage === 1 && !filterOrderNo.value && filterStatus.value === '') {
      const order = await getOrderById(initialOrderId)
      orders.value = [order]
      pageNum.value = 1
      total.value = 1
      totalPages.value = 1
      return
    }
    const page = await getOrderList({
      orderNo: filterOrderNo.value,
      status: filterStatus.value,
      pageNum: targetPage,
      pageSize,
    })
    orders.value = page.records
    pageNum.value = page.pageNum
    total.value = page.total
    totalPages.value = page.totalPages
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载订单失败'
    orders.value = []
  } finally {
    loading.value = false
  }
}

function resetFilter() {
  filterOrderNo.value = ''
  filterStatus.value = ''
  loadOrders(1)
}

async function handlePay(order: OrderVO) {
  if (!order.orderNo) return
  if (isOrderPayExpired(order)) {
    errorMsg.value = '订单已超过支付时限'
    const closed = await closeOrderIfExpired(order)
    if (closed) await loadOrders(pageNum.value)
    return
  }
  payMethodOrder.value = order
  selectedPayMethod.value = 'alipay'
  payMethodVisible.value = true
  await loadBalance({ silent: true })
}

function closePayMethodModal() {
  if (paying.value) return
  payMethodVisible.value = false
  payMethodOrder.value = null
}

function isBalanceInsufficient(order: OrderVO | null) {
  const balance = balanceInfo.value?.balance
  const amount = order ? getOrderAmount(order) : undefined
  if (balance == null || amount == null || !Number.isFinite(balance) || !Number.isFinite(amount)) {
    return false
  }
  return balance < amount
}

async function confirmPayMethod() {
  const order = payMethodOrder.value
  if (!order?.orderNo) return

  if (selectedPayMethod.value === 'balance' && isBalanceInsufficient(order)) {
    errorMsg.value = '账户余额不足，请先充值或选择支付宝支付'
    return
  }

  payingOrderNo.value = order.orderNo
  errorMsg.value = ''
  try {
    if (selectedPayMethod.value === 'balance') {
      await payWithBalance(order)
      payMethodVisible.value = false
      payMethodOrder.value = null
      router.push({
        path: '/pay/result',
        query: { orderNo: order.orderNo, method: 'balance' },
      })
      return
    }

    const { presentation, payResult } = await payWithAlipay(order)

    if (presentation === 'url') {
      payMethodVisible.value = false
      payMethodOrder.value = null
      return
    }

    payQrOrderNo.value = payResult.orderNo || order.orderNo
    payQrContent.value = payResult.qrCode!.trim()
    payQrPayUrl.value = payResult.payUrl?.trim() || ''
    payQrAmount.value = getOrderAmount(order)
    payQrExpireTime.value = order.expireTime ?? ''
    payQrCreateTime.value = order.createTime ?? ''
    payQrVisible.value = true
    payMethodVisible.value = false
    payMethodOrder.value = null
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '发起支付失败'
  } finally {
    payingOrderNo.value = ''
  }
}

async function handlePayDirect(order: OrderVO, method: PaymentMethod) {
  payMethodOrder.value = order
  selectedPayMethod.value = method
  await confirmPayMethod()
}

async function handlePayQrSuccess() {
  await loadOrders(pageNum.value)
}

async function handlePayQrExpired() {
  payQrVisible.value = false
  errorMsg.value = '订单已超过 15 分钟支付时限，已自动关闭'
  await loadOrders(pageNum.value)
}

function openCancelConfirm(order: OrderVO) {
  if (!order.orderNo) return
  pendingCancelOrder.value = order
  cancelConfirmVisible.value = true
}

function closeCancelConfirm() {
  if (cancellingOrderNo.value) return
  cancelConfirmVisible.value = false
  pendingCancelOrder.value = null
}

async function confirmCancelOrder() {
  const order = pendingCancelOrder.value
  if (!order?.orderNo) return

  cancellingOrderNo.value = order.orderNo
  errorMsg.value = ''
  try {
    await cancelOrder(order.orderNo)
    cancelConfirmVisible.value = false
    pendingCancelOrder.value = null
    void loadOrders(pageNum.value)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '取消订单失败'
  } finally {
    cancellingOrderNo.value = ''
  }
}

function openReceiveConfirm(order: OrderVO) {
  if (!order.orderNo) return
  pendingReceiveOrder.value = order
  receiveConfirmVisible.value = true
}

function closeReceiveConfirm() {
  if (receivingOrderNo.value) return
  receiveConfirmVisible.value = false
  pendingReceiveOrder.value = null
}

async function confirmReceiveOrder() {
  const order = pendingReceiveOrder.value
  if (!order?.orderNo) return

  receivingOrderNo.value = order.orderNo
  errorMsg.value = ''
  try {
    await confirmReceipt(order.orderNo)
    receiveConfirmVisible.value = false
    pendingReceiveOrder.value = null
    void loadOrders(pageNum.value)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '确认收货失败'
  } finally {
    receivingOrderNo.value = ''
  }
}

onMounted(async () => {
  await loadOrders(1)
  expireTickTimer = window.setInterval(() => {
    nowTick.value += 1
    void scanAndCloseExpiredOrders(orders.value, () => loadOrders(pageNum.value))
  }, 1000)

  if (shouldAutoPay) {
    const pendingOrder = orders.value.find((order) => canPayOrder(order))
    if (pendingOrder) {
      await handlePayDirect(pendingOrder, autoPayMethod)
    }
  }
})

onUnmounted(() => {
  if (expireTickTimer) {
    window.clearInterval(expireTickTimer)
    expireTickTimer = 0
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f4f4f4;
}

.orders-main {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 16px 0 56px;
}

.orders-card {
  padding: 20px 0 0;
  background: transparent;
}

.orders-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 0 4px;
}

.orders-head h1 {
  margin: 0 0 6px;
  font-size: 20px;
  color: #333;
}

.orders-head p {
  margin: 0;
  color: #999;
  font-size: 12px;
}

.orders-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 8px;
  background: #fff;
}

.orders-input {
  height: 34px;
  min-width: 180px;
  padding: 0 12px;
  border: 1px solid #e5e5e5;
  border-radius: 4px;
  outline: none;
  font-size: 13px;
}

.orders-input:focus {
  border-color: #ff5000;
}

.orders-btn {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #ddd;
  border-radius: 16px;
  background: #fff;
  color: #666;
  cursor: pointer;
  font-size: 13px;
}

.orders-btn--primary {
  border-color: #ff5000;
  background: #ff5000;
  color: #fff;
}

.orders-btn--danger {
  border-color: #ff4d4f;
  background: #ff4d4f;
  color: #fff;
}

.orders-btn--danger:disabled {
  opacity: 0.6;
}

.orders-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.orders-error {
  margin: 0 0 12px;
  padding: 0 4px;
  color: #f5222d;
  font-size: 13px;
}

.orders-state {
  padding: 70px 0;
  text-align: center;
  color: #999;
  background: #fff;
  border-radius: 8px;
}

.orders-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.order-card {
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.order-goods-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr) 92px 170px;
  gap: 16px;
  align-items: start;
  padding: 18px 20px;
}

.order-goods-thumb {
  display: block;
  width: 92px;
  height: 92px;
  border-radius: 6px;
  overflow: hidden;
  background: #f7f7f7;
}

.order-goods-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.order-goods-thumb-img--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ccc;
  font-size: 28px;
  font-weight: 700;
}

.order-goods-main {
  min-width: 0;
  padding-top: 2px;
}

.order-goods-title {
  display: -webkit-box;
  margin: 0 0 8px;
  overflow: hidden;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  text-decoration: none;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.order-goods-title:hover {
  color: #ff5000;
}

.order-goods-spec {
  margin: 0 0 8px;
  color: #999;
  font-size: 12px;
  line-height: 1.5;
}

.order-goods-service {
  margin: 0;
  color: #999;
  font-size: 12px;
}

.order-goods-price {
  padding-top: 4px;
  text-align: center;
  color: #333;
}

.order-goods-price strong {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
}

.order-goods-price span {
  color: #999;
  font-size: 12px;
}

.order-goods-pay {
  padding-top: 2px;
  text-align: right;
}

.order-pay-total {
  margin: 0 0 4px;
  color: #333;
  font-size: 12px;
}

.order-pay-total strong {
  margin-left: 4px;
  font-size: 16px;
  font-weight: 700;
}

.order-pay-freight {
  margin: 0 0 8px;
  color: #999;
  font-size: 12px;
}

.order-pay-countdown {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--accent);
  line-height: 1.5;
}

.order-pay-countdown--expired {
  color: #f5222d;
}

.order-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 92px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid #ff5000;
  border-radius: 16px;
  background: #fff;
  color: #ff5000;
  font-size: 13px;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.order-action-btn:hover {
  background: #fff7f0;
}

.order-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.order-action-btn--ghost {
  border-color: #ddd;
  color: #666;
}

.order-meta-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 44px;
  padding: 0 20px;
  background: #f7f7f7;
  border-top: 1px solid #f0f0f0;
  color: #666;
  font-size: 12px;
}

.order-meta-left,
.order-meta-center,
.order-meta-right {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.order-meta-left {
  flex: 1;
  color: #999;
}

.order-meta-center {
  flex: 1.2;
  justify-content: center;
}

.order-meta-right {
  flex: 0 0 auto;
  justify-content: flex-end;
}

.order-meta-divider {
  color: #ddd;
}

.order-shop-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 18px;
  padding: 0 4px;
  border-radius: 2px;
  background: #ff5000;
  color: #fff;
  font-size: 11px;
  line-height: 1;
}

.order-shop-name {
  color: #333;
  font-size: 12px;
}

.order-detail-link {
  color: #ff5000;
  font-size: 12px;
  text-decoration: none;
}

.order-detail-link:hover {
  text-decoration: underline;
}

.order-status-text {
  color: #ff5000;
  font-size: 13px;
  font-weight: 500;
}

.order-delete-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: none;
  color: #bbb;
  cursor: pointer;
}

.order-delete-btn:hover {
  color: #999;
}

.order-delete-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.order-delete-btn svg {
  width: 16px;
  height: 16px;
}

.orders-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 22px;
  color: #777;
  font-size: 13px;
}

@media (max-width: 960px) {
  .order-goods-row {
    grid-template-columns: 80px minmax(0, 1fr);
  }

  .order-goods-price,
  .order-goods-pay {
    grid-column: 2;
  }

  .order-goods-pay {
    text-align: left;
  }

  .order-meta-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
    padding: 12px 16px;
  }

  .order-meta-center,
  .order-meta-right {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 768px) {
  .orders-main {
    padding: 12px 12px 40px;
  }

  .orders-filter {
    padding: 12px;
  }
}

.pay-method-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
}

.pay-method-modal {
  width: 420px;
  max-width: 100%;
  padding: 20px 22px 22px;
  border-radius: 12px;
  background: #fff;
}

.pay-method-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pay-method-modal-head h2 {
  margin: 0;
  font-size: 18px;
  color: #222;
}

.pay-method-modal-close {
  border: none;
  background: none;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
}

.pay-method-order-amount {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;
}

.pay-method-order-amount strong {
  color: var(--accent);
  font-size: 20px;
}

.pay-method-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pay-method-option {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
}

.pay-method-option:hover,
.pay-method-option.active {
  border-color: var(--accent);
  background: #fff7f0;
}

.pay-method-option:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.pay-method-option-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.pay-method-option-icon--alipay {
  background: #1677ff;
}

.pay-method-option-icon--balance {
  background: #ff8a00;
}

.pay-method-option-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.pay-method-option-text strong {
  font-size: 14px;
  color: #222;
}

.pay-method-option-text em {
  font-size: 12px;
  color: #999;
  font-style: normal;
}

.pay-method-warn {
  margin: 12px 0 0;
  font-size: 12px;
  color: #cf1322;
}

.pay-method-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.cancel-order-tip {
  margin: 0 0 14px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.cancel-order-preview {
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.cancel-order-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.5;
}

.cancel-order-line:last-child {
  margin-bottom: 0;
}

.cancel-order-line span {
  flex-shrink: 0;
  color: #999;
}

.cancel-order-line strong {
  text-align: right;
  color: #333;
  font-weight: 500;
}

.cancel-order-amount {
  color: var(--accent) !important;
  font-size: 16px;
  font-weight: 700 !important;
}
</style>
