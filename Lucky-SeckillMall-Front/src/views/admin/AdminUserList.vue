<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">用户管理</span>
    </div>

    <div class="account-panel admin-user-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <form class="admin-user-filter" @submit.prevent="handleSearch">
        <div class="admin-user-filter-grid">
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">昵称</label>
            <input
              v-model="filterNickname"
              type="text"
              maxlength="20"
              class="account-input admin-user-filter-input"
              placeholder="模糊搜索昵称"
            />
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">手机号</label>
            <input
              v-model="filterMobile"
              type="text"
              maxlength="11"
              class="account-input admin-user-filter-input"
              placeholder="模糊搜索手机号"
            />
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">状态</label>
            <select v-model="filterStatus" class="account-input admin-user-filter-input">
              <option value="">全部</option>
              <option :value="UserStatus.ENABLED">启用</option>
              <option :value="UserStatus.DISABLED">禁用</option>
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
        <p class="admin-user-toolbar-tip">共 {{ total }} 位用户</p>
      </div>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载用户列表...
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
        <p class="address-empty-text">暂无符合条件的用户</p>
      </div>

      <div v-else class="admin-user-table-wrap">
        <table class="admin-user-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>手机号</th>
              <th>角色</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in records" :key="user.id">
              <td>
                <div class="admin-user-cell-user">
                  <img
                    v-if="user.avatar && !avatarErrors[user.id]"
                    :src="user.avatar"
                    alt=""
                    class="admin-user-avatar"
                    referrerpolicy="no-referrer"
                    @error="avatarErrors[user.id] = true"
                  />
                  <span v-else class="admin-user-avatar admin-user-avatar--placeholder">
                    {{ user.nickname?.charAt(0) || 'U' }}
                  </span>
                  <div class="admin-user-meta">
                    <span class="admin-user-name">{{ user.nickname || '--' }}</span>
                    <span class="admin-user-id">ID: {{ user.id }}</span>
                  </div>
                </div>
              </td>
              <td>{{ user.mobile || '--' }}</td>
              <td>
                <div v-if="user.roleNames?.length" class="admin-user-roles">
                  <span
                    v-for="role in user.roleNames"
                    :key="role"
                    class="admin-user-role-tag"
                    :class="{ 'admin-user-role-tag--admin': role === 'admin' }"
                  >
                    {{ formatRoleName(role) }}
                  </span>
                </div>
                <span v-else class="admin-user-empty-text">--</span>
              </td>
              <td>
                <span
                  class="admin-user-status-tag"
                  :class="{
                    'admin-user-status-tag--enabled': isUserEnabled(user.status),
                    'admin-user-status-tag--disabled': !isUserEnabled(user.status),
                  }"
                >
                  {{ user.statusDesc || (isUserEnabled(user.status) ? '启用' : '禁用') }}
                </span>
              </td>
              <td>{{ formatDateTime(user.createTime) }}</td>
              <td>{{ formatDateTime(user.updateTime) }}</td>
              <td>
                <div class="admin-user-actions">
                  <button type="button" class="admin-user-action" @click="openEditModal(user)">
                    编辑昵称
                  </button>
                  <span v-if="!isTargetAdminUser(user)" class="address-action-divider">|</span>
                  <button
                    v-if="!isTargetAdminUser(user)"
                    type="button"
                    class="admin-user-action"
                    :class="{
                      'admin-user-action--danger': isUserEnabled(user.status),
                      'admin-user-action--success': !isUserEnabled(user.status),
                    }"
                    :disabled="togglingId === user.id"
                    @click="openStatusConfirm(user)"
                  >
                    {{
                      togglingId === user.id
                        ? '处理中...'
                        : isUserEnabled(user.status)
                          ? '禁用'
                          : '启用'
                    }}
                  </button>
                  <span v-if="!isTargetAdminUser(user)" class="address-action-divider">|</span>
                  <button
                    v-if="!isTargetAdminUser(user)"
                    type="button"
                    class="admin-user-action admin-user-action--danger"
                    :disabled="!canDeleteUser(user) || deletingId === user.id"
                    :title="getDeleteDisabledReason(user) || undefined"
                    @click="openDeleteConfirm(user)"
                  >
                    {{ deletingId === user.id ? '删除中...' : '删除' }}
                  </button>
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
      <div v-if="showEditModal && editingUser" class="address-modal-mask" @click.self="closeEditModal">
        <form class="address-modal" @submit.prevent="submitEdit">
          <div class="address-modal-header">
            <h2 class="address-modal-title">编辑用户昵称</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="editSubmitting"
              @click="closeEditModal"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="editErrorMsg" class="account-msg account-msg--error">{{ editErrorMsg }}</p>
            <div class="account-form-row">
              <label class="account-form-label">用户 ID</label>
              <div class="account-form-field">
                <input :value="editingUser.id" type="text" class="account-input" readonly />
              </div>
            </div>
            <div class="account-form-row">
              <label class="account-form-label">手机号</label>
              <div class="account-form-field">
                <input :value="editingUser.mobile" type="text" class="account-input" readonly />
              </div>
            </div>
            <div class="account-form-row">
              <label class="account-form-label">昵称</label>
              <div class="account-form-field">
                <input
                  v-model="editNickname"
                  type="text"
                  maxlength="20"
                  class="account-input"
                  placeholder="请输入昵称"
                />
              </div>
            </div>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="editSubmitting" @click="closeEditModal">
              取消
            </button>
            <button type="submit" class="account-btn account-btn-primary" :disabled="editSubmitting">
              {{ editSubmitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showStatusConfirm && pendingStatusUser"
        class="address-modal-mask"
        @click.self="closeStatusConfirm"
      >
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">
              {{ isUserEnabled(pendingStatusUser.status) ? '禁用用户' : '启用用户' }}
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
            <p class="admin-user-confirm-text">
              确定要{{ isUserEnabled(pendingStatusUser.status) ? '禁用' : '启用' }}用户
              <strong>{{ pendingStatusUser.nickname }}</strong>
              （{{ pendingStatusUser.mobile }}）吗？
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="!!togglingId" @click="closeStatusConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn"
              :class="{
                'account-btn-danger': isUserEnabled(pendingStatusUser.status),
                'account-btn-primary': !isUserEnabled(pendingStatusUser.status),
              }"
              :disabled="!!togglingId"
              @click="confirmToggleStatus"
            >
              {{ togglingId ? '处理中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showDeleteConfirm && pendingDeleteUser"
        class="address-modal-mask"
        @click.self="closeDeleteConfirm"
      >
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">删除用户</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="!!deletingId"
              @click="closeDeleteConfirm"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body address-delete-body">
            <p class="address-delete-tip">
              确定要删除用户
              <strong>{{ pendingDeleteUser.nickname }}</strong>
              （{{ pendingDeleteUser.mobile }}）吗？此操作为逻辑删除，删除后该用户将无法登录。
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="!!deletingId" @click="closeDeleteConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-danger"
              :disabled="!!deletingId"
              @click="confirmDeleteUser"
            >
              {{ deletingId ? '删除中...' : '确认删除' }}
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
  canDeleteUser,
  formatDateTime,
  formatRoleName,
  getDeleteDisabledReason,
  isTargetAdminUser,
  isUserEnabled,
  useAdminUserList,
} from '@/composables/useAdminUserList'

const avatarErrors = reactive<Record<string, boolean>>({})

const {
  loading,
  errorMsg,
  toastMsg,
  records,
  pageNum,
  total,
  totalPages,
  filterNickname,
  filterMobile,
  filterStatus,
  filterStartTime,
  filterEndTime,
  showEditModal,
  editingUser,
  editNickname,
  editSubmitting,
  editErrorMsg,
  showStatusConfirm,
  pendingStatusUser,
  togglingId,
  showDeleteConfirm,
  pendingDeleteUser,
  deletingId,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  UserStatus,
  loadList,
  handleSearch,
  handleReset,
  goPrevPage,
  goNextPage,
  openEditModal,
  closeEditModal,
  submitEdit,
  openStatusConfirm,
  closeStatusConfirm,
  confirmToggleStatus,
  openDeleteConfirm,
  closeDeleteConfirm,
  confirmDeleteUser,
} = useAdminUserList()
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
  border-radius: 10px;
  background: #fff;
}

.admin-user-table {
  width: 100%;
  min-width: 1080px;
  border-collapse: collapse;
  font-size: 12px;
  table-layout: fixed;
}

.admin-user-table th,
.admin-user-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: middle;
}

.admin-user-table th:nth-child(1),
.admin-user-table td:nth-child(1) {
  width: 210px;
}

.admin-user-table th:nth-child(2),
.admin-user-table td:nth-child(2) {
  width: 130px;
}

.admin-user-table th:nth-child(3),
.admin-user-table td:nth-child(3) {
  width: 160px;
}

.admin-user-table th:nth-child(4),
.admin-user-table td:nth-child(4) {
  width: 90px;
}

.admin-user-table th:nth-child(5),
.admin-user-table td:nth-child(5),
.admin-user-table th:nth-child(6),
.admin-user-table td:nth-child(6) {
  width: 160px;
  white-space: nowrap;
}

.admin-user-table th:nth-child(7),
.admin-user-table td:nth-child(7) {
  width: 170px;
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

.admin-user-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #f0f0f0;
}

.admin-user-avatar--placeholder {
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

.admin-user-roles {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.admin-user-role-tag {
  display: inline-flex;
  align-items: center;
  flex-shrink: 0;
  white-space: nowrap;
  padding: 0 8px;
  border: 1px solid #d9d9d9;
  border-radius: 999px;
  background: #fafafa;
  color: #666;
  line-height: 20px;
}

.admin-user-role-tag--admin {
  border-color: #ffd6bf;
  background: #fff5eb;
  color: var(--accent);
}

.admin-user-status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 0 8px;
  border-radius: 999px;
  line-height: 20px;
  white-space: nowrap;
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
  flex-wrap: nowrap;
  gap: 6px;
  white-space: nowrap;
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
