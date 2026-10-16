<template>
  <aside class="account-sidebar notify-sidebar">
    <div class="account-sidebar-head">
      <div class="notify-sidebar-head-row">
        <span>消息中心</span>
        <button
          v-if="totalUnread > 0"
          type="button"
          class="notify-sidebar-mark-all"
          :disabled="markingAll"
          @click="handleMarkAllCategoriesRead"
        >
          {{ markingAll ? '处理中...' : '全部已读' }}
        </button>
      </div>
      <span v-if="totalUnread > 0" class="notify-sidebar-total">
        {{ formatBadgeCount(totalUnread) }} 条未读
      </span>
    </div>
    <nav class="account-nav">
      <div class="nav-group">
        <h3 class="nav-group-title">消息分类</h3>
        <router-link
          v-for="item in visibleCategories"
          :key="item.routeName"
          :to="{ name: item.routeName }"
          class="nav-item notify-nav-item"
          active-class="nav-item--active"
        >
          <span class="notify-nav-label">{{ item.label }}</span>
          <span
            v-if="getUnreadByType(item.type) > 0"
            class="notify-nav-badge"
          >
            {{ formatBadgeCount(getUnreadByType(item.type)) }}
          </span>
        </router-link>
      </div>
    </nav>

    <Teleport to="body">
      <Transition name="address-toast-fade">
        <div v-if="toastMsg" class="address-success-toast">{{ toastMsg }}</div>
      </Transition>
    </Teleport>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { markAllAdminNotifyRead, markAllUserNotifyRead } from '@/api/notify'
import { useNotifyUnreadStats } from '@/composables/useNotifyUnreadStats'
import { getVisibleNotifyCategories } from '@/constants/notify'
import { dispatchNotifyChanged } from '@/utils/notifyEvent'
import { isAdmin } from '@/utils/auth'

const isAdminUser = computed(() => isAdmin())
const visibleCategories = computed(() => getVisibleNotifyCategories(isAdminUser.value))
const { totalUnread, getUnreadByType, formatBadgeCount, refreshUnreadStats } = useNotifyUnreadStats()
const markingAll = ref(false)
const toastMsg = ref('')

function showToast(message: string) {
  toastMsg.value = message
  window.setTimeout(() => {
    if (toastMsg.value === message) toastMsg.value = ''
  }, 2000)
}

async function handleMarkAllCategoriesRead() {
  if (markingAll.value || totalUnread.value <= 0) return
  markingAll.value = true
  try {
    const result = isAdminUser.value
      ? await markAllAdminNotifyRead()
      : await markAllUserNotifyRead()
    const count = result?.markedCount ?? 0
    showToast(count > 0 ? `已将 ${count} 条消息全部标记为已读` : '暂无未读消息')
    await refreshUnreadStats()
    dispatchNotifyChanged()
  } catch (err) {
    showToast(err instanceof Error ? err.message : '全部标记已读失败')
  } finally {
    markingAll.value = false
  }
}
</script>

<style scoped>
.notify-sidebar {
  flex: 0 0 180px;
}

.account-sidebar-head {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.notify-sidebar-head-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notify-sidebar-mark-all {
  flex-shrink: 0;
  height: 24px;
  padding: 0 8px;
  border: 1px solid var(--accent);
  border-radius: 4px;
  background: #fff;
  font-size: 11px;
  color: var(--accent);
  cursor: pointer;
}

.notify-sidebar-mark-all:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.notify-sidebar-mark-all:hover:not(:disabled) {
  background: #fff5eb;
}

.notify-sidebar-total {
  font-size: 12px;
  font-weight: 400;
  color: var(--accent);
}

.notify-nav-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notify-nav-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notify-nav-badge {
  flex-shrink: 0;
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
</style>
