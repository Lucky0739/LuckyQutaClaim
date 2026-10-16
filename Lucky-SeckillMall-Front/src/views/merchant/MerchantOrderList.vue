<template>
  <div class="page">
    <Header top-only />

    <main class="order-manage-page">
      <div class="order-manage-inner">
        <nav class="account-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span>商家中心</span>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span class="account-breadcrumb-current">订单管理</span>
        </nav>

        <section class="order-panel">
          <div class="order-panel-head">
            <div>
              <h1 class="order-panel-title">订单管理</h1>
              <p class="order-panel-desc">查看买家订单，处理待发货订单并完成发货</p>
            </div>
          </div>

          <form class="order-filter" @submit.prevent="handleSearch">
            <label class="order-filter-item">
              <span>订单号</span>
              <input
                v-model.trim="filterOrderNo"
                type="text"
                class="order-filter-input"
                placeholder="请输入订单号"
              />
            </label>
            <label class="order-filter-item">
              <span>商品名称</span>
              <input
                v-model.trim="filterGoodsName"
                type="text"
                class="order-filter-input"
                placeholder="请输入商品名称"
              />
            </label>
            <label class="order-filter-item">
              <span>订单状态</span>
              <select v-model="filterStatus" class="order-filter-input">
                <option value="">全部</option>
                <option :value="OrderStatus.PAID">待发货</option>
                <option :value="OrderStatus.SHIPPED">已发货</option>
                <option :value="OrderStatus.RECEIVED">已收货</option>
                <option :value="OrderStatus.CANCELLED">已取消</option>
              </select>
            </label>
            <div class="order-filter-actions">
              <button type="submit" class="filter-btn filter-btn--primary" :disabled="loading">
                {{ loading ? '查询中...' : '查询' }}
              </button>
              <button type="button" class="filter-btn" :disabled="loading" @click="handleReset">
                重置
              </button>
            </div>
          </form>

          <p v-if="errorMsg" class="order-msg order-msg--error">{{ errorMsg }}</p>
          <p v-if="successMsg" class="order-msg order-msg--success">{{ successMsg }}</p>

          <div class="order-toolbar">
            <span>共 {{ total }} 笔订单</span>
          </div>

          <div v-if="loading" class="order-loading">加载订单列表...</div>

          <div v-else-if="!records.length" class="order-empty">
            <p>暂无订单数据</p>
          </div>

          <div v-else class="order-table-wrap">
            <table class="order-table">
              <thead>
                <tr>
                  <th>商品</th>
                  <th>买家信息</th>
                  <th>金额</th>
                  <th>状态</th>
                  <th>下单时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in records" :key="item.orderNo || item.id">
                  <td>
                    <div class="order-goods-cell">
                      <img
                        v-if="item.goodsImg"
                        :src="resolveFileUrl(item.goodsImg)"
                        alt=""
                        class="order-goods-img"
                        referrerpolicy="no-referrer"
                      />
                      <span v-else class="order-goods-img order-goods-img--placeholder">图</span>
                      <div class="order-goods-meta">
                        <span class="order-goods-name" :title="item.goodsName">{{ item.goodsName || '--' }}</span>
                        <span class="order-goods-sub">x{{ item.buyGoodsNum || item.goodsNum || 1 }}</span>
                        <span class="order-goods-sub">订单号：{{ item.orderNo }}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <p class="order-receiver">{{ item.receiver || '--' }} {{ item.mobile || '' }}</p>
                    <p class="order-address">{{ formatOrderAddress(item) }}</p>
                  </td>
                  <td class="order-amount">¥{{ formatOrderAmount(getOrderAmount(item)) }}</td>
                  <td>
                    <span class="status-tag" :class="getStatusClass(item.status)">
                      {{ formatOrderStatus(item) }}
                    </span>
                  </td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td>
                    <button
                      v-if="canDeliverOrder(item)"
                      type="button"
                      class="order-action-btn"
                      :disabled="deliveringOrderNo === item.orderNo"
                      @click="openDeliverConfirm(item)"
                    >
                      {{ deliveringOrderNo === item.orderNo ? '发货中...' : '确认发货' }}
                    </button>
                    <span v-else class="order-action-tip">{{ getActionTip(item) }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="records.length && totalPages > 1" class="order-pagination">
            <button type="button" class="filter-btn" :disabled="pageNum <= 1 || loading" @click="goPrevPage">
              上一页
            </button>
            <span>第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条</span>
            <button
              type="button"
              class="filter-btn"
              :disabled="pageNum >= totalPages || loading"
              @click="goNextPage"
            >
              下一页
            </button>
          </div>
        </section>
      </div>
    </main>

    <Teleport to="body">
      <div
        v-if="deliverConfirmVisible && pendingDeliverOrder"
        class="order-modal-mask"
        @click.self="closeDeliverConfirm"
      >
        <div class="order-modal">
          <div class="order-modal-head">
            <h2>确认发货</h2>
            <button
              type="button"
              class="order-modal-close"
              aria-label="关闭"
              :disabled="!!deliveringOrderNo"
              @click="closeDeliverConfirm"
            >
              ×
            </button>
          </div>

          <p class="order-modal-tip">确认该订单已发出商品？发货后买家将收到通知并可确认收货。</p>

          <div class="order-modal-preview">
            <p class="order-modal-line">
              <span>订单号</span>
              <strong>{{ pendingDeliverOrder.orderNo }}</strong>
            </p>
            <p class="order-modal-line">
              <span>商品</span>
              <strong>{{ pendingDeliverOrder.goodsName || '--' }}</strong>
            </p>
            <p class="order-modal-line">
              <span>收货人</span>
              <strong>{{ pendingDeliverOrder.receiver || '--' }} {{ pendingDeliverOrder.mobile || '' }}</strong>
            </p>
            <p class="order-modal-line">
              <span>收货地址</span>
              <strong>{{ formatOrderAddress(pendingDeliverOrder) }}</strong>
            </p>
          </div>

          <div class="order-modal-actions">
            <button
              type="button"
              class="filter-btn"
              :disabled="!!deliveringOrderNo"
              @click="closeDeliverConfirm"
            >
              取消
            </button>
            <button
              type="button"
              class="filter-btn filter-btn--primary"
              :disabled="!!deliveringOrderNo"
              @click="confirmDeliverOrder"
            >
              {{ deliveringOrderNo ? '发货中...' : '确认发货' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Header from '@/components/Header.vue'
import { deliverOrder, getMerchantOrderList } from '@/api/order'
import { formatDateTime } from '@/composables/useAdminUserList'
import {
  canDeliverOrder,
  formatOrderAddress,
  formatOrderAmount,
  formatOrderStatus,
  getOrderAmount,
  OrderStatus,
  type OrderVO,
} from '@/types/order'
import { resolveFileUrl } from '@/utils/file'

const PAGE_SIZE = 10

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const records = ref<OrderVO[]>([])
const pageNum = ref(1)
const total = ref(0)
const totalPages = ref(0)
const filterOrderNo = ref('')
const filterGoodsName = ref('')
const filterStatus = ref<OrderStatus | ''>('')
const deliveringOrderNo = ref('')
const deliverConfirmVisible = ref(false)
const pendingDeliverOrder = ref<OrderVO | null>(null)

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    const result = await getMerchantOrderList({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      orderNo: filterOrderNo.value,
      goodsName: filterGoodsName.value,
      status: filterStatus.value,
    })
    records.value = result.records ?? []
    total.value = result.total ?? 0
    totalPages.value = result.totalPages ?? 0
    pageNum.value = result.pageNum ?? pageNum.value
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载订单列表失败'
    records.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handleReset() {
  filterOrderNo.value = ''
  filterGoodsName.value = ''
  filterStatus.value = ''
  pageNum.value = 1
  loadList()
}

function goPrevPage() {
  if (pageNum.value <= 1 || loading.value) return
  pageNum.value -= 1
  loadList()
}

function goNextPage() {
  if (pageNum.value >= totalPages.value || loading.value) return
  pageNum.value += 1
  loadList()
}

function getStatusClass(status?: number) {
  return {
    'status-tag--paid': status === OrderStatus.PAID,
    'status-tag--shipped': status === OrderStatus.SHIPPED,
    'status-tag--received': status === OrderStatus.RECEIVED,
    'status-tag--cancelled': status === OrderStatus.CANCELLED || status === OrderStatus.CLOSED,
  }
}

function getActionTip(order: OrderVO) {
  if (order.status === OrderStatus.SHIPPED) return '等待买家收货'
  if (order.status === OrderStatus.RECEIVED) return '交易完成'
  if (order.status === OrderStatus.CANCELLED) return '订单已取消'
  if (order.status === OrderStatus.PENDING_PAYMENT) return '等待买家付款'
  return '--'
}

function openDeliverConfirm(order: OrderVO) {
  if (!order.orderNo) return
  pendingDeliverOrder.value = order
  deliverConfirmVisible.value = true
  successMsg.value = ''
}

function closeDeliverConfirm() {
  if (deliveringOrderNo.value) return
  deliverConfirmVisible.value = false
  pendingDeliverOrder.value = null
}

async function confirmDeliverOrder() {
  const order = pendingDeliverOrder.value
  if (!order?.orderNo) return

  deliveringOrderNo.value = order.orderNo
  errorMsg.value = ''
  try {
    await deliverOrder(order.orderNo)
    deliverConfirmVisible.value = false
    pendingDeliverOrder.value = null
    successMsg.value = `订单 ${order.orderNo} 发货成功`
    void loadList()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '发货失败'
  } finally {
    deliveringOrderNo.value = ''
  }
}

onMounted(loadList)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.order-manage-page {
  padding: 16px 0 48px;
}

.order-manage-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 0 10px;
}

.order-panel {
  padding: 24px 28px 32px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
}

.order-panel-head {
  margin-bottom: 20px;
}

.order-panel-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.order-panel-desc {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.order-filter {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) 160px auto;
  gap: 14px;
  align-items: end;
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 10px;
  background: #fafafa;
}

.order-filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  font-size: 12px;
  color: #666;
}

.order-filter-input {
  height: 34px;
  padding: 0 10px;
  border: 1px solid #dddddd;
  border-radius: 6px;
  background: #fff;
  color: #333;
  outline: none;
}

.order-filter-input:focus {
  border-color: var(--accent);
}

.order-filter-actions,
.order-pagination {
  display: flex;
  align-items: center;
  gap: 10px;
}

.filter-btn {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #dddddd;
  border-radius: 6px;
  background: #fff;
  color: #555;
  cursor: pointer;
}

.filter-btn--primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.order-msg {
  margin: 0 0 14px;
  font-size: 13px;
}

.order-msg--error {
  color: #f5222d;
}

.order-msg--success {
  color: #52c41a;
}

.order-toolbar {
  margin-bottom: 12px;
  font-size: 12px;
  color: #999;
}

.order-loading,
.order-empty {
  padding: 56px 0;
  text-align: center;
  color: #999;
}

.order-table-wrap {
  overflow-x: auto;
  border: 1px solid #eeeeee;
  border-radius: 10px;
}

.order-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12px;
}

.order-table th,
.order-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: top;
}

.order-table th {
  background: #fafafa;
  color: #666;
  font-weight: 600;
}

.order-goods-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.order-goods-img {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eeeeee;
  background: #f5f5f5;
}

.order-goods-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
}

.order-goods-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-goods-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  color: #333;
}

.order-goods-sub {
  color: #999;
}

.order-receiver {
  margin: 0 0 4px;
  color: #333;
  font-weight: 600;
}

.order-address {
  margin: 0;
  color: #999;
  line-height: 1.5;
}

.order-amount {
  color: var(--accent);
  font-weight: 700;
  white-space: nowrap;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.status-tag--paid {
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #d48806;
}

.status-tag--shipped {
  border: 1px solid #91d5ff;
  background: #e6f7ff;
  color: #1890ff;
}

.status-tag--received {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.status-tag--cancelled {
  border: 1px solid #d9d9d9;
  background: #f5f5f5;
  color: #999;
}

.order-action-btn {
  height: 30px;
  padding: 0 12px;
  border: 1px solid var(--accent);
  border-radius: 6px;
  background: #fff;
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.order-action-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.order-action-tip {
  color: #999;
}

.order-pagination {
  justify-content: center;
  margin-top: 18px;
  color: #666;
}

.order-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  background: rgba(0, 0, 0, 0.45);
}

.order-modal {
  width: 460px;
  max-width: 100%;
  padding: 20px 22px 22px;
  border-radius: 12px;
  background: #fff;
}

.order-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.order-modal-head h2 {
  margin: 0;
  font-size: 18px;
  color: #222;
}

.order-modal-close {
  border: none;
  background: none;
  font-size: 24px;
  line-height: 1;
  color: #999;
  cursor: pointer;
}

.order-modal-tip {
  margin: 0 0 14px;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.order-modal-preview {
  padding: 14px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.order-modal-line {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin: 0 0 10px;
  font-size: 13px;
  line-height: 1.5;
}

.order-modal-line:last-child {
  margin-bottom: 0;
}

.order-modal-line span {
  flex-shrink: 0;
  color: #999;
}

.order-modal-line strong {
  text-align: right;
  color: #333;
  font-weight: 500;
}

.order-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

@media (max-width: 900px) {
  .order-filter {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .order-filter {
    grid-template-columns: 1fr;
  }
}
</style>
