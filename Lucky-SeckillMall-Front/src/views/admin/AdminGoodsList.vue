<template>
  <section class="admin-goods-page">
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">商品管理</span>
    </div>

    <div class="account-panel admin-goods-panel">
      <header class="admin-goods-hero">
        <div class="admin-goods-hero-main">
          <div class="admin-goods-hero-badge">商品审核</div>
          <h2 class="admin-goods-hero-title">上架申请审核中心</h2>
          <p class="admin-goods-hero-desc">
            审核商家提交的商品上架申请，通过后商品将自动上架并通知商家。
          </p>
        </div>
        <button type="button" class="admin-goods-refresh-btn" :disabled="loading" @click="loadList">
          <span class="admin-goods-refresh-icon" :class="{ 'is-spinning': loading }">↻</span>
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </header>

      <div class="admin-goods-tabs">
        <button
          type="button"
          class="admin-goods-tab"
          :class="{ 'admin-goods-tab--active': activeTab === 'pending' }"
          @click="switchTab('pending')"
        >
          待审核
          <span v-if="pendingTotal > 0" class="admin-goods-tab-count">{{ pendingTotal }}</span>
        </button>
        <button
          type="button"
          class="admin-goods-tab"
          :class="{ 'admin-goods-tab--active': activeTab === 'passed' }"
          @click="switchTab('passed')"
        >
          已通过
        </button>
        <button
          type="button"
          class="admin-goods-tab"
          :class="{ 'admin-goods-tab--active': activeTab === 'rejected' }"
          @click="switchTab('rejected')"
        >
          已拒绝
        </button>
      </div>

      <form class="admin-goods-filter" @submit.prevent="handleSearch">
        <div class="admin-goods-filter-grid">
          <div class="admin-goods-filter-item">
            <label class="admin-goods-filter-label">商品名称</label>
            <input
              v-model="filterGoodsName"
              type="text"
              maxlength="200"
              class="account-input admin-goods-filter-input"
              placeholder="模糊搜索商品名称"
            />
          </div>
          <div class="admin-goods-filter-item">
            <label class="admin-goods-filter-label">开始时间</label>
            <input
              v-model="filterStartTime"
              type="datetime-local"
              class="account-input admin-goods-filter-input admin-goods-filter-input--datetime"
            />
          </div>
          <div class="admin-goods-filter-item">
            <label class="admin-goods-filter-label">结束时间</label>
            <input
              v-model="filterEndTime"
              type="datetime-local"
              class="account-input admin-goods-filter-input admin-goods-filter-input--datetime"
            />
          </div>
        </div>
        <div class="admin-goods-filter-actions">
          <button type="submit" class="account-btn account-btn-primary" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button type="button" class="account-btn" :disabled="loading" @click="handleReset">
            重置
          </button>
        </div>
      </form>

      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div v-if="!loading && (hasRecords || showPendingAutoRefresh)" class="admin-goods-toolbar">
        <p class="admin-goods-toolbar-tip">
          共 {{ total }} 件商品
          <span v-if="showPendingAutoRefresh" class="admin-goods-toolbar-live">
            · 每 10 秒自动刷新（直连数据库，无缓存延迟）
            <span v-if="autoRefreshing" class="admin-goods-toolbar-sync">更新中...</span>
            <span v-else-if="lastRefreshedAt" class="admin-goods-toolbar-sync">
              最近更新 {{ formatRefreshTime(lastRefreshedAt) }}
            </span>
          </span>
        </p>
      </div>

      <div v-if="loading" class="admin-goods-loading">
        <span class="address-loading-dot"></span>
        加载商品列表...
      </div>

      <div v-else-if="errorMsg && !hasRecords" class="admin-goods-empty">
        <div class="admin-goods-empty-icon admin-goods-empty-icon--warn">!</div>
        <p class="admin-goods-empty-title">加载失败</p>
        <p class="admin-goods-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasRecords" class="admin-goods-empty">
        <div class="admin-goods-empty-icon">📦</div>
        <p class="admin-goods-empty-title">{{ emptyTitle }}</p>
        <p class="admin-goods-empty-text">{{ emptyDesc }}</p>
      </div>

      <div v-else class="admin-goods-grid">
        <article v-for="goods in records" :key="goods.id" class="admin-goods-card">
          <div class="admin-goods-card-media">
            <img
              v-if="goods.goodsImg && !imageErrors[goods.id]"
              :src="resolveFileUrl(goods.goodsImg)"
              alt=""
              class="admin-goods-card-image"
              referrerpolicy="no-referrer"
              @error="imageErrors[goods.id] = true"
            />
            <div v-else class="admin-goods-card-image admin-goods-card-image--placeholder">
              {{ goods.goodsName?.slice(0, 2) || '商品' }}
            </div>
            <span class="admin-goods-card-status" :class="getAuditTagClass(activeTab)">
              {{ getTabStatusLabel(activeTab) }}
            </span>
          </div>

          <div class="admin-goods-card-body">
            <h3 class="admin-goods-card-name" :title="goods.goodsName">
              {{ goods.goodsName || '--' }}
            </h3>
            <p class="admin-goods-card-id">ID {{ goods.id }}</p>

            <div class="admin-goods-card-meta">
              <span class="admin-goods-card-price">¥{{ formatGoodsPrice(goods.goodsPrice) }}</span>
              <span class="admin-goods-card-sales">{{ formatGoodsSalesNum(goods.salesNum) }}</span>
            </div>

            <p class="admin-goods-card-category" :title="formatGoodsCategory(goods)">
              {{ formatGoodsCategory(goods) }}
            </p>
            <p v-if="goods.updateTime || goods.createTime" class="admin-goods-card-time">
              提交时间 {{ formatDateTime(goods.updateTime || goods.createTime) }}
            </p>
          </div>

          <div class="admin-goods-card-footer">
            <RouterLink
              :to="{ name: 'GoodsDetail', params: { id: goods.id } }"
              class="admin-goods-btn admin-goods-btn--ghost"
              target="_blank"
            >
              查看详情
            </RouterLink>
            <button
              v-if="activeTab === 'pending'"
              type="button"
              class="admin-goods-btn admin-goods-btn--primary"
              @click="openAuditModal(goods)"
            >
              审核
            </button>
          </div>
        </article>
      </div>

      <div v-if="hasRecords && totalPages > 1" class="notify-pagination">
        <button type="button" class="account-btn" :disabled="!hasPrevPage || loading" @click="goPrevPage">
          上一页
        </button>
        <span class="notify-pagination-info">
          第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条
        </span>
        <button type="button" class="account-btn" :disabled="!hasNextPage || loading" @click="goNextPage">
          下一页
        </button>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="address-toast-fade">
        <div v-if="toastMsg" class="address-success-toast">{{ toastMsg }}</div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <div v-if="showAuditModal && auditingGoods" class="address-modal-mask" @click.self="closeAuditModal">
        <div class="address-modal admin-goods-audit-modal">
          <div class="address-modal-header">
            <h2 class="address-modal-title">审核商品上架申请</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="auditSubmitting"
              @click="closeAuditModal"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="auditErrorMsg" class="account-msg account-msg--error">{{ auditErrorMsg }}</p>

            <div class="admin-goods-audit-preview">
              <img
                v-if="auditingGoods.goodsImg"
                :src="resolveFileUrl(auditingGoods.goodsImg)"
                alt=""
                class="admin-goods-audit-image"
                referrerpolicy="no-referrer"
              />
              <div v-else class="admin-goods-audit-image admin-goods-audit-image--placeholder">
                {{ auditingGoods.goodsName?.slice(0, 2) || '商品' }}
              </div>
              <div class="admin-goods-audit-info">
                <p class="admin-goods-audit-name">{{ auditingGoods.goodsName }}</p>
                <p class="admin-goods-audit-meta">
                  ID {{ auditingGoods.id }} · ¥{{ formatGoodsPrice(auditingGoods.goodsPrice) }}
                </p>
                <p class="admin-goods-audit-category">{{ formatGoodsCategory(auditingGoods) }}</p>
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label">审核备注</label>
              <div class="account-form-field">
                <textarea
                  v-model="auditRemark"
                  class="admin-goods-textarea"
                  rows="3"
                  maxlength="200"
                  placeholder="拒绝时必填；通过时可留空"
                />
              </div>
            </div>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="auditSubmitting" @click="closeAuditModal">
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-danger"
              :disabled="auditSubmitting"
              @click="submitAudit(GoodsAuditStatus.REJECT)"
            >
              {{ auditSubmitting ? '处理中...' : '拒绝' }}
            </button>
            <button
              type="button"
              class="account-btn account-btn-primary"
              :disabled="auditSubmitting"
              @click="submitAudit(GoodsAuditStatus.PASS)"
            >
              {{ auditSubmitting ? '处理中...' : '通过' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useAdminGoodsList, type AdminGoodsTab } from '@/composables/useAdminGoodsList'
import { resolveFileUrl } from '@/utils/file'

const imageErrors = reactive<Record<string, boolean>>({})

const {
  loading,
  errorMsg,
  toastMsg,
  pendingTotal,
  records,
  pageNum,
  total,
  totalPages,
  activeTab,
  filterGoodsName,
  filterStartTime,
  filterEndTime,
  showAuditModal,
  auditingGoods,
  auditRemark,
  auditSubmitting,
  auditErrorMsg,
  autoRefreshing,
  lastRefreshedAt,
  showPendingAutoRefresh,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  loadList,
  switchTab,
  handleSearch,
  handleReset,
  goPrevPage,
  goNextPage,
  openAuditModal,
  closeAuditModal,
  submitAudit,
  formatGoodsCategory,
  formatGoodsPrice,
  formatGoodsSalesNum,
  formatDateTime,
  GoodsAuditStatus,
} = useAdminGoodsList()

function formatRefreshTime(date: Date) {
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

const emptyTitle = computed(() => {
  if (activeTab.value === 'pending') return '暂无待审核商品'
  if (activeTab.value === 'passed') return '暂无已通过商品'
  return '暂无已拒绝商品'
})

const emptyDesc = computed(() => {
  if (activeTab.value === 'pending') return '当前没有商家提交的上架申请。'
  if (activeTab.value === 'passed') return '还没有审核通过的商品记录。'
  return '还没有审核拒绝的商品记录。'
})

function getTabStatusLabel(tab: AdminGoodsTab): string {
  if (tab === 'pending') return '待审核'
  if (tab === 'passed') return '已通过'
  return '已拒绝'
}

function getAuditTagClass(tab: AdminGoodsTab): string {
  if (tab === 'pending') return 'admin-goods-card-status--pending'
  if (tab === 'passed') return 'admin-goods-card-status--passed'
  return 'admin-goods-card-status--rejected'
}
</script>

<style scoped>
.admin-page-tab {
  cursor: default;
}

.admin-goods-panel {
  padding-top: 20px;
}

.admin-goods-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  padding: 22px 24px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff7f2 0%, #ffffff 58%, #fffaf7 100%);
  border: 1px solid rgba(255, 80, 0, 0.12);
  box-shadow: 0 8px 24px rgba(255, 80, 0, 0.06);
}

.admin-goods-hero-badge {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 0 10px;
  border-radius: 999px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 22px;
}

.admin-goods-hero-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.admin-goods-hero-desc {
  margin: 0;
  max-width: 560px;
  font-size: 13px;
  line-height: 1.7;
  color: #777;
}

.admin-goods-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 0 16px;
  height: 38px;
  border: 1px solid #ffd6bf;
  border-radius: 999px;
  background: #fff;
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.admin-goods-refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-goods-refresh-icon {
  display: inline-block;
  font-size: 16px;
}

.admin-goods-refresh-icon.is-spinning {
  animation: admin-goods-spin 0.8s linear infinite;
}

@keyframes admin-goods-spin {
  to {
    transform: rotate(360deg);
  }
}

.admin-goods-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-goods-tab {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  height: 36px;
  border: 1px solid #ececec;
  border-radius: 999px;
  background: #fff;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s;
}

.admin-goods-tab:hover {
  border-color: #ffd6bf;
  color: var(--accent);
}

.admin-goods-tab--active {
  border-color: transparent;
  background: var(--brand-gradient);
  color: #fff;
  box-shadow: 0 6px 16px rgba(255, 80, 0, 0.18);
}

.admin-goods-tab-count {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 11px;
  line-height: 20px;
  text-align: center;
}

.admin-goods-filter {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #e5e5e5;
}

.admin-goods-filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 24px;
}

.admin-goods-filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.admin-goods-filter-label {
  flex-shrink: 0;
  width: 64px;
  font-size: 12px;
  color: #666;
  text-align: right;
}

.admin-goods-filter-input {
  flex: 1;
  max-width: none;
}

.admin-goods-filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-left: 76px;
}

.admin-goods-toolbar {
  margin-bottom: 16px;
}

.admin-goods-toolbar-tip {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.admin-goods-toolbar-live {
  color: #666;
}

.admin-goods-toolbar-sync {
  margin-left: 4px;
  color: var(--accent);
}

.admin-goods-card-time {
  margin: 6px 0 0;
  font-size: 11px;
  color: #999;
}

.admin-goods-loading,
.admin-goods-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 260px;
  padding: 32px 20px;
  border-radius: 14px;
  border: 1px dashed #e8e8e8;
  background: #fcfcfc;
  text-align: center;
}

.admin-goods-empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 24px;
  font-weight: 700;
}

.admin-goods-empty-icon--warn {
  background: #fff2f0;
  color: #f5222d;
}

.admin-goods-empty-title {
  margin: 4px 0 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.admin-goods-empty-text {
  margin: 0;
  max-width: 360px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.admin-goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}

.admin-goods-card {
  display: flex;
  flex-direction: column;
  border-radius: 14px;
  border: 1px solid #ececec;
  background: #fff;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  transition: transform 0.18s, box-shadow 0.18s;
}

.admin-goods-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.08);
}

.admin-goods-card-media {
  position: relative;
  aspect-ratio: 1;
  background: #f5f5f5;
}

.admin-goods-card-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-goods-card-image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
  color: #bbb;
  background: linear-gradient(180deg, #fafafa 0%, #f0f0f0 100%);
}

.admin-goods-card-status {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 0 10px;
  border-radius: 999px;
  line-height: 24px;
  font-size: 11px;
  font-weight: 600;
}

.admin-goods-card-status--pending {
  color: #d46b08;
  background: rgba(255, 247, 230, 0.95);
  border: 1px solid #ffd591;
}

.admin-goods-card-status--passed {
  color: #389e0d;
  background: rgba(246, 255, 237, 0.95);
  border: 1px solid #b7eb8f;
}

.admin-goods-card-status--rejected {
  color: #cf1322;
  background: rgba(255, 242, 240, 0.95);
  border: 1px solid #ffa39e;
}

.admin-goods-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 14px 10px;
}

.admin-goods-card-name {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-goods-card-id {
  margin: 0;
  font-size: 11px;
  color: #999;
}

.admin-goods-card-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.admin-goods-card-price {
  font-size: 18px;
  font-weight: 700;
  color: var(--accent);
}

.admin-goods-card-sales {
  font-size: 11px;
  color: #999;
}

.admin-goods-card-category {
  margin: 0;
  font-size: 12px;
  color: #777;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-goods-card-footer {
  display: flex;
  gap: 10px;
  padding: 0 14px 14px;
  margin-top: auto;
}

.admin-goods-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  text-decoration: none;
  transition: all 0.18s;
}

.admin-goods-btn--ghost {
  border: 1px solid #e5e5e5;
  background: #fff;
  color: #666;
}

.admin-goods-btn--ghost:hover {
  border-color: #ffd6bf;
  color: var(--accent);
}

.admin-goods-btn--primary {
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  box-shadow: 0 4px 12px rgba(255, 80, 0, 0.18);
}

.admin-goods-audit-modal {
  max-width: 520px;
}

.admin-goods-audit-preview {
  display: flex;
  gap: 14px;
  margin-bottom: 18px;
  padding: 14px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  background: #fafafa;
}

.admin-goods-audit-image {
  flex-shrink: 0;
  width: 88px;
  height: 88px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #eee;
}

.admin-goods-audit-image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #bbb;
  font-size: 22px;
  font-weight: 700;
}

.admin-goods-audit-info {
  min-width: 0;
}

.admin-goods-audit-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: #222;
  line-height: 1.5;
}

.admin-goods-audit-meta,
.admin-goods-audit-category {
  margin: 0 0 4px;
  font-size: 12px;
  color: #888;
  line-height: 1.6;
}

.admin-goods-textarea {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid #ccc;
  font-size: 12px;
  color: #333;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}

.admin-goods-textarea:focus {
  border-color: var(--accent);
}

@media (max-width: 960px) {
  .admin-goods-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-goods-hero {
    flex-direction: column;
  }

  .admin-goods-refresh-btn {
    width: 100%;
    justify-content: center;
  }

  .admin-goods-filter-grid {
    grid-template-columns: 1fr;
  }

  .admin-goods-filter-actions {
    padding-left: 0;
  }

  .admin-goods-grid {
    grid-template-columns: 1fr;
  }
}
</style>
