<template>
  <section class="admin-activity-page">
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">秒杀活动管理</span>
    </div>

    <div class="account-panel admin-activity-panel">
      <header class="admin-activity-hero">
        <div class="admin-activity-hero-main">
          <div class="admin-activity-hero-badge">限时秒杀</div>
          <h2 class="admin-activity-hero-title">活动运营中心</h2>
          <p class="admin-activity-hero-desc">
            手动开始将预热 Redis 库存，结束活动后用户将无法继续参与秒杀。
          </p>
        </div>
        <button
          type="button"
          class="admin-activity-refresh-btn"
          :disabled="isBusy"
          @click="loadList"
        >
          <span class="admin-activity-refresh-icon" :class="{ 'is-spinning': loading }">↻</span>
          {{ loading ? '刷新中...' : '刷新列表' }}
        </button>
      </header>

      <div v-if="!loading && hasRecords" class="admin-activity-stats">
        <article class="admin-activity-stat-card">
          <span class="admin-activity-stat-label">全部活动</span>
          <strong class="admin-activity-stat-value">{{ stats.total }}</strong>
        </article>
        <article class="admin-activity-stat-card admin-activity-stat-card--pending">
          <span class="admin-activity-stat-label">未开始</span>
          <strong class="admin-activity-stat-value">{{ stats.notStarted }}</strong>
        </article>
        <article class="admin-activity-stat-card admin-activity-stat-card--active">
          <span class="admin-activity-stat-label">进行中</span>
          <strong class="admin-activity-stat-value">{{ stats.inProgress }}</strong>
        </article>
        <article class="admin-activity-stat-card admin-activity-stat-card--ended">
          <span class="admin-activity-stat-label">已结束</span>
          <strong class="admin-activity-stat-value">{{ stats.ended }}</strong>
        </article>
      </div>

      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div v-if="loading" class="admin-activity-loading">
        <span class="address-loading-dot"></span>
        加载秒杀活动列表...
      </div>

      <div v-else-if="errorMsg && !hasRecords" class="admin-activity-empty">
        <div class="admin-activity-empty-icon admin-activity-empty-icon--warn">!</div>
        <p class="admin-activity-empty-title">加载失败</p>
        <p class="admin-activity-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasRecords" class="admin-activity-empty">
        <div class="admin-activity-empty-icon">⚡</div>
        <p class="admin-activity-empty-title">暂无秒杀活动</p>
        <p class="admin-activity-empty-text">当前还没有可管理的秒杀活动，请稍后再来查看。</p>
      </div>

      <div v-else class="admin-activity-grid">
        <article
          v-for="activity in records"
          :key="activity.id"
          class="admin-activity-card"
          :class="getCardClass(activity.status)"
        >
          <div class="admin-activity-card-accent"></div>

          <div class="admin-activity-card-head">
            <div class="admin-activity-card-identity">
              <span class="admin-activity-card-avatar">{{ getActivityInitial(activity.activityName) }}</span>
              <div class="admin-activity-card-meta">
                <h3 class="admin-activity-card-name" :title="activity.activityName">
                  {{ activity.activityName || '--' }}
                </h3>
                <span class="admin-activity-card-id">ID {{ activity.id }}</span>
              </div>
            </div>
            <span
              class="admin-activity-status"
              :class="getStatusClass(activity.status)"
            >
              <span
                v-if="activity.status === SeckillActivityStatus.IN_PROGRESS"
                class="admin-activity-status-dot"
              ></span>
              {{ formatActivityStatus(activity.status) }}
            </span>
          </div>

          <div class="admin-activity-timeline">
            <div class="admin-activity-time-row">
              <span class="admin-activity-time-label">开始</span>
              <time class="admin-activity-time-value">{{ formatDateTime(activity.beginTime) }}</time>
            </div>
            <div class="admin-activity-time-track">
              <span
                class="admin-activity-time-progress"
                :style="{ width: getTimeProgress(activity) + '%' }"
              ></span>
            </div>
            <div class="admin-activity-time-row">
              <span class="admin-activity-time-label">结束</span>
              <time class="admin-activity-time-value">{{ formatDateTime(activity.endTime) }}</time>
            </div>
          </div>

          <p class="admin-activity-desc" :title="activity.description">
            {{ activity.description || '暂无活动描述' }}
          </p>

          <div class="admin-activity-card-footer">
            <template v-if="canStartActivity(activity) || canEndActivity(activity)">
              <button
                v-if="canStartActivity(activity)"
                type="button"
                class="admin-activity-btn admin-activity-btn--start"
                :disabled="actingId === activity.id"
                @click="openStartConfirm(activity)"
              >
                {{ actingId === activity.id && actingType === 'start' ? '启动中...' : '开始活动' }}
              </button>
              <button
                v-if="canEndActivity(activity)"
                type="button"
                class="admin-activity-btn admin-activity-btn--end"
                :disabled="actingId === activity.id"
                @click="openEndConfirm(activity)"
              >
                {{ actingId === activity.id && actingType === 'end' ? '结束中...' : '结束活动' }}
              </button>
            </template>
            <span v-else class="admin-activity-no-action">活动已归档，暂无可用操作</span>
          </div>
        </article>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="address-toast-fade">
        <div v-if="toastMsg" class="address-success-toast">{{ toastMsg }}</div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showActionConfirm && pendingActivity && pendingAction"
        class="address-modal-mask"
        @click.self="closeActionConfirm"
      >
        <div class="address-delete-dialog admin-activity-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">
              {{ pendingAction === 'start' ? '开始秒杀活动' : '结束秒杀活动' }}
            </h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="actingId !== null"
              @click="closeActionConfirm"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="actionErrorMsg" class="account-msg account-msg--error">{{ actionErrorMsg }}</p>

            <div class="admin-activity-dialog-preview">
              <div class="admin-activity-dialog-preview-head">
                <span class="admin-activity-card-avatar admin-activity-card-avatar--large">
                  {{ getActivityInitial(pendingActivity.activityName) }}
                </span>
                <div>
                  <p class="admin-activity-dialog-name">{{ pendingActivity.activityName }}</p>
                  <span
                    class="admin-activity-status"
                    :class="getStatusClass(pendingActivity.status)"
                  >
                    {{ formatActivityStatus(pendingActivity.status) }}
                  </span>
                </div>
              </div>
              <ul class="admin-activity-dialog-detail">
                <li>
                  <span>开始时间</span>
                  <strong>{{ formatDateTime(pendingActivity.beginTime) }}</strong>
                </li>
                <li>
                  <span>结束时间</span>
                  <strong>{{ formatDateTime(pendingActivity.endTime) }}</strong>
                </li>
              </ul>
            </div>

            <p class="admin-user-confirm-text">
              <template v-if="pendingAction === 'start'">
                确认后将预热 Redis 库存，并把活动状态更新为<strong>进行中</strong>。
              </template>
              <template v-else>
                确认结束后，用户将无法继续参与该场秒杀，请谨慎操作。
              </template>
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="actingId !== null" @click="closeActionConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn"
              :class="{
                'account-btn-primary': pendingAction === 'start',
                'account-btn-danger': pendingAction === 'end',
              }"
              :disabled="actingId !== null"
              @click="confirmAction"
            >
              {{ actingId !== null ? '处理中...' : pendingAction === 'start' ? '确认开始' : '确认结束' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { useAdminActivityList } from '@/composables/useAdminActivityList'
import type { SeckillActivityVO } from '@/types/activity'
import { SeckillActivityStatus } from '@/types/activity'

const {
  loading,
  errorMsg,
  toastMsg,
  records,
  actingId,
  actingType,
  showActionConfirm,
  pendingActivity,
  pendingAction,
  actionErrorMsg,
  hasRecords,
  isBusy,
  stats,
  loadList,
  openStartConfirm,
  openEndConfirm,
  closeActionConfirm,
  confirmAction,
  canStartActivity,
  canEndActivity,
  formatActivityStatus,
  formatDateTime,
} = useAdminActivityList()

function getActivityInitial(name?: string): string {
  const text = name?.trim()
  if (!text) return '秒'
  return text.slice(0, 1)
}

function getCardClass(status: SeckillActivityStatus): string {
  if (status === SeckillActivityStatus.IN_PROGRESS) return 'admin-activity-card--active'
  if (status === SeckillActivityStatus.ENDED) return 'admin-activity-card--ended'
  return 'admin-activity-card--pending'
}

function getStatusClass(status: SeckillActivityStatus): string {
  if (status === SeckillActivityStatus.IN_PROGRESS) return 'admin-activity-status--active'
  if (status === SeckillActivityStatus.ENDED) return 'admin-activity-status--ended'
  return 'admin-activity-status--pending'
}

function getTimeProgress(activity: SeckillActivityVO): number {
  if (!activity.beginTime || !activity.endTime) return 0

  const begin = new Date(activity.beginTime).getTime()
  const end = new Date(activity.endTime).getTime()
  if (Number.isNaN(begin) || Number.isNaN(end) || end <= begin) return 0

  const now = Date.now()
  if (now <= begin) return 0
  if (now >= end) return 100
  return Math.round(((now - begin) / (end - begin)) * 100)
}
</script>

<style scoped>
.admin-page-tab {
  cursor: default;
}

.admin-activity-panel {
  padding-top: 20px;
}

.admin-activity-hero {
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

.admin-activity-hero-badge {
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
  letter-spacing: 0.4px;
}

.admin-activity-hero-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
  color: #222;
}

.admin-activity-hero-desc {
  margin: 0;
  max-width: 560px;
  font-size: 13px;
  line-height: 1.7;
  color: #777;
}

.admin-activity-refresh-btn {
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
  transition: background 0.18s, border-color 0.18s, transform 0.18s;
}

.admin-activity-refresh-btn:hover:not(:disabled) {
  background: var(--accent-soft);
  border-color: #ffb88a;
}

.admin-activity-refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-activity-refresh-icon {
  display: inline-block;
  font-size: 16px;
  line-height: 1;
}

.admin-activity-refresh-icon.is-spinning {
  animation: admin-activity-spin 0.8s linear infinite;
}

@keyframes admin-activity-spin {
  to {
    transform: rotate(360deg);
  }
}

.admin-activity-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 22px;
}

.admin-activity-stat-card {
  padding: 16px 18px;
  border-radius: 12px;
  border: 1px solid #eee;
  background: #fff;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.admin-activity-stat-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: #888;
}

.admin-activity-stat-value {
  font-size: 28px;
  line-height: 1;
  color: #222;
}

.admin-activity-stat-card--pending {
  border-color: #ffe7ba;
  background: linear-gradient(180deg, #fffaf0 0%, #ffffff 100%);
}

.admin-activity-stat-card--pending .admin-activity-stat-value {
  color: #d46b08;
}

.admin-activity-stat-card--active {
  border-color: #ffd6bf;
  background: linear-gradient(180deg, #fff7f2 0%, #ffffff 100%);
}

.admin-activity-stat-card--active .admin-activity-stat-value {
  color: var(--accent);
}

.admin-activity-stat-card--ended {
  border-color: #ececec;
  background: linear-gradient(180deg, #fafafa 0%, #ffffff 100%);
}

.admin-activity-stat-card--ended .admin-activity-stat-value {
  color: #8c8c8c;
}

.admin-activity-loading,
.admin-activity-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 280px;
  padding: 32px 20px;
  border-radius: 14px;
  border: 1px dashed #e8e8e8;
  background: #fcfcfc;
  text-align: center;
}

.admin-activity-empty-icon {
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

.admin-activity-empty-icon--warn {
  background: #fff2f0;
  color: #f5222d;
}

.admin-activity-empty-title {
  margin: 4px 0 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.admin-activity-empty-text {
  margin: 0 0 8px;
  max-width: 360px;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.admin-activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
}

.admin-activity-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 18px 18px 16px;
  border-radius: 14px;
  border: 1px solid #ececec;
  background: #fff;
  box-shadow: 0 4px 18px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s;
}

.admin-activity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.08);
}

.admin-activity-card--pending {
  border-color: #ffe7ba;
}

.admin-activity-card--active {
  border-color: rgba(255, 80, 0, 0.22);
  box-shadow: 0 8px 24px rgba(255, 80, 0, 0.08);
}

.admin-activity-card--ended {
  opacity: 0.88;
  background: #fafafa;
}

.admin-activity-card-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: #d9d9d9;
}

.admin-activity-card--pending .admin-activity-card-accent {
  background: linear-gradient(180deg, #ffc069 0%, #fa8c16 100%);
}

.admin-activity-card--active .admin-activity-card-accent {
  background: var(--brand-gradient);
}

.admin-activity-card--ended .admin-activity-card-accent {
  background: #bfbfbf;
}

.admin-activity-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.admin-activity-card-identity {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.admin-activity-card-avatar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.admin-activity-card-avatar--large {
  width: 48px;
  height: 48px;
  font-size: 18px;
}

.admin-activity-card-meta {
  min-width: 0;
}

.admin-activity-card-name {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 700;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-activity-card-id {
  font-size: 11px;
  color: #999;
}

.admin-activity-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 0 10px;
  border-radius: 999px;
  line-height: 24px;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.admin-activity-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
  animation: admin-activity-pulse 1.4s ease-in-out infinite;
}

@keyframes admin-activity-pulse {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.45;
    transform: scale(0.85);
  }
}

.admin-activity-status--pending {
  color: #d46b08;
  background: #fff7e6;
  border: 1px solid #ffd591;
}

.admin-activity-status--active {
  color: var(--accent);
  background: #fff7f2;
  border: 1px solid #ffd6bf;
}

.admin-activity-status--ended {
  color: #8c8c8c;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
}

.admin-activity-timeline {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #fafafa;
}

.admin-activity-time-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.admin-activity-time-label {
  flex-shrink: 0;
  font-size: 11px;
  color: #999;
}

.admin-activity-time-value {
  font-size: 12px;
  color: #555;
  font-variant-numeric: tabular-nums;
}

.admin-activity-time-track {
  position: relative;
  height: 6px;
  border-radius: 999px;
  background: #ececec;
  overflow: hidden;
}

.admin-activity-time-progress {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--brand-gradient);
  transition: width 0.4s ease;
}

.admin-activity-card--ended .admin-activity-time-progress {
  background: #bfbfbf;
}

.admin-activity-card--pending .admin-activity-time-progress {
  background: linear-gradient(90deg, #ffc069 0%, #fa8c16 100%);
}

.admin-activity-desc {
  margin: 0;
  min-height: 40px;
  font-size: 13px;
  line-height: 1.7;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.admin-activity-card-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
  padding-top: 4px;
}

.admin-activity-btn {
  flex: 1;
  min-width: 120px;
  height: 36px;
  border: none;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s, opacity 0.18s;
}

.admin-activity-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.admin-activity-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.admin-activity-btn--start {
  color: #fff;
  background: var(--brand-gradient);
  box-shadow: 0 6px 16px rgba(255, 80, 0, 0.22);
}

.admin-activity-btn--end {
  color: #cf1322;
  background: #fff;
  border: 1px solid #ffa39e;
}

.admin-activity-btn--end:hover:not(:disabled) {
  background: #fff2f0;
}

.admin-activity-no-action {
  font-size: 12px;
  color: #aaa;
}

.admin-activity-dialog {
  max-width: 460px;
}

.admin-activity-dialog-preview {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  background: linear-gradient(180deg, #fffaf7 0%, #ffffff 100%);
}

.admin-activity-dialog-preview-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.admin-activity-dialog-name {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 700;
  color: #222;
}

.admin-activity-dialog-detail {
  margin: 0;
  padding: 0;
  list-style: none;
}

.admin-activity-dialog-detail li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px dashed #eee;
  font-size: 12px;
  color: #888;
}

.admin-activity-dialog-detail strong {
  color: #444;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.admin-user-confirm-text {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

.admin-user-confirm-text strong {
  color: var(--accent);
}

@media (max-width: 960px) {
  .admin-activity-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-activity-hero {
    flex-direction: column;
  }

  .admin-activity-refresh-btn {
    width: 100%;
    justify-content: center;
  }

  .admin-activity-grid {
    grid-template-columns: 1fr;
  }

  .admin-activity-card-footer {
    flex-direction: column;
  }

  .admin-activity-btn {
    width: 100%;
  }
}
</style>
