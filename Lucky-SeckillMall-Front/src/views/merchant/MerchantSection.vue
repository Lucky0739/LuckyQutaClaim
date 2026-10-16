<template>
  <div class="page">
    <Header top-only />

    <main class="goods-manage-page">
      <div class="goods-manage-inner">
        <nav class="account-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span>商家中心</span>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span class="account-breadcrumb-current">商品管理</span>
        </nav>

        <section class="goods-panel">
          <div class="goods-panel-head">
            <div>
              <h1 class="goods-panel-title">商品管理</h1>
              <p class="goods-panel-desc">管理商品上下架，提交上架申请并等待管理员审核</p>
            </div>
            <div class="goods-panel-actions">
              <router-link to="/merchant/seckill" class="publish-btn publish-btn--ghost">
                秒杀商品申请
              </router-link>
              <router-link to="/merchant/goods/publish" class="publish-btn">
                <span class="publish-btn-icon">+</span>
                发布商品
              </router-link>
            </div>
          </div>

          <form class="goods-filter" @submit.prevent="handleSearch">
            <label class="goods-filter-item">
              <span>商品名称</span>
              <input
                v-model="filterName"
                type="text"
                class="goods-filter-input"
                placeholder="请输入商品名称"
              />
            </label>
            <label class="goods-filter-item">
              <span>上架状态</span>
              <select v-model="filterStatus" class="goods-filter-input">
                <option value="">全部</option>
                <option :value="GoodsStatus.ON_SHELF">上架</option>
                <option :value="GoodsStatus.OFF_SHELF">下架</option>
              </select>
            </label>
            <label class="goods-filter-item">
              <span>审核状态</span>
              <select v-model="filterAuditStatus" class="goods-filter-input">
                <option value="">全部</option>
                <option :value="MerchantGoodsAuditStatus.NOT_APPLIED">未申请</option>
                <option :value="MerchantGoodsAuditStatus.PENDING">待审核</option>
                <option :value="MerchantGoodsAuditStatus.PASS">审核通过</option>
                <option :value="MerchantGoodsAuditStatus.REJECT">审核拒绝</option>
              </select>
            </label>
            <div class="goods-filter-actions">
              <button type="submit" class="filter-btn filter-btn--primary" :disabled="loading">
                {{ loading ? '查询中...' : '查询' }}
              </button>
              <button type="button" class="filter-btn" :disabled="loading" @click="handleReset">
                重置
              </button>
            </div>
          </form>

          <p v-if="errorMsg" class="goods-msg goods-msg--error">{{ errorMsg }}</p>
          <p v-if="successMsg" class="goods-msg goods-msg--success">{{ successMsg }}</p>

          <div class="goods-toolbar">
            <span>共 {{ total }} 个商品</span>
          </div>

          <div v-if="loading" class="goods-loading">加载商品列表...</div>

          <div v-else-if="!records.length" class="goods-empty">
            <p>暂无商品数据</p>
            <router-link to="/merchant/goods/publish" class="publish-btn publish-btn--small">
              去发布商品
            </router-link>
          </div>

          <div v-else class="goods-table-wrap">
            <table class="goods-table">
              <thead>
                <tr>
                  <th>商品</th>
                  <th>价格</th>
                  <th>上架状态</th>
                  <th>审核状态</th>
                  <th>分类</th>
                  <th>销量</th>
                  <th>创建时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in records" :key="item.id">
                  <td>
                    <div class="goods-cell">
                      <img
                        v-if="item.goodsImg"
                        :src="resolveFileUrl(item.goodsImg)"
                        alt=""
                        class="goods-img"
                        referrerpolicy="no-referrer"
                      />
                      <span v-else class="goods-img goods-img--placeholder">图</span>
                      <div class="goods-meta">
                        <span class="goods-name" :title="item.goodsName">{{ item.goodsName }}</span>
                        <span class="goods-id">ID: {{ item.id }}</span>
                      </div>
                    </div>
                  </td>
                  <td class="goods-price">¥{{ formatGoodsPrice(item.goodsPrice) }}</td>
                  <td>
                    <span class="status-tag" :class="getStatusClass(item.status)">
                      {{ item.statusDesc || formatGoodsStatus(item.status) }}
                    </span>
                  </td>
                  <td>
                    <span class="audit-tag" :class="getAuditClass(item.auditStatus)">
                      {{ formatMerchantGoodsAuditStatus(item.auditStatus) }}
                    </span>
                  </td>
                  <td>{{ formatGoodsCategory(item) }}</td>
                  <td>{{ item.salesNum ?? 0 }}</td>
                  <td>{{ formatDateTime(item.createTime) }}</td>
                  <td>
                    <div class="goods-actions">
                      <button
                        v-if="canMerchantApplyOnShelf(item)"
                        type="button"
                        class="goods-btn goods-btn--apply"
                        :disabled="!!actionGoodsId"
                        @click="openApplyDialog(item)"
                      >
                        <span class="goods-btn-icon" aria-hidden="true">↑</span>
                        申请上架
                      </button>
                      <span
                        v-else-if="item.auditStatus === MerchantGoodsAuditStatus.PENDING"
                        class="goods-action-tip"
                      >
                        审核中
                      </span>
                      <button
                        v-if="canMerchantOffShelf(item)"
                        type="button"
                        class="goods-btn goods-btn--off"
                        :disabled="!!actionGoodsId"
                        @click="handleOffShelf(item)"
                      >
                        下架
                      </button>
                      <router-link
                        v-if="canApplySeckill(item)"
                        :to="{ path: '/merchant/seckill', query: { goodsId: item.id } }"
                        class="goods-btn goods-btn--seckill"
                      >
                        申请秒杀
                      </router-link>
                      <router-link
                        :to="{ name: 'MerchantGoodsDetail', params: { id: item.id } }"
                        class="goods-btn goods-btn--detail"
                      >
                        <span class="goods-btn-icon" aria-hidden="true">›</span>
                        详情
                      </router-link>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="records.length && totalPages > 1" class="goods-pagination">
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

    <ApplyOnShelfConfirmDialog
      v-model="showApplyDialog"
      :goods="pendingApplyItem"
      :loading="!!actionGoodsId"
      @confirm="confirmApplyOnShelf"
      @cancel="closeApplyDialog"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import ApplyOnShelfConfirmDialog from '@/components/merchant/ApplyOnShelfConfirmDialog.vue'
import Header from '@/components/Header.vue'
import { applyOnShelf, getMyGoodsPage, offShelf } from '@/api/goods'
import {
  canMerchantApplyOnShelf,
  canMerchantOffShelf,
  formatGoodsCategory,
  formatGoodsPrice,
  formatMerchantGoodsAuditStatus,
  GoodsStatus,
  MerchantGoodsAuditStatus,
  type GoodsListVO,
  type GoodsQueryRequest,
} from '@/types/goods'
import { formatDateTime } from '@/composables/useAdminUserList'
import { resolveFileUrl } from '@/utils/file'

const PAGE_SIZE = 10

const loading = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const actionGoodsId = ref('')
const showApplyDialog = ref(false)
const pendingApplyItem = ref<GoodsListVO | null>(null)
const records = ref<GoodsListVO[]>([])
const pageNum = ref(1)
const total = ref(0)
const totalPages = ref(0)
const filterName = ref('')
const filterStatus = ref<GoodsStatus | ''>('')
const filterAuditStatus = ref('')

function buildQuery(): GoodsQueryRequest {
  const query: GoodsQueryRequest = {
    pageNum: pageNum.value,
    pageSize: PAGE_SIZE,
  }

  const goodsName = filterName.value.trim()
  if (goodsName) query.goodsName = goodsName
  if (filterStatus.value !== '') query.status = filterStatus.value
  if (filterAuditStatus.value !== '') query.auditStatus = Number(filterAuditStatus.value)

  return query
}

function clearMessages() {
  errorMsg.value = ''
  successMsg.value = ''
}

async function loadList(keepMessages = false) {
  loading.value = true
  if (!keepMessages) clearMessages()
  try {
    const result = await getMyGoodsPage(buildQuery())
    records.value = result.records ?? []
    total.value = result.total ?? 0
    totalPages.value = result.totalPages ?? 0
    pageNum.value = result.pageNum ?? pageNum.value
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载商品列表失败'
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
  filterName.value = ''
  filterStatus.value = ''
  filterAuditStatus.value = ''
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

function canApplySeckill(item: GoodsListVO): boolean {
  return item.status === GoodsStatus.ON_SHELF && item.auditStatus === MerchantGoodsAuditStatus.PASS
}

function openApplyDialog(item: GoodsListVO) {
  if (actionGoodsId.value || !canMerchantApplyOnShelf(item)) return
  pendingApplyItem.value = item
  showApplyDialog.value = true
}

function closeApplyDialog() {
  showApplyDialog.value = false
  pendingApplyItem.value = null
}

async function confirmApplyOnShelf() {
  const item = pendingApplyItem.value
  if (!item || actionGoodsId.value) return

  actionGoodsId.value = item.id
  clearMessages()
  try {
    await applyOnShelf(item.id)
    closeApplyDialog()
    await loadList(true)
    successMsg.value = '上架申请已提交，请等待管理员审核'
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '申请上架失败'
  } finally {
    actionGoodsId.value = ''
  }
}

async function handleOffShelf(item: GoodsListVO) {
  if (actionGoodsId.value || !canMerchantOffShelf(item)) return
  const name = item.goodsName || '该商品'
  if (!window.confirm(`确定将「${name}」下架吗？下架后买家将无法购买。`)) return

  actionGoodsId.value = item.id
  clearMessages()
  try {
    await offShelf(item.id)
    await loadList(true)
    successMsg.value = '商品已下架'
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '下架失败'
  } finally {
    actionGoodsId.value = ''
  }
}

function formatGoodsStatus(status?: number) {
  if (status === GoodsStatus.ON_SHELF) return '上架'
  if (status === GoodsStatus.OFF_SHELF) return '下架'
  return '--'
}

function getStatusClass(status?: number) {
  return {
    'status-tag--on': status === GoodsStatus.ON_SHELF,
    'status-tag--off': status !== GoodsStatus.ON_SHELF,
  }
}

function getAuditClass(status?: number) {
  return {
    'audit-tag--pending':
      status === MerchantGoodsAuditStatus.PENDING ||
      status === MerchantGoodsAuditStatus.NOT_APPLIED,
    'audit-tag--approved': status === MerchantGoodsAuditStatus.PASS,
    'audit-tag--rejected': status === MerchantGoodsAuditStatus.REJECT,
  }
}

onMounted(loadList)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.goods-manage-page {
  padding: 16px 0 48px;
}

.goods-manage-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 0 10px;
}

.goods-panel {
  padding: 24px 28px 32px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
}

.goods-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.goods-panel-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.goods-panel-desc {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.goods-panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.publish-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
  box-shadow: 0 6px 14px rgba(255, 80, 0, 0.2);
}

.publish-btn--small {
  height: 32px;
  font-size: 13px;
}

.publish-btn--ghost {
  background: #fff;
  color: var(--accent);
  border: 1px solid var(--accent);
  box-shadow: none;
}

.publish-btn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  font-size: 16px;
}

.goods-filter {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 180px 180px auto;
  gap: 14px;
  align-items: end;
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 10px;
  background: #fafafa;
}

.goods-filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  font-size: 12px;
  color: #666;
}

.goods-filter-input {
  height: 34px;
  padding: 0 10px;
  border: 1px solid #dddddd;
  border-radius: 6px;
  background: #fff;
  color: #333;
  outline: none;
}

.goods-filter-input:focus {
  border-color: var(--accent);
}

.goods-filter-actions,
.goods-pagination {
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

.goods-msg {
  margin: 0 0 14px;
  font-size: 13px;
}

.goods-msg--error {
  color: #f5222d;
}

.goods-msg--success {
  color: #52c41a;
}

.goods-toolbar {
  margin-bottom: 12px;
  font-size: 12px;
  color: #999;
}

.goods-loading,
.goods-empty {
  padding: 56px 0;
  text-align: center;
  color: #999;
}

.goods-table-wrap {
  overflow-x: auto;
  border: 1px solid #eeeeee;
  border-radius: 10px;
}

.goods-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: 12px;
}

.goods-table th,
.goods-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: middle;
}

.goods-table th {
  background: #fafafa;
  color: #666;
  font-weight: 600;
}

.goods-table th:nth-child(1),
.goods-table td:nth-child(1) {
  width: 280px;
}

.goods-table th:nth-child(2),
.goods-table td:nth-child(2) {
  width: 100px;
}

.goods-table th:nth-child(3),
.goods-table td:nth-child(3),
.goods-table th:nth-child(4),
.goods-table td:nth-child(4) {
  width: 110px;
}

.goods-table th:nth-child(5),
.goods-table td:nth-child(5) {
  width: 120px;
}

.goods-table th:nth-child(6),
.goods-table td:nth-child(6) {
  width: 80px;
}

.goods-table th:nth-child(7),
.goods-table td:nth-child(7) {
  width: 150px;
  white-space: nowrap;
}

.goods-table th:nth-child(8),
.goods-table td:nth-child(8) {
  width: 200px;
}

.goods-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.goods-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s,
    border-color 0.15s,
    background 0.15s,
    color 0.15s;
}

.goods-btn-icon {
  font-size: 13px;
  line-height: 1;
}

.goods-btn--apply {
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  box-shadow: 0 4px 10px rgba(255, 80, 0, 0.2);
}

.goods-btn--apply:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 14px rgba(255, 80, 0, 0.28);
}

.goods-btn--seckill {
  border: 1px solid #ffd591;
  background: #fff7e6;
  color: #d48806;
}

.goods-btn--seckill:hover {
  border-color: #ffc069;
  background: #fff1b8;
}

.goods-btn--detail {
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #555;
}

.goods-btn--detail:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: #fff9f5;
}

.goods-btn--detail .goods-btn-icon {
  font-size: 15px;
  font-weight: 700;
}

.goods-btn--off {
  border: 1px solid #ffd8bf;
  background: #fff;
  color: #fa8c16;
}

.goods-btn--off:hover:not(:disabled) {
  border-color: #ffb98a;
  background: #fff7e6;
}

.goods-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.goods-action-tip {
  display: inline-flex;
  align-items: center;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid #ffe58f;
  background: #fffbe6;
  font-size: 12px;
  color: #d48806;
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.goods-img {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eeeeee;
  background: #f5f5f5;
}

.goods-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
}

.goods-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.goods-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
  color: #333;
}

.goods-id {
  color: #999;
}

.goods-price {
  color: var(--accent);
  font-weight: 700;
}

.status-tag,
.audit-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.status-tag--on,
.audit-tag--approved {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.status-tag--off {
  border: 1px solid #d9d9d9;
  background: #f5f5f5;
  color: #999;
}

.audit-tag--pending {
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #d48806;
}

.audit-tag--rejected {
  border: 1px solid #ffccc7;
  background: #fff2f0;
  color: #f5222d;
}

.goods-pagination {
  justify-content: center;
  margin-top: 18px;
  color: #666;
}

@media (max-width: 900px) {
  .goods-filter {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .goods-filter,
  .goods-panel-head {
    grid-template-columns: 1fr;
    flex-direction: column;
  }
}
</style>
