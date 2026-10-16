<template>
  <section class="admin-failed-order-page">
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">失败订单处理</span>
    </div>

    <div class="account-panel admin-failed-order-panel">
      <header class="admin-failed-order-hero">
        <div class="admin-failed-order-hero-main">
          <div class="admin-failed-order-hero-badge">DLQ 死信队列</div>
          <h2 class="admin-failed-order-hero-title">秒杀失败订单人工处理</h2>
          <p class="admin-failed-order-hero-desc">
            DLQ 重试耗尽后订单进入死信队列，管理员可手动重试或放弃。重试将重新投递到主队列并重置重试次数。
          </p>
        </div>
        <button
          type="button"
          class="admin-failed-order-refresh-btn"
          :disabled="loading"
          @click="loadList"
        >
          <span class="admin-failed-order-refresh-icon" :class="{ 'is-spinning': loading }">↻</span>
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </header>

      <!-- 后端按单一处理状态筛选，分别展示以保证分页与总数准确 -->
      <div class="admin-failed-order-tabs">
        <button
          type="button"
          class="admin-failed-order-tab"
          :class="{ 'admin-failed-order-tab--active': activeTab === 'pending' }"
          @click="switchTab('pending')"
        >
          待处理
          <span v-if="pendingTotal > 0" class="admin-failed-order-tab-count">{{ pendingTotal }}</span>
        </button>
        <button
          type="button"
          class="admin-failed-order-tab"
          :class="{ 'admin-failed-order-tab--active': activeTab === 'retried' }"
          @click="switchTab('retried')"
        >
          已重试
        </button>
        <button
          type="button"
          class="admin-failed-order-tab"
          :class="{ 'admin-failed-order-tab--active': activeTab === 'discarded' }"
          @click="switchTab('discarded')"
        >
          已放弃
        </button>
      </div>

      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div v-if="!loading && hasRecords" class="admin-failed-order-toolbar">
        <p class="admin-failed-order-toolbar-tip">
          共 {{ total }} 条
          <span v-if="activeTab === 'pending'" class="admin-failed-order-toolbar-live">
            · 每 15 秒自动刷新
            <span v-if="autoRefreshing" class="admin-failed-order-toolbar-sync">更新中...</span>
            <span v-else-if="lastRefreshedAt" class="admin-failed-order-toolbar-sync">
              最近更新 {{ formatRefreshTime(lastRefreshedAt) }}
            </span>
          </span>
        </p>
      </div>

      <div v-if="loading" class="admin-failed-order-loading">
        <span class="address-loading-dot"></span>
        加载失败订单列表...
      </div>

      <div v-else-if="errorMsg && !hasRecords" class="admin-failed-order-empty">
        <div class="admin-failed-order-empty-icon admin-failed-order-empty-icon--warn">!</div>
        <p class="admin-failed-order-empty-title">加载失败</p>
        <p class="admin-failed-order-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasRecords" class="admin-failed-order-empty">
        <div class="admin-failed-order-empty-icon">📋</div>
        <p class="admin-failed-order-empty-title">{{ emptyTitle }}</p>
        <p class="admin-failed-order-empty-text">
          {{
            activeTab === 'pending'
              ? '当前没有需要人工处理的失败订单。'
              : activeTab === 'retried'
                ? '已手动重试的失败订单将显示在这里。'
                : '已手动放弃的失败订单将显示在这里。'
          }}
        </p>
      </div>

      <div v-else class="admin-failed-order-table-wrap">
        <table class="admin-failed-order-table">
          <thead>
            <tr>
              <th>订单号 / 商品</th>
              <th>秒杀价 / 数量</th>
              <th>失败原因</th>
              <th>重试次数</th>
              <th>处理状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in records" :key="item.id">
              <td>
                <div class="admin-failed-order-cell">
                  <img
                    v-if="item.goodsImg"
                    :src="resolveFileUrl(item.goodsImg)"
                    alt=""
                    class="admin-failed-order-img"
                    referrerpolicy="no-referrer"
                  />
                  <span v-else class="admin-failed-order-img admin-failed-order-img--placeholder">商</span>
                  <div>
                    <p class="admin-failed-order-name">{{ item.goodsName || '--' }}</p>
                    <p class="admin-failed-order-meta">
                      订单 {{ item.orderNo || item.id }}
                    </p>
                    <p class="admin-failed-order-meta">
                      用户 {{ item.userId || '--' }} · 商品 {{ item.goodsId || '--' }}
                    </p>
                  </div>
                </div>
              </td>
              <td>
                <p class="admin-failed-order-price">
                  ¥{{ item.seckillPrice != null ? item.seckillPrice.toFixed(2) : '--' }}
                </p>
                <p class="admin-failed-order-meta">×{{ item.buyNum ?? 1 }}</p>
              </td>
              <td>
                <p class="admin-failed-order-error" :title="item.lastError">
                  {{ item.lastError || '--' }}
                </p>
              </td>
              <td>
                <span class="admin-failed-order-retry-badge" :class="getRetryClass(item.retryCount)">
                  {{ item.retryCount ?? 0 }} 次
                </span>
              </td>
              <td>
                <span class="audit-tag" :class="getHandleStatusClass(item.handleStatus)">
                  {{ item.handleStatusText || formatFailedOrderHandleStatus(item.handleStatus) }}
                </span>
                <p v-if="item.handledBy" class="admin-failed-order-meta">
                  {{ item.handledBy }}
                </p>
                <p v-if="item.handledTime" class="admin-failed-order-meta">
                  {{ formatDateTime(item.handledTime) }}
                </p>
              </td>
              <td>{{ formatDateTime(item.createTime) }}</td>
              <td>
                <template v-if="canHandleFailedOrder(item.handleStatus)">
                  <button
                    type="button"
                    class="admin-failed-order-btn admin-failed-order-btn--retry"
                    @click="openRetryConfirm(item)"
                  >
                    重试
                  </button>
                  <button
                    type="button"
                    class="admin-failed-order-btn admin-failed-order-btn--discard"
                    @click="openDiscardDialog(item)"
                  >
                    放弃
                  </button>
                </template>
                <span v-else class="admin-failed-order-meta">--</span>
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

    <!-- Toast -->
    <Teleport to="body">
      <Transition name="address-toast-fade">
        <div v-if="toastMsg" class="address-success-toast">{{ toastMsg }}</div>
      </Transition>
    </Teleport>

    <!-- 重试确认弹窗 -->
    <Teleport to="body">
      <div
        v-if="showRetryConfirm && retryingItem"
        class="address-modal-mask"
        @click.self="closeRetryConfirm"
      >
        <div class="address-delete-dialog admin-failed-order-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">重试失败订单</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="retrySubmitting"
              @click="closeRetryConfirm"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="retryErrorMsg" class="account-msg account-msg--error">{{ retryErrorMsg }}</p>

            <div class="admin-failed-order-dialog-preview">
              <div class="admin-failed-order-dialog-preview-head">
                <img
                  v-if="retryingItem.goodsImg"
                  :src="resolveFileUrl(retryingItem.goodsImg)"
                  alt=""
                  class="admin-failed-order-dialog-img"
                  referrerpolicy="no-referrer"
                />
                <span v-else class="admin-failed-order-dialog-img admin-failed-order-dialog-img--placeholder">
                  {{ (retryingItem.goodsName || '商').slice(0, 2) }}
                </span>
                <div>
                  <p class="admin-failed-order-dialog-name">{{ retryingItem.goodsName || '--' }}</p>
                  <p class="admin-failed-order-dialog-meta">
                    订单 {{ retryingItem.orderNo || retryingItem.id }}
                  </p>
                </div>
              </div>
              <ul class="admin-failed-order-dialog-detail">
                <li>
                  <span>秒杀价</span>
                  <strong>
                    ¥{{ retryingItem.seckillPrice != null ? retryingItem.seckillPrice.toFixed(2) : '--' }}
                  </strong>
                </li>
                <li>
                  <span>失败原因</span>
                  <strong>{{ retryingItem.lastError || '--' }}</strong>
                </li>
                <li>
                  <span>已重试次数</span>
                  <strong>{{ retryingItem.retryCount ?? 0 }} 次</strong>
                </li>
              </ul>
            </div>

            <p class="admin-user-confirm-text">
              确认后该订单将<strong>重新投递到主队列</strong>，重试次数清零并重新开始 DLQ 自动重试流程。
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="retrySubmitting" @click="closeRetryConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-primary"
              :disabled="retrySubmitting"
              @click="confirmRetry"
            >
              {{ retrySubmitting ? '处理中...' : '确认重试' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 放弃弹窗 -->
    <Teleport to="body">
      <div
        v-if="showDiscardDialog && discardingItem"
        class="address-modal-mask"
        @click.self="closeDiscardDialog"
      >
        <div class="address-modal admin-failed-order-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">放弃失败订单</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="discardSubmitting"
              @click="closeDiscardDialog"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="discardErrorMsg" class="account-msg account-msg--error">{{ discardErrorMsg }}</p>

            <div class="admin-failed-order-dialog-preview">
              <div class="admin-failed-order-dialog-preview-head">
                <img
                  v-if="discardingItem.goodsImg"
                  :src="resolveFileUrl(discardingItem.goodsImg)"
                  alt=""
                  class="admin-failed-order-dialog-img"
                  referrerpolicy="no-referrer"
                />
                <span v-else class="admin-failed-order-dialog-img admin-failed-order-dialog-img--placeholder">
                  {{ (discardingItem.goodsName || '商').slice(0, 2) }}
                </span>
                <div>
                  <p class="admin-failed-order-dialog-name">{{ discardingItem.goodsName || '--' }}</p>
                  <p class="admin-failed-order-dialog-meta">
                    订单 {{ discardingItem.orderNo || discardingItem.id }}
                  </p>
                </div>
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label">放弃备注</label>
              <div class="account-form-field">
                <textarea
                  v-model="discardRemark"
                  class="admin-failed-order-textarea"
                  rows="3"
                  maxlength="200"
                  placeholder="选填：放弃原因"
                />
              </div>
            </div>

            <p class="admin-user-confirm-text" style="margin-top: 16px;">
              确认后该订单将被标记为<strong>已放弃</strong>，不再进行任何自动或手动重试。
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="discardSubmitting" @click="closeDiscardDialog">
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-danger"
              :disabled="discardSubmitting"
              @click="confirmDiscard"
            >
              {{ discardSubmitting ? '处理中...' : '确认放弃' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { useAdminFailedOrders } from '@/composables/useAdminFailedOrders'
import { resolveFileUrl } from '@/utils/file'

const {
  loading,
  errorMsg,
  toastMsg,
  records,
  pageNum,
  total,
  totalPages,
  pendingTotal,
  activeTab,
  autoRefreshing,
  lastRefreshedAt,
  showRetryConfirm,
  retryingItem,
  retrySubmitting,
  retryErrorMsg,
  showDiscardDialog,
  discardingItem,
  discardRemark,
  discardSubmitting,
  discardErrorMsg,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  emptyTitle,
  loadList,
  switchTab,
  goPrevPage,
  goNextPage,
  openRetryConfirm,
  closeRetryConfirm,
  confirmRetry,
  openDiscardDialog,
  closeDiscardDialog,
  confirmDiscard,
  formatDateTime,
  formatRefreshTime,
  formatFailedOrderHandleStatus,
  canHandleFailedOrder,
  FailedOrderHandleStatus,
} = useAdminFailedOrders()

function getHandleStatusClass(status?: number) {
  return {
    'audit-tag--pending': status === FailedOrderHandleStatus.PENDING,
    'audit-tag--approved': status === FailedOrderHandleStatus.RETRIED,
    'audit-tag--rejected': status === FailedOrderHandleStatus.DISCARDED,
  }
}

function getRetryClass(count?: number) {
  if (count == null || count === 0) return 'admin-failed-order-retry-badge--zero'
  if (count >= 5) return 'admin-failed-order-retry-badge--high'
  return 'admin-failed-order-retry-badge--mid'
}
</script>

<style scoped>
.admin-failed-order-panel {
  padding: 24px;
}

.admin-failed-order-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.admin-failed-order-hero-badge {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  margin-bottom: 10px;
  border-radius: 999px;
  background: #fff2f0;
  color: #cf1322;
  font-size: 12px;
  font-weight: 600;
}

.admin-failed-order-hero-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #333;
}

.admin-failed-order-hero-desc {
  margin: 0;
  max-width: 600px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.admin-failed-order-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  background: #fff;
  color: #555;
  font-size: 13px;
  cursor: pointer;
  flex-shrink: 0;
}

.admin-failed-order-refresh-btn:hover:not(:disabled) {
  border-color: #ffa39e;
  color: #cf1322;
}

.admin-failed-order-refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-failed-order-refresh-icon.is-spinning {
  animation: admin-failed-spin 0.8s linear infinite;
}

@keyframes admin-failed-spin {
  to {
    transform: rotate(360deg);
  }
}

/* 标签页 */
.admin-failed-order-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.admin-failed-order-tab {
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
  transition: all 0.18s;
}

.admin-failed-order-tab--active {
  border-color: transparent;
  background: var(--brand-gradient);
  color: #fff;
}

.admin-failed-order-tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  font-size: 11px;
  line-height: 18px;
}

/* 工具栏 */
.admin-failed-order-toolbar {
  margin-bottom: 12px;
}

.admin-failed-order-toolbar-tip {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.admin-failed-order-toolbar-live {
  color: #666;
}

.admin-failed-order-toolbar-sync {
  color: #bbb;
}

/* 加载与空状态 */
.admin-failed-order-loading,
.admin-failed-order-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 240px;
  text-align: center;
}

.admin-failed-order-empty-icon {
  font-size: 36px;
}

.admin-failed-order-empty-icon--warn {
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

.admin-failed-order-empty-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.admin-failed-order-empty-text {
  margin: 0;
  max-width: 420px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

/* 表格 */
.admin-failed-order-table-wrap {
  overflow-x: auto;
  border: 1px solid #eee;
  border-radius: 10px;
}

.admin-failed-order-table {
  width: 100%;
  min-width: 1020px;
  border-collapse: collapse;
  font-size: 12px;
}

.admin-failed-order-table th,
.admin-failed-order-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: middle;
}

.admin-failed-order-table th {
  background: #fafafa;
  color: #666;
  font-weight: 600;
  white-space: nowrap;
}

.admin-failed-order-table tbody tr:last-child td {
  border-bottom: none;
}

.admin-failed-order-table tbody tr:hover {
  background: #fafafa;
}

.admin-failed-order-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.admin-failed-order-img {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
}

.admin-failed-order-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #bbb;
  font-size: 12px;
}

.admin-failed-order-name {
  margin: 0 0 4px;
  font-weight: 600;
  color: #333;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-failed-order-meta {
  margin: 0;
  color: #999;
  line-height: 1.6;
  font-size: 11px;
}

.admin-failed-order-price {
  margin: 0 0 4px;
  color: var(--accent);
  font-weight: 700;
}

.admin-failed-order-error {
  margin: 0;
  max-width: 220px;
  color: #cf1322;
  font-size: 12px;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 重试次数徽标 */
.admin-failed-order-retry-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-weight: 600;
  white-space: nowrap;
}

.admin-failed-order-retry-badge--zero {
  background: #f5f5f5;
  color: #999;
}

.admin-failed-order-retry-badge--mid {
  background: #fff7e6;
  color: #d48806;
}

.admin-failed-order-retry-badge--high {
  background: #fff2f0;
  color: #cf1322;
}

/* 操作按钮 */
.admin-failed-order-btn {
  height: 30px;
  padding: 0 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s;
  margin-right: 6px;
  margin-bottom: 4px;
}

.admin-failed-order-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.admin-failed-order-btn--retry {
  border: 1px solid var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
}

.admin-failed-order-btn--retry:hover:not(:disabled) {
  background: var(--accent);
  color: #fff;
}

.admin-failed-order-btn--discard {
  border: 1px solid #ffa39e;
  background: #fff;
  color: #cf1322;
}

.admin-failed-order-btn--discard:hover:not(:disabled) {
  background: #fff2f0;
}

/* 弹窗 */
.admin-failed-order-dialog {
  width: min(520px, calc(100vw - 32px));
}

.admin-failed-order-dialog-preview {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fffaf7 0%, #ffffff 100%);
}

.admin-failed-order-dialog-preview-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.admin-failed-order-dialog-img {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid #eee;
}

.admin-failed-order-dialog-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #bbb;
  font-size: 14px;
}

.admin-failed-order-dialog-name {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.admin-failed-order-dialog-meta {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.admin-failed-order-dialog-detail {
  margin: 0;
  padding: 0;
  list-style: none;
}

.admin-failed-order-dialog-detail li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px dashed #eee;
  font-size: 12px;
  color: #888;
}

.admin-failed-order-dialog-detail strong {
  color: #444;
  font-weight: 600;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-failed-order-textarea {
  width: 100%;
  min-height: 88px;
  padding: 10px 12px;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  resize: vertical;
  font: inherit;
  font-size: 13px;
}

.admin-failed-order-textarea:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.12);
}

@media (max-width: 768px) {
  .admin-failed-order-hero {
    flex-direction: column;
  }

  .admin-failed-order-refresh-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
