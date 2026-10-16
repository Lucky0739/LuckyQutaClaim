<template>
  <section class="admin-seckill-goods-page">
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">秒杀商品审核</span>
    </div>

    <div class="account-panel admin-seckill-goods-panel">
      <header class="admin-seckill-goods-hero">
        <div class="admin-seckill-goods-hero-main">
          <div class="admin-seckill-goods-hero-badge">秒杀审核</div>
          <h2 class="admin-seckill-goods-hero-title">商家秒杀商品申请审核</h2>
          <p class="admin-seckill-goods-hero-desc">
            审核商家提交的秒杀商品申请，通过后活动开始即可在秒杀页展示售卖。
          </p>
        </div>
        <button type="button" class="admin-seckill-goods-refresh-btn" :disabled="loading" @click="loadList">
          <span class="admin-seckill-goods-refresh-icon" :class="{ 'is-spinning': loading }">↻</span>
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </header>

      <div class="admin-seckill-goods-tabs">
        <button
          type="button"
          class="admin-seckill-goods-tab"
          :class="{ 'admin-seckill-goods-tab--active': activeTab === 'pending' }"
          @click="switchTab('pending')"
        >
          待审核
          <span v-if="pendingTotal > 0" class="admin-seckill-goods-tab-count">{{ pendingTotal }}</span>
        </button>
        <button
          type="button"
          class="admin-seckill-goods-tab"
          :class="{ 'admin-seckill-goods-tab--active': activeTab === 'approved' }"
          @click="switchTab('approved')"
        >
          已通过
        </button>
        <button
          type="button"
          class="admin-seckill-goods-tab"
          :class="{ 'admin-seckill-goods-tab--active': activeTab === 'rejected' }"
          @click="switchTab('rejected')"
        >
          已拒绝
        </button>
      </div>

      <form class="admin-seckill-goods-filter" @submit.prevent="handleSearch">
        <label class="admin-seckill-goods-filter-item">
          <span>秒杀活动</span>
          <select v-model="filterActivityId" class="account-input">
            <option value="">全部活动</option>
            <option v-for="activity in activities" :key="activity.id" :value="activity.id">
              {{ activity.activityName }}
            </option>
          </select>
        </label>
        <div class="admin-seckill-goods-filter-actions">
          <button type="submit" class="account-btn account-btn-primary" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button type="button" class="account-btn" :disabled="loading" @click="handleReset">
            重置
          </button>
        </div>
      </form>

      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div v-if="!loading && (hasRecords || showPendingAutoRefresh)" class="admin-seckill-goods-toolbar">
        <p class="admin-seckill-goods-toolbar-tip">
          共 {{ total }} 条申请
          <span v-if="showPendingAutoRefresh" class="admin-seckill-goods-toolbar-live">
            · 每 10 秒自动刷新
            <span v-if="autoRefreshing" class="admin-seckill-goods-toolbar-sync">更新中...</span>
            <span v-else-if="lastRefreshedAt" class="admin-seckill-goods-toolbar-sync">
              最近更新 {{ formatRefreshTime(lastRefreshedAt) }}
            </span>
          </span>
        </p>
      </div>

      <div v-if="loading" class="admin-seckill-goods-loading">
        <span class="address-loading-dot"></span>
        加载申请列表...
      </div>

      <div v-else-if="errorMsg && !hasRecords" class="admin-seckill-goods-empty">
        <div class="admin-seckill-goods-empty-icon admin-seckill-goods-empty-icon--warn">!</div>
        <p class="admin-seckill-goods-empty-title">加载失败</p>
        <p class="admin-seckill-goods-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasRecords" class="admin-seckill-goods-empty">
        <div class="admin-seckill-goods-empty-icon">⚡</div>
        <p class="admin-seckill-goods-empty-title">{{ emptyTitle }}</p>
        <p class="admin-seckill-goods-empty-text">商家可在「商家中心 → 秒杀商品申请」提交新的秒杀商品。</p>
      </div>

      <div v-else class="admin-seckill-goods-table-wrap">
        <table class="admin-seckill-goods-table">
          <thead>
            <tr>
              <th>秒杀商品</th>
              <th>活动</th>
              <th>秒杀价 / 库存</th>
              <th>商家</th>
              <th>审核状态</th>
              <th>申请时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in records" :key="item.id">
              <td>
                <div class="admin-seckill-goods-cell">
                  <img
                    v-if="item.seckillImg"
                    :src="resolveFileUrl(item.seckillImg)"
                    alt=""
                    class="admin-seckill-goods-img"
                    referrerpolicy="no-referrer"
                  />
                  <span v-else class="admin-seckill-goods-img admin-seckill-goods-img--placeholder">图</span>
                  <div>
                    <p class="admin-seckill-goods-name">{{ item.seckillTitle || '--' }}</p>
                    <p class="admin-seckill-goods-meta">
                      商品 {{ item.goodsId }} · {{ item.skuSpecName || '默认规格' }}
                    </p>
                    <p v-if="item.auditRemark" class="admin-seckill-goods-remark">备注：{{ item.auditRemark }}</p>
                  </div>
                </div>
              </td>
              <td>{{ item.activityName || item.activityId }}</td>
              <td>
                <p class="admin-seckill-goods-price">¥{{ formatSeckillPrice(item.seckillPrice) }}</p>
                <p class="admin-seckill-goods-meta">库存 {{ item.seckillTotal ?? '--' }} · 限购 {{ item.seckillLimit ?? 1 }}</p>
              </td>
              <td>{{ item.userId || '--' }}</td>
              <td>
                <span class="audit-tag" :class="getAuditClass(item.auditStatus)">
                  {{ formatSeckillGoodsAuditStatus(item.auditStatus) }}
                </span>
              </td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td>
                <button
                  v-if="activeTab === 'pending'"
                  type="button"
                  class="admin-seckill-goods-btn admin-seckill-goods-btn--primary"
                  @click="openAuditModal(item)"
                >
                  审核
                </button>
                <span v-else class="admin-seckill-goods-meta">--</span>
              </td>
            </tr>
          </tbody>
        </table>
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
      <div v-if="showAuditModal && auditingItem" class="address-modal-mask" @click.self="closeAuditModal">
        <div class="address-modal admin-seckill-goods-audit-modal">
          <div class="address-modal-header">
            <h2 class="address-modal-title">审核秒杀商品申请</h2>
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

            <div class="admin-seckill-goods-audit-preview">
              <img
                v-if="auditingItem.seckillImg"
                :src="resolveFileUrl(auditingItem.seckillImg)"
                alt=""
                class="admin-seckill-goods-audit-image"
                referrerpolicy="no-referrer"
              />
              <div v-else class="admin-seckill-goods-audit-image admin-seckill-goods-audit-image--placeholder">
                {{ auditingItem.seckillTitle?.slice(0, 2) || '秒杀' }}
              </div>
              <div class="admin-seckill-goods-audit-info">
                <p class="admin-seckill-goods-audit-name">{{ auditingItem.seckillTitle || '--' }}</p>
                <p class="admin-seckill-goods-audit-meta">
                  活动 {{ auditingItem.activityName || auditingItem.activityId }}
                </p>
                <p class="admin-seckill-goods-audit-meta">
                  商品 {{ auditingItem.goodsId }} · {{ auditingItem.skuSpecName || '默认规格' }}
                </p>
                <p class="admin-seckill-goods-audit-meta">
                  秒杀价 ¥{{ formatSeckillPrice(auditingItem.seckillPrice) }}
                  · 库存 {{ auditingItem.seckillTotal ?? '--' }}
                  · 限购 {{ auditingItem.seckillLimit ?? 1 }}
                </p>
                <p class="admin-seckill-goods-audit-meta">商家 ID {{ auditingItem.userId || '--' }}</p>
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label">审核备注</label>
              <div class="account-form-field">
                <textarea
                  v-model="auditRemark"
                  class="admin-seckill-goods-textarea"
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
              @click="submitAudit(SeckillGoodsAuditStatus.REJECTED)"
            >
              {{ auditSubmitting ? '处理中...' : '拒绝' }}
            </button>
            <button
              type="button"
              class="account-btn account-btn-primary"
              :disabled="auditSubmitting"
              @click="submitAudit(SeckillGoodsAuditStatus.APPROVED)"
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
import { useAdminSeckillGoodsList } from '@/composables/useAdminSeckillGoodsList'
import { formatSeckillPrice } from '@/types/activity'
import { resolveFileUrl } from '@/utils/file'

const {
  loading,
  errorMsg,
  toastMsg,
  records,
  activities,
  pageNum,
  total,
  totalPages,
  pendingTotal,
  activeTab,
  filterActivityId,
  autoRefreshing,
  lastRefreshedAt,
  showAuditModal,
  auditingItem,
  auditRemark,
  auditSubmitting,
  auditErrorMsg,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  showPendingAutoRefresh,
  emptyTitle,
  loadList,
  switchTab,
  handleSearch,
  handleReset,
  goPrevPage,
  goNextPage,
  openAuditModal,
  closeAuditModal,
  submitAudit,
  formatDateTime,
  formatRefreshTime,
  formatSeckillGoodsAuditStatus,
  SeckillGoodsAuditStatus,
} = useAdminSeckillGoodsList()

function getAuditClass(status?: number) {
  return {
    'audit-tag--pending': status === SeckillGoodsAuditStatus.PENDING,
    'audit-tag--approved': status === SeckillGoodsAuditStatus.APPROVED,
    'audit-tag--rejected': status === SeckillGoodsAuditStatus.REJECTED,
  }
}
</script>

<style scoped>
.admin-seckill-goods-panel {
  padding: 24px;
}

.admin-seckill-goods-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.admin-seckill-goods-hero-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  margin-bottom: 10px;
  border-radius: 999px;
  background: #fff7e6;
  color: #d48806;
  font-size: 12px;
  font-weight: 600;
}

.admin-seckill-goods-hero-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.admin-seckill-goods-hero-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.admin-seckill-goods-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #555;
  cursor: pointer;
}

.admin-seckill-goods-refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-seckill-goods-refresh-icon.is-spinning {
  animation: admin-seckill-spin 0.8s linear infinite;
}

@keyframes admin-seckill-spin {
  to {
    transform: rotate(360deg);
  }
}

.admin-seckill-goods-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-seckill-goods-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 16px;
  border: 1px solid #e5e5e5;
  border-radius: 999px;
  background: #fff;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.admin-seckill-goods-tab--active {
  border-color: transparent;
  background: var(--brand-gradient);
  color: #fff;
}

.admin-seckill-goods-tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  font-size: 11px;
  line-height: 18px;
}

.admin-seckill-goods-filter {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 14px;
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 10px;
  background: #fafafa;
}

.admin-seckill-goods-filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
  font-size: 12px;
  color: #666;
}

.admin-seckill-goods-filter-actions {
  display: flex;
  gap: 10px;
}

.admin-seckill-goods-toolbar {
  margin-bottom: 12px;
}

.admin-seckill-goods-toolbar-tip {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.admin-seckill-goods-toolbar-live {
  color: #666;
}

.admin-seckill-goods-toolbar-sync {
  color: #bbb;
}

.admin-seckill-goods-loading,
.admin-seckill-goods-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 240px;
  text-align: center;
}

.admin-seckill-goods-empty-icon {
  font-size: 36px;
}

.admin-seckill-goods-empty-icon--warn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #fff2f0;
  color: #f5222d;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.admin-seckill-goods-empty-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.admin-seckill-goods-empty-text {
  margin: 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.admin-seckill-goods-table-wrap {
  overflow-x: auto;
  border: 1px solid #eee;
  border-radius: 10px;
}

.admin-seckill-goods-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  font-size: 12px;
}

.admin-seckill-goods-table th,
.admin-seckill-goods-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: middle;
}

.admin-seckill-goods-table th {
  background: #fafafa;
  color: #666;
  font-weight: 600;
}

.admin-seckill-goods-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.admin-seckill-goods-img {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
}

.admin-seckill-goods-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #bbb;
}

.admin-seckill-goods-name {
  margin: 0 0 4px;
  font-weight: 600;
  color: #333;
}

.admin-seckill-goods-meta,
.admin-seckill-goods-remark {
  margin: 0;
  color: #999;
  line-height: 1.6;
}

.admin-seckill-goods-remark {
  color: #f5222d;
}

.admin-seckill-goods-price {
  margin: 0 0 4px;
  color: var(--accent);
  font-weight: 700;
}

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

.audit-tag--pending {
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #d48806;
}

.audit-tag--approved {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.audit-tag--rejected {
  border: 1px solid #ffccc7;
  background: #fff2f0;
  color: #f5222d;
}

.admin-seckill-goods-btn {
  height: 30px;
  padding: 0 14px;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  background: #fff;
  color: #555;
  cursor: pointer;
}

.admin-seckill-goods-btn--primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.admin-seckill-goods-audit-modal {
  width: min(560px, calc(100vw - 32px));
}

.admin-seckill-goods-audit-preview {
  display: flex;
  gap: 14px;
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 10px;
  background: #fafafa;
}

.admin-seckill-goods-audit-image {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
}

.admin-seckill-goods-audit-image--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #bbb;
}

.admin-seckill-goods-audit-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.admin-seckill-goods-audit-meta {
  margin: 0 0 4px;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}

.admin-seckill-goods-textarea {
  width: 100%;
  min-height: 88px;
  padding: 10px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  resize: vertical;
  font: inherit;
}

@media (max-width: 768px) {
  .admin-seckill-goods-hero {
    flex-direction: column;
  }

  .admin-seckill-goods-filter {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
