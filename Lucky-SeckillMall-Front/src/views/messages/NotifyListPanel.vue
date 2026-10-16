<template>
  <section>
    <div class="account-tabs notify-type-tabs">
      <router-link
        v-for="item in visibleCategories"
        :key="item.routeName"
        :to="{ name: item.routeName }"
        class="account-tab notify-type-tab"
        active-class="active"
      >
        {{ item.label }}
        <span
          v-if="getTabUnreadCount(item.type) > 0"
          class="notify-tab-badge"
        >
          {{ formatBadgeCount(getTabUnreadCount(item.type)) }}
        </span>
      </router-link>
    </div>

    <div class="account-panel notify-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div v-if="isUserMode" class="notify-filter-bar">
        <button
          v-for="item in readFilterOptions"
          :key="item.value"
          type="button"
          class="notify-filter-btn"
          :class="{ 'notify-filter-btn--active': readFilter === item.value }"
          @click="setReadFilter(item.value)"
        >
          {{ item.label }}
        </button>
      </div>

      <div v-if="hasRecords || loading" class="notify-toolbar">
        <p class="notify-toolbar-tip">
          共 {{ total }} 条消息
          <template v-if="isUserMode">
            ，未读 {{ typeUnreadCount }} 条
          </template>
          <template v-else>
            ，本类未读 {{ tabUnreadCount }} 条
            <template v-if="pageUnreadCount > 0 && pageUnreadCount !== tabUnreadCount">
              （当前页 {{ pageUnreadCount }} 条）
            </template>
          </template>
        </p>
        <button
          v-if="hasUnread"
          type="button"
          class="notify-mark-all-btn"
          :disabled="markingAll || loading"
          @click="() => markAllAsRead()"
        >
          {{ markingAll ? '处理中...' : '本类全部已读' }}
        </button>
      </div>

      <div v-if="loading" class="notify-loading">
        <span class="address-loading-dot"></span>
        加载消息列表...
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
        <p class="address-empty-text">暂无{{ pageTitle }}消息</p>
      </div>

      <div v-else class="notify-list">
        <article
          v-for="item in records"
          :key="item.id"
          class="notify-item"
          :class="{
            'notify-item--unread': isUnreadNotify(item),
            'notify-item--read': !isUnreadNotify(item),
            'notify-item--clickable': isUserMode,
          }"
          @click="isUserMode ? openDetail(item) : undefined"
        >
          <div class="notify-item-main">
            <div class="notify-item-head">
              <h3 class="notify-item-title">{{ item.title || item.notifyTypeName || pageTitle }}</h3>
              <span v-if="isUnreadNotify(item)" class="notify-status-tag notify-status-tag--unread">
                未读
              </span>
              <span v-else class="notify-status-tag notify-status-tag--read">已读</span>
            </div>
            <p class="notify-item-content">{{ item.content }}</p>
            <p class="notify-item-time">
              发送时间：{{ formatNotifyTime(item.createTime) }}
              <template v-if="!isUnreadNotify(item) && item.readTime">
                · 已读于 {{ formatNotifyTime(item.readTime) }}
              </template>
            </p>
          </div>

          <div
            v-if="showGoodsAuditLink(item) || isUnreadNotify(item)"
            class="notify-item-actions"
            @click.stop
          >
            <RouterLink
              v-if="showGoodsAuditLink(item)"
              :to="{ name: 'AdminGoods', query: { goodsId: String(item.businessId) } }"
              class="notify-read-btn notify-read-btn--audit"
            >
              去审核
            </RouterLink>
            <button
              v-if="isUnreadNotify(item)"
              type="button"
              class="notify-read-btn"
              :disabled="markingId === item.id || markingAll"
              @click="markAsRead(item)"
            >
              {{ markingId === item.id ? '处理中...' : '标记已读' }}
            </button>
          </div>
        </article>
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
      <Transition name="notify-detail-fade">
        <div v-if="showDetail" class="notify-detail-mask" @click.self="closeDetail">
          <div class="notify-detail-dialog" role="dialog" aria-modal="true">
            <header class="notify-detail-head">
              <h3 class="notify-detail-title">
                {{ detailItem?.title || detailItem?.notifyTypeName || pageTitle }}
              </h3>
              <button type="button" class="notify-detail-close" aria-label="关闭" @click="closeDetail">
                ×
              </button>
            </header>
            <div v-if="detailLoading" class="notify-detail-loading">加载详情...</div>
            <div v-else-if="detailItem" class="notify-detail-body">
              <p class="notify-detail-meta">
                类型：{{ detailItem.notifyTypeName || pageTitle }}
                · {{ formatNotifyTime(detailItem.createTime) }}
                ·
                <span
                  :class="isUnreadNotify(detailItem) ? 'notify-detail-status--unread' : 'notify-detail-status--read'"
                >
                  {{ isUnreadNotify(detailItem) ? '未读' : '已读' }}
                </span>
              </p>
              <p class="notify-detail-content">{{ detailItem.content }}</p>
              <p v-if="detailItem.readTime" class="notify-detail-read-time">
                已读于 {{ formatNotifyTime(detailItem.readTime) }}
              </p>
            </div>
            <footer
              v-if="detailItem && !detailLoading && isUnreadNotify(detailItem)"
              class="notify-detail-footer"
            >
              <button
                type="button"
                class="notify-read-btn notify-read-btn--block"
                :disabled="markingId === detailItem.id"
                @click="markAsRead(detailItem)"
              >
                {{ markingId === detailItem.id ? '处理中...' : '标记为已读' }}
              </button>
            </footer>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getVisibleNotifyCategories, NOTIFY_TYPE } from '@/constants/notify'
import type { Notify } from '@/types/notify'
import type { NotifyReadFilter } from '@/types/notify'
import { isAdmin } from '@/utils/auth'
import { formatNotifyBadgeCount as formatBadgeCount } from '@/composables/useNotifyUnreadStats'
import { formatNotifyTime, useNotifyList } from '@/composables/useNotifyList'

const route = useRoute()

const visibleCategories = computed(() => getVisibleNotifyCategories(isAdmin()))
const notifyType = computed(() => route.meta.notifyType as number)
const pageTitle = computed(() => (route.meta.title as string) || '消息')
const isGoodsListingApplyPage = computed(
  () => notifyType.value === NOTIFY_TYPE.GOODS_LISTING_APPLY,
)

function showGoodsAuditLink(item: Notify) {
  return !isUserMode.value && isGoodsListingApplyPage.value && !!item.businessId
}

const readFilterOptions: { value: NotifyReadFilter; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
  { value: 'read', label: '已读' },
]

const {
  loading,
  errorMsg,
  toastMsg,
  records,
  pageNum,
  total,
  totalPages,
  typeUnreadCount,
  readFilter,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  pageUnreadCount,
  tabUnreadCount,
  hasUnread,
  isUserMode,
  markingId,
  markingAll,
  detailLoading,
  detailItem,
  showDetail,
  loadList,
  setReadFilter,
  goPrevPage,
  goNextPage,
  markAsRead,
  markAllAsRead,
  openDetail,
  closeDetail,
  getTabUnreadCount,
  isUnreadNotify: checkUnread,
} = useNotifyList(() => notifyType.value)

const isUnreadNotify = checkUnread
</script>

<style scoped>
.notify-type-tabs {
  flex-wrap: wrap;
  padding-right: 12px;
}

.notify-type-tab {
  text-decoration: none;
  white-space: nowrap;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.notify-tab-badge {
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: var(--accent);
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
}

.notify-filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.notify-filter-btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid #e5e5e5;
  background: #fff;
  font-size: 12px;
  color: #666;
  cursor: pointer;
}

.notify-filter-btn--active {
  border-color: var(--accent);
  color: var(--accent);
  background: #fffaf7;
}

.notify-panel {
  padding-top: 20px;
}

.notify-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #e5e5e5;
}

.notify-toolbar-tip {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.notify-mark-all-btn {
  height: 32px;
  padding: 0 16px;
  border: 1px solid var(--accent);
  background: #fff;
  font-size: 12px;
  color: var(--accent);
  cursor: pointer;
  transition: all 0.2s;
}

.notify-mark-all-btn:hover:not(:disabled) {
  background: #fff5eb;
}

.notify-mark-all-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.notify-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 80px 0;
  font-size: 12px;
  color: #999;
}

.notify-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notify-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 20px;
  border: 1px solid #e5e5e5;
  background: #fff;
  transition: border-color 0.2s, box-shadow 0.2s, background 0.2s;
}

.notify-item--clickable {
  cursor: pointer;
}

.notify-item--clickable:hover {
  border-color: #ffd6bf;
  box-shadow: 0 2px 8px rgba(255, 102, 0, 0.08);
}

.notify-item--unread {
  border-color: #ffd6bf;
  background: #fffaf7;
}

.notify-item--unread::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent);
}

.notify-item--read {
  background: #fafafa;
}

.notify-item-main {
  flex: 1;
  min-width: 0;
}

.notify-item-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.notify-item-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.notify-status-tag {
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
}

.notify-status-tag--unread {
  border: 1px solid var(--accent);
  background: #fff;
  color: var(--accent);
}

.notify-status-tag--read {
  border: 1px solid #d9d9d9;
  background: #fff;
  color: #999;
}

.notify-item-content {
  margin: 0 0 8px;
  font-size: 12px;
  color: #666;
  line-height: 1.8;
  word-break: break-word;
}

.notify-item-time {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.notify-item-actions {
  flex-shrink: 0;
  padding-top: 2px;
}

.notify-read-btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid var(--accent);
  background: #fff;
  font-size: 12px;
  color: var(--accent);
  cursor: pointer;
  transition: all 0.2s;
}

.notify-read-btn:hover:not(:disabled) {
  background: #fff5eb;
}

.notify-read-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.notify-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed #e5e5e5;
}

.notify-pagination-info {
  font-size: 12px;
  color: #999;
}

.notify-detail-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.45);
}

.notify-detail-dialog {
  width: min(520px, 100%);
  max-height: min(70vh, 560px);
  overflow: auto;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.notify-detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.notify-detail-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.notify-detail-close {
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  font-size: 22px;
  line-height: 1;
  color: #999;
  cursor: pointer;
}

.notify-detail-loading {
  padding: 40px 20px;
  text-align: center;
  font-size: 12px;
  color: #999;
}

.notify-detail-body {
  padding: 16px 20px 20px;
}

.notify-detail-meta {
  margin: 0 0 12px;
  font-size: 12px;
  color: #999;
}

.notify-detail-content {
  margin: 0;
  font-size: 14px;
  color: #333;
  line-height: 1.8;
  word-break: break-word;
}

.notify-detail-read-time {
  margin: 12px 0 0;
  font-size: 12px;
  color: #999;
}

.notify-detail-status--unread {
  color: var(--accent);
}

.notify-detail-status--read {
  color: #999;
}

.notify-detail-footer {
  padding: 12px 20px 20px;
  border-top: 1px solid #f0f0f0;
}

.notify-read-btn--block {
  width: 100%;
  height: 36px;
}

.notify-read-btn--audit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
  border-radius: 4px;
  background: var(--brand-gradient, linear-gradient(135deg, #ff8a00, #ff5000));
  border-color: transparent;
  color: #fff;
  font-weight: 600;
}

.notify-read-btn--audit:hover {
  background: var(--brand-gradient, linear-gradient(135deg, #ff8a00, #ff5000));
  color: #fff;
  opacity: 0.92;
}

.notify-detail-fade-enter-active,
.notify-detail-fade-leave-active {
  transition: opacity 0.2s ease;
}

.notify-detail-fade-enter-from,
.notify-detail-fade-leave-to {
  opacity: 0;
}
</style>
