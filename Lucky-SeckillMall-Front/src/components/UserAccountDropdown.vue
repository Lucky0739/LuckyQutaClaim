<template>
  <div ref="rootRef" class="user-account-dropdown">
    <button
      type="button"
      class="dropdown-trigger"
      :class="{ open: isOpen }"
      @click="toggle"
    >
      <span class="trigger-name">{{ displayNickname }}</span>
      <span class="dropdown-arrow"></span>
    </button>

    <Transition name="dropdown-fade">
      <div v-show="isOpen" class="dropdown-panel">
        <div class="user-card">
          <div class="avatar-wrap">
            <img
              :src="avatarSrc"
              alt=""
              class="user-avatar"
              referrerpolicy="no-referrer"
              @error="avatarLoadFailed = true"
            />
          </div>
          <div class="user-meta">
            <div class="user-name" :title="displayNickname">{{ displayNickname }}</div>
            <div class="user-actions">
              <router-link to="/account/profile" class="action-link" @click="close">
                账号管理
              </router-link>
              <span class="action-divider">|</span>
              <button type="button" class="action-link" @click="handleLogout">
                退出
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { logout } from '@/api/user'
import { clearAuth } from '@/utils/auth'
import { resolveFileUrl } from '@/utils/file'
import type { LoginUserInfo } from '@/types/user'
import defaultAvatar from '@/assets/morentouxiang.png'

const props = defineProps<{
  userInfo: LoginUserInfo
}>()

const emit = defineEmits<{
  logout: []
}>()

const router = useRouter()
const isOpen = ref(false)
const rootRef = ref<HTMLElement | null>(null)
const avatarLoadFailed = ref(false)

const displayNickname = computed(() => {
  const nickname = props.userInfo.nickname?.trim()
  return nickname || `用户${props.userInfo.id}`
})

const avatarUrl = computed(() => resolveFileUrl(props.userInfo.avatar))
const avatarSrc = computed(() => {
  if (avatarUrl.value && !avatarLoadFailed.value) return avatarUrl.value
  return defaultAvatar
})

watch(avatarUrl, () => {
  avatarLoadFailed.value = false
})

function toggle() {
  isOpen.value = !isOpen.value
}

function close() {
  isOpen.value = false
}

function handleClickOutside(event: MouseEvent) {
  if (!rootRef.value?.contains(event.target as Node)) {
    isOpen.value = false
  }
}

async function handleLogout() {
  close()
  try {
    await logout()
  } catch {
    // 本地仍清除登录态
  } finally {
    clearAuth()
    emit('logout')
    router.push('/login')
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.user-account-dropdown {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 12px;
  color: var(--text-normal);
  line-height: 32px;
  outline: none;
}

.dropdown-trigger:hover,
.dropdown-trigger.open {
  color: var(--taobao-orange);
}

.trigger-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: var(--taobao-orange);
}

.dropdown-arrow {
  width: 0;
  height: 0;
  margin-left: 2px;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid currentColor;
  transition: transform 0.2s;
  opacity: 0.7;
}

.dropdown-trigger.open .dropdown-arrow {
  transform: rotate(180deg);
}

.dropdown-panel {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  z-index: 200;
  width: 248px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 0 0 8px 8px;
  box-shadow:
    0 10px 28px rgba(0, 0, 0, 0.12),
    0 2px 8px rgba(0, 0, 0, 0.06);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-wrap {
  flex-shrink: 0;
}

.user-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  background: #f5f5f5;
  border: 1px solid #eeeeee;
}

.user-meta {
  min-width: 0;
  flex: 1;
}

.user-name {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-link {
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  color: #333;
  cursor: pointer;
  transition: color 0.2s;
}

.action-link:hover {
  color: var(--taobao-orange);
}

.action-divider {
  color: #dddddd;
  font-size: 12px;
  line-height: 1;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
