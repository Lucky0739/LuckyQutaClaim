<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">店铺管理</span>
    </div>

    <div class="account-panel admin-user-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <form class="admin-user-filter" @submit.prevent="handleSearch">
        <div class="admin-user-filter-grid">
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">申请人</label>
            <input
              v-model="filterUsername"
              type="text"
              maxlength="20"
              class="account-input admin-user-filter-input"
              placeholder="模糊搜索用户名"
            />
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">店铺名</label>
            <input
              v-model="filterShopName"
              type="text"
              maxlength="30"
              class="account-input admin-user-filter-input"
              placeholder="模糊搜索店铺名称"
            />
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">审核状态</label>
            <select v-model="filterAuditStatus" class="account-input admin-user-filter-input">
              <option value="">全部</option>
              <option :value="ShopAuditStatus.PENDING">待审核</option>
              <option :value="ShopAuditStatus.APPROVED">审核通过</option>
              <option :value="ShopAuditStatus.REJECTED">审核拒绝</option>
            </select>
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">店铺状态</label>
            <select v-model="filterShopStatus" class="account-input admin-user-filter-input">
              <option value="">全部</option>
              <option :value="ShopStatus.ENABLED">启用</option>
              <option :value="ShopStatus.DISABLED">禁用</option>
            </select>
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">开始时间</label>
            <input
              v-model="filterStartTime"
              type="datetime-local"
              class="account-input admin-user-filter-input admin-user-filter-input--datetime"
            />
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">结束时间</label>
            <input
              v-model="filterEndTime"
              type="datetime-local"
              class="account-input admin-user-filter-input admin-user-filter-input--datetime"
            />
          </div>
        </div>
        <div class="admin-user-filter-actions">
          <button type="submit" class="account-btn account-btn-primary" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button type="button" class="account-btn" :disabled="loading" @click="handleReset">
            重置
          </button>
        </div>
      </form>

      <div v-if="!loading && hasRecords" class="admin-user-toolbar">
        <p class="admin-user-toolbar-tip">共 {{ total }} 家店铺</p>
      </div>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载店铺列表...
      </div>

      <div v-else-if="errorMsg && !hasRecords" class="address-empty">
        <div class="address-empty-icon address-empty-icon--warn"></div>
        <p class="address-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasRecords" class="address-empty">
        <div class="address-empty-icon"></div>
        <p class="address-empty-text">暂无符合条件的店铺</p>
      </div>

      <div v-else class="admin-user-table-wrap">
        <table class="admin-user-table admin-shop-table">
          <thead>
            <tr>
              <th>店铺</th>
              <th>申请人</th>
              <th>简介</th>
              <th>审核状态</th>
              <th>店铺状态</th>
              <th>申请时间</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="shop in records" :key="shop.id">
              <td>
                <div class="admin-user-cell-user">
                  <img
                    v-if="shop.shopAvatar && !avatarErrors[shop.id]"
                    :src="resolveFileUrl(shop.shopAvatar)"
                    alt=""
                    class="admin-shop-avatar"
                    referrerpolicy="no-referrer"
                    @error="avatarErrors[shop.id] = true"
                  />
                  <span v-else class="admin-shop-avatar admin-shop-avatar--placeholder">
                    {{ shop.shopName?.charAt(0) || '店' }}
                  </span>
                  <div class="admin-user-meta">
                    <span class="admin-user-name">{{ shop.shopName || '--' }}</span>
                    <span class="admin-user-id">ID: {{ shop.id }}</span>
                  </div>
                </div>
              </td>
              <td>{{ shop.username || '--' }}</td>
              <td>
                <span class="admin-shop-desc" :title="shop.shopDesc">{{ shop.shopDesc || '--' }}</span>
              </td>
              <td>
                <span
                  class="admin-user-status-tag"
                  :class="{
                    'admin-shop-audit-tag--pending': shop.auditStatus === ShopAuditStatus.PENDING,
                    'admin-shop-audit-tag--approved': shop.auditStatus === ShopAuditStatus.APPROVED,
                    'admin-shop-audit-tag--rejected': shop.auditStatus === ShopAuditStatus.REJECTED,
                  }"
                >
                  {{ formatAuditStatus(shop.auditStatus, shop.auditStatusDesc) }}
                </span>
              </td>
              <td>
                <span
                  v-if="isShopApproved(shop)"
                  class="admin-user-status-tag"
                  :class="{
                    'admin-user-status-tag--enabled': isShopEnabled(shop.shopStatus),
                    'admin-user-status-tag--disabled': !isShopEnabled(shop.shopStatus),
                  }"
                >
                  {{ formatShopStatus(shop.shopStatus, shop.shopStatusDesc) }}
                </span>
                <span v-else class="admin-user-empty-text">--</span>
              </td>
              <td>{{ formatDateTime(shop.createTime) }}</td>
              <td>{{ formatDateTime(shop.updateTime) }}</td>
              <td>
                <div class="admin-user-actions">
                  <button
                    v-if="canAuditShop(shop)"
                    type="button"
                    class="admin-user-action"
                    @click="openAuditModal(shop)"
                  >
                    {{ isShopPending(shop) ? '审核' : '修改审核' }}
                  </button>
                  <span
                    v-if="canAuditShop(shop) && canUpdateShopStatus(shop)"
                    class="address-action-divider"
                  >
                    |
                  </span>
                  <button
                    v-if="canUpdateShopStatus(shop)"
                    type="button"
                    class="admin-user-action"
                    :class="{
                      'admin-user-action--danger': isShopEnabled(shop.shopStatus),
                      'admin-user-action--success': !isShopEnabled(shop.shopStatus),
                    }"
                    :disabled="togglingId === shop.id"
                    @click="openStatusConfirm(shop)"
                  >
                    {{
                      togglingId === shop.id
                        ? '处理中...'
                        : isShopEnabled(shop.shopStatus)
                          ? '禁用'
                          : '启用'
                    }}
                  </button>
                  <span v-if="!canAuditShop(shop) && !canUpdateShopStatus(shop)" class="admin-user-empty-text">
                    --
                  </span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="hasRecords && totalPages > 1" class="notify-pagination">
        <button
          type="button"
          class="account-btn"
          :disabled="!hasPrevPage || loading"
          @click="goPrevPage"
        >
          上一页
        </button>
        <span class="notify-pagination-info">
          第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条
        </span>
        <button
          type="button"
          class="account-btn"
          :disabled="!hasNextPage || loading"
          @click="goNextPage"
        >
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
      <div v-if="showAuditModal && auditingShop" class="address-modal-mask" @click.self="closeAuditModal">
        <div class="address-modal">
          <div class="address-modal-header">
            <h2 class="address-modal-title">
              {{ isShopPending(auditingShop) ? '审核店铺申请' : '修改审核状态' }}
            </h2>
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
            <div class="account-form-row">
              <label class="account-form-label">店铺名称</label>
              <div class="account-form-field">
                <input :value="auditingShop.shopName" type="text" class="account-input" readonly />
              </div>
            </div>
            <div class="account-form-row">
              <label class="account-form-label">申请人</label>
              <div class="account-form-field">
                <input :value="auditingShop.username || '--'" type="text" class="account-input" readonly />
              </div>
            </div>
            <div class="account-form-row">
              <label class="account-form-label">店铺简介</label>
              <div class="account-form-field">
                <textarea
                  :value="auditingShop.shopDesc || '暂无简介'"
                  class="admin-shop-textarea"
                  rows="3"
                  readonly
                />
              </div>
            </div>
            <div class="account-form-row">
              <label class="account-form-label">当前审核状态</label>
              <div class="account-form-field">
                <span
                  class="admin-user-status-tag"
                  :class="{
                    'admin-shop-audit-tag--pending': auditingShop.auditStatus === ShopAuditStatus.PENDING,
                    'admin-shop-audit-tag--approved': auditingShop.auditStatus === ShopAuditStatus.APPROVED,
                    'admin-shop-audit-tag--rejected': auditingShop.auditStatus === ShopAuditStatus.REJECTED,
                  }"
                >
                  {{ formatAuditStatus(auditingShop.auditStatus, auditingShop.auditStatusDesc) }}
                </span>
              </div>
            </div>
            <div class="account-form-row">
              <label class="account-form-label">审核备注</label>
              <div class="account-form-field">
                <textarea
                  v-model="auditRemark"
                  class="admin-shop-textarea"
                  rows="3"
                  maxlength="200"
                  placeholder="拒绝时必填，通过时可留空；修改审核时可更新备注"
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
              :disabled="auditSubmitting || !canRejectShopAudit(auditingShop)"
              :title="canRejectShopAudit(auditingShop) ? undefined : '审核通过后不允许改为拒绝'"
              @click="submitAudit(ShopAuditStatus.REJECTED)"
            >
              {{ auditSubmitting ? '处理中...' : '拒绝' }}
            </button>
            <button
              type="button"
              class="account-btn account-btn-primary"
              :disabled="auditSubmitting"
              @click="submitAudit(ShopAuditStatus.APPROVED)"
            >
              {{ auditSubmitting ? '处理中...' : '通过' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showStatusConfirm && pendingStatusShop"
        class="address-modal-mask"
        @click.self="closeStatusConfirm"
      >
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">
              {{ isShopEnabled(pendingStatusShop.shopStatus) ? '禁用店铺' : '启用店铺' }}
            </h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="!!togglingId"
              @click="closeStatusConfirm"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="statusModalErrorMsg" class="account-msg account-msg--error">{{ statusModalErrorMsg }}</p>
            <p class="admin-user-confirm-text">
              确定要{{ isShopEnabled(pendingStatusShop.shopStatus) ? '禁用' : '启用' }}店铺
              <strong>{{ pendingStatusShop.shopName }}</strong>
              吗？
            </p>
            <ul class="admin-shop-status-detail">
              <li>店铺 ID：{{ pendingStatusShop.id }}</li>
              <li>
                当前状态：
                <span
                  class="admin-user-status-tag"
                  :class="{
                    'admin-user-status-tag--enabled': isShopEnabled(pendingStatusShop.shopStatus),
                    'admin-user-status-tag--disabled': !isShopEnabled(pendingStatusShop.shopStatus),
                  }"
                >
                  {{ formatShopStatus(pendingStatusShop.shopStatus, pendingStatusShop.shopStatusDesc) }}
                </span>
              </li>
              <li>
                变更后：
                <span
                  class="admin-user-status-tag"
                  :class="{
                    'admin-user-status-tag--enabled': !isShopEnabled(pendingStatusShop.shopStatus),
                    'admin-user-status-tag--disabled': isShopEnabled(pendingStatusShop.shopStatus),
                  }"
                >
                  {{ formatShopStatus(getToggleShopStatus(pendingStatusShop.shopStatus)) }}
                </span>
              </li>
            </ul>
            <p class="admin-shop-status-tip">仅审核通过的店铺支持禁用/启用操作</p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="!!togglingId" @click="closeStatusConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn"
              :class="{
                'account-btn-danger': isShopEnabled(pendingStatusShop.shopStatus),
                'account-btn-primary': !isShopEnabled(pendingStatusShop.shopStatus),
              }"
              :disabled="!!togglingId"
              @click="confirmToggleStatus"
            >
              {{ togglingId ? '处理中...' : isShopEnabled(pendingStatusShop.shopStatus) ? '确认禁用' : '确认启用' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import {
  canAuditShop,
  canRejectShopAudit,
  canUpdateShopStatus,
  formatAuditStatus,
  formatShopStatus,
  getToggleShopStatus,
  isShopApproved,
  isShopEnabled,
  isShopPending,
  useAdminShopList,
} from '@/composables/useAdminShopList'
import { resolveFileUrl } from '@/utils/file'

const avatarErrors = reactive<Record<string, boolean>>({})

const {
  loading,
  errorMsg,
  toastMsg,
  records,
  pageNum,
  total,
  totalPages,
  filterUsername,
  filterShopName,
  filterAuditStatus,
  filterShopStatus,
  filterStartTime,
  filterEndTime,
  showAuditModal,
  auditingShop,
  auditRemark,
  auditSubmitting,
  auditErrorMsg,
  showStatusConfirm,
  pendingStatusShop,
  statusModalErrorMsg,
  togglingId,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  ShopAuditStatus,
  ShopStatus,
  formatDateTime,
  loadList,
  handleSearch,
  handleReset,
  goPrevPage,
  goNextPage,
  openAuditModal,
  closeAuditModal,
  submitAudit,
  openStatusConfirm,
  closeStatusConfirm,
  confirmToggleStatus,
} = useAdminShopList()
</script>

<style scoped>
.admin-page-tab {
  cursor: default;
}

.admin-user-panel {
  padding-top: 20px;
}

.admin-user-filter {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #e5e5e5;
}

.admin-user-filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px 24px;
}

.admin-user-filter-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.admin-user-filter-label {
  flex-shrink: 0;
  width: 64px;
  font-size: 12px;
  color: #666;
  text-align: right;
}

.admin-user-filter-input {
  flex: 1;
  max-width: none;
}

.admin-user-filter-input--datetime {
  min-width: 0;
}

.admin-user-filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-left: 76px;
}

.admin-user-toolbar {
  margin-bottom: 16px;
}

.admin-user-toolbar-tip {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.admin-user-table-wrap {
  overflow-x: auto;
  border: 1px solid #e5e5e5;
  background: #fff;
}

.admin-shop-table {
  min-width: 1080px;
}

.admin-user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.admin-user-table th,
.admin-user-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: middle;
}

.admin-user-table th {
  background: #fafafa;
  color: #666;
  font-weight: 600;
}

.admin-user-table tbody tr:hover {
  background: #fffaf7;
}

.admin-user-cell-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.admin-shop-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid #f0f0f0;
}

.admin-shop-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
}

.admin-user-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.admin-user-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.admin-user-id {
  font-size: 11px;
  color: #999;
}

.admin-shop-desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 180px;
  color: #666;
  line-height: 1.6;
}

.admin-user-status-tag {
  display: inline-block;
  padding: 0 8px;
  line-height: 20px;
}

.admin-shop-audit-tag--pending {
  border: 1px solid #ffd591;
  background: #fff7e6;
  color: #fa8c16;
}

.admin-shop-audit-tag--approved {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.admin-shop-audit-tag--rejected {
  border: 1px solid #ffccc7;
  background: #fff2f0;
  color: #f5222d;
}

.admin-user-status-tag--enabled {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.admin-user-status-tag--disabled {
  border: 1px solid #ffccc7;
  background: #fff2f0;
  color: #f5222d;
}

.admin-user-empty-text {
  color: #999;
}

.admin-user-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-user-action {
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  color: var(--accent);
  cursor: pointer;
}

.admin-user-action:hover:not(:disabled) {
  text-decoration: underline;
}

.admin-user-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-user-action--danger {
  color: #f5222d;
}

.admin-user-action--success {
  color: #52c41a;
}

.admin-user-confirm-text {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

.admin-user-confirm-text strong {
  color: #333;
}

.admin-shop-status-detail {
  margin: 12px 0 0;
  padding: 12px 16px;
  list-style: none;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  font-size: 12px;
  color: #666;
  line-height: 2;
}

.admin-shop-status-detail li {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.admin-shop-status-tip {
  margin: 12px 0 0;
  font-size: 12px;
  color: #999;
}

.admin-shop-textarea {
  width: 100%;
  max-width: 280px;
  padding: 8px 10px;
  border: 1px solid #ccc;
  font-size: 12px;
  color: #333;
  line-height: 1.6;
  resize: vertical;
  outline: none;
}

.admin-shop-textarea:focus {
  border-color: var(--accent);
}

@media (max-width: 960px) {
  .admin-user-filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-user-filter-grid {
    grid-template-columns: 1fr;
  }

  .admin-user-filter-actions {
    padding-left: 0;
  }
}
</style>
