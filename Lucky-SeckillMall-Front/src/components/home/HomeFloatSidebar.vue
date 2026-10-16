<template>
  <Teleport to="body">
    <!-- 右侧固定工具栏 -->
    <aside class="float-toolbar" aria-label="快捷工具">
      <button
        type="button"
        class="float-toolbar-item"
        :class="{ 'float-toolbar-item--active': activePanel === 'service' }"
        @click="togglePanel('service')"
      >
        <span class="float-toolbar-icon float-toolbar-icon--service" aria-hidden="true"></span>
        <span class="float-toolbar-label">客服</span>
      </button>

      <button
        type="button"
        class="float-toolbar-item"
        :class="{ 'float-toolbar-item--active': activePanel === 'message' }"
        @click="togglePanel('message')"
      >
        <span class="float-toolbar-icon float-toolbar-icon--message" aria-hidden="true">
          <span v-if="messageBadge" class="float-toolbar-badge">{{ messageBadge }}</span>
        </span>
        <span class="float-toolbar-label">消息</span>
      </button>

      <span class="float-toolbar-divider"></span>

      <button
        type="button"
        class="float-toolbar-item"
        :class="{ 'float-toolbar-item--muted': !showBackTop }"
        @click="scrollToTop"
      >
        <span class="float-toolbar-icon float-toolbar-icon--top" aria-hidden="true"></span>
        <span class="float-toolbar-label float-toolbar-label--stack">
          <span>返回</span>
          <span>顶部</span>
        </span>
      </button>
    </aside>

    <!-- 遮罩 -->
    <Transition name="float-mask-fade">
      <div
        v-if="activePanel"
        class="float-panel-mask"
        @click="closePanel"
      ></div>
    </Transition>

    <!-- 客服面板 -->
    <Transition name="float-panel-slide">
      <aside
        v-if="activePanel === 'service'"
        class="float-panel float-panel--service"
        role="dialog"
        aria-labelledby="service-panel-title"
      >
        <header class="service-header">
          <div class="service-header-main">
            <div class="service-header-avatar" aria-hidden="true">
              <span class="service-header-avatar-icon">蜜</span>
            </div>
            <div class="service-header-info">
              <div class="service-header-name-row">
                <h2 id="service-panel-title" class="service-header-name">智能客服淘小蜜</h2>
                <span class="service-header-status">
                  <span class="service-header-status-dot"></span>
                  在线
                </span>
              </div>
              <p class="service-header-desc">秒杀商城官方客服 · 09:00-22:00 为您服务</p>
            </div>
          </div>
          <button type="button" class="service-header-close" aria-label="关闭" @click="closePanel">×</button>
        </header>

        <div ref="chatBodyRef" class="service-chat-body">
          <div class="service-welcome-card">
            <p class="service-welcome-title">Hi，有什么可以帮您？</p>
            <p class="service-welcome-desc">商品咨询、订单查询、开店指引，随时为您解答</p>
          </div>

          <div
            v-for="msg in chatMessages"
            :key="msg.id"
            class="service-chat-item"
            :class="msg.role === 'user' ? 'service-chat-item--user' : 'service-chat-item--bot'"
          >
            <span
              v-if="msg.role === 'bot'"
              class="service-chat-avatar service-chat-avatar--bot"
              aria-hidden="true"
            >
              <span class="service-chat-avatar-icon">蜜</span>
            </span>
            <div class="service-chat-content">
              <div
                class="service-chat-bubble"
                :class="{ 'service-chat-bubble--streaming': msg.streaming && msg.text }"
              >
                <template v-if="msg.text">{{ msg.text }}</template>
                <span v-else-if="msg.streaming" class="service-chat-typing">
                  <span></span><span></span><span></span>
                </span>
              </div>
            </div>
            <span
              v-if="msg.role === 'user'"
              class="service-chat-avatar service-chat-avatar--user"
              aria-hidden="true"
            >
              <img
                v-if="userAvatarSrc"
                :src="userAvatarSrc"
                alt=""
                class="service-chat-avatar-img"
                referrerpolicy="no-referrer"
                @error="userAvatarLoadFailed = true"
              />
              <span v-else>我</span>
            </span>
          </div>
        </div>

        <div class="service-quick-section">
          <p class="service-quick-title">猜你想问</p>
          <div class="service-quick-tags">
            <button
              v-for="item in quickQuestions"
              :key="item"
              type="button"
              class="service-quick-tag"
              :disabled="chatLoading"
              @click="sendQuickQuestion(item)"
            >
              {{ item }}
            </button>
          </div>
        </div>

        <form class="service-footer" @submit.prevent="sendChatMessage">
          <div class="service-input-wrap">
            <input
              v-model="chatInput"
              type="text"
              maxlength="200"
              class="service-input"
              placeholder="描述您的问题，我来帮您..."
              :disabled="chatLoading"
            />
            <button
              type="submit"
              class="service-send-btn"
              :disabled="chatLoading || !chatInput.trim()"
            >
              发送
            </button>
          </div>
        </form>
      </aside>
    </Transition>

    <!-- 消息面板 -->
    <Transition name="float-panel-slide">
      <aside
        v-if="activePanel === 'message'"
        class="float-panel float-panel--message"
        role="dialog"
        aria-labelledby="message-panel-title"
      >
        <header class="float-panel-header">
          <div>
            <h2 id="message-panel-title" class="float-panel-title">
              我的消息
              <span v-if="isLoggedIn && totalUnread > 0" class="float-panel-unread">
                {{ formatBadgeCount(totalUnread) }} 条未读
              </span>
            </h2>
            <p class="float-panel-subtitle">
              {{ messagePanelSubtitle }}
            </p>
          </div>
          <button type="button" class="float-panel-close" aria-label="关闭" @click="closePanel">×</button>
        </header>

        <div class="message-panel-body">
          <div v-if="!isLoggedIn" class="message-panel-empty">
            <span class="message-panel-empty-icon">🔔</span>
            <p>登录后即可查看您的消息</p>
            <router-link to="/login" class="float-panel-btn" @click="closePanel">去登录</router-link>
          </div>

          <div v-else-if="messageLoading" class="message-panel-loading">
            <span class="message-panel-loading-dot"></span>
            加载消息中...
          </div>

          <div v-else-if="messageError" class="message-panel-empty message-panel-empty--error">
            <p>{{ messageError }}</p>
            <button type="button" class="float-panel-btn float-panel-btn--ghost" @click="loadMessages">
              重新加载
            </button>
          </div>

          <div v-else-if="!messagePreview.length" class="message-panel-empty">
            <span class="message-panel-empty-icon">📭</span>
            <p>{{ totalUnread > 0 ? '暂无未读消息预览' : '暂无新消息' }}</p>
            <p class="message-panel-empty-tip">{{ messageEmptyTip }}</p>
          </div>

          <ul v-else class="message-preview-list">
            <li
              v-for="item in messagePreview"
              :key="item.id"
              class="message-preview-item"
              :class="{ 'message-preview-item--unread': isUnreadNotify(item) }"
            >
              <router-link
                :to="getNotifyItemPath(item)"
                class="message-preview-link"
                @click="closePanel"
              >
                <div class="message-preview-head">
                  <span v-if="item.notifyTypeName" class="message-preview-type">
                    {{ item.notifyTypeName }}
                  </span>
                  <h3 class="message-preview-title">{{ item.title || item.notifyTypeName || '系统消息' }}</h3>
                  <span
                    v-if="isUnreadNotify(item)"
                    class="message-preview-dot"
                    aria-label="未读"
                  ></span>
                </div>
                <p class="message-preview-content">{{ item.content }}</p>
                <p class="message-preview-time">{{ formatNotifyTime(item.createTime) }}</p>
              </router-link>
            </li>
          </ul>
        </div>

        <footer v-if="isLoggedIn && messageCenterLink" class="float-panel-footer">
          <router-link :to="messageCenterLink" class="float-panel-link" @click="closePanel">
            进入消息中心 ›
          </router-link>
        </footer>
      </aside>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { streamCustomerChat } from '@/api/customer'
import { getDefaultNotifyPath, getNotifyMessagePath } from '@/constants/notify'
import {
  formatNotifyTime,
  isUnreadNotify,
  loadNotifyPreview,
} from '@/composables/useNotifyList'
import { useNotifyUnreadStats } from '@/composables/useNotifyUnreadStats'
import { NOTIFY_CHANGED_EVENT } from '@/utils/notifyEvent'
import type { Notify } from '@/types/notify'
import {
  AUTH_CHANGED_EVENT,
  getUserInfo,
  isAdmin,
  isLoggedIn as checkLoggedIn,
} from '@/utils/auth'
import { resolveFileUrl } from '@/utils/file'
import type { LoginUserInfo } from '@/types/user'

type PanelType = 'service' | 'message'

interface ChatMessage {
  id: string
  role: 'user' | 'bot'
  text: string
  streaming?: boolean
}

const CHAT_ID_STORAGE_KEY = 'customer_chat_id'

const quickQuestions = [
  '如何申请开店',
  '如何发布商品',
  '忘记密码怎么办',
  '订单什么时候发货',
]

const activePanel = ref<PanelType | null>(null)
const showBackTop = ref(false)
const chatInput = ref('')
const chatLoading = ref(false)
const chatId = ref(sessionStorage.getItem(CHAT_ID_STORAGE_KEY) ?? '')
const chatBodyRef = ref<HTMLElement | null>(null)
let streamAbort: AbortController | null = null
const chatMessages = ref<ChatMessage[]>([])

const isLoggedIn = ref(checkLoggedIn())
const currentUserInfo = ref<LoginUserInfo | null>(getUserInfo())
const userAvatarLoadFailed = ref(false)
const messageLoading = ref(false)
const messageError = ref('')
const messagePreview = ref<Notify[]>([])
const {
  totalUnread,
  formatBadgeCount,
  refreshUnreadStats,
} = useNotifyUnreadStats()

const userAvatarUrl = computed(() => resolveFileUrl(currentUserInfo.value?.avatar))
const userAvatarSrc = computed(() => {
  if (userAvatarUrl.value && !userAvatarLoadFailed.value) return userAvatarUrl.value
  return ''
})

const messageBadge = computed(() => {
  if (!isLoggedIn.value || !totalUnread.value) return ''
  return formatBadgeCount(totalUnread.value)
})

const messageCenterLink = computed(() => {
  if (!checkLoggedIn()) return ''
  return getDefaultNotifyPath(isAdmin())
})

const messagePanelSubtitle = computed(() => {
  if (!isLoggedIn.value) return '登录后可查看消息'
  if (isAdmin()) return '开店申请、商品上架等审核提醒'
  return '订单、发货、收货等业务通知'
})

const messageEmptyTip = computed(() => {
  if (isAdmin()) return '有新的开店/上架申请时会在此提醒'
  return '订单、审核等通知将在此展示'
})

function getNotifyItemPath(item: Notify) {
  return getNotifyMessagePath(item.notifyType, isAdmin())
}

function togglePanel(panel: PanelType) {
  activePanel.value = activePanel.value === panel ? null : panel
}

function closePanel() {
  activePanel.value = null
}

function handleScroll() {
  showBackTop.value = window.scrollY > 320
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function createChatId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

async function scrollChatToBottom() {
  await nextTick()
  const el = chatBodyRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function ensureChatId() {
  if (!chatId.value) {
    chatId.value = createChatId()
    sessionStorage.setItem(CHAT_ID_STORAGE_KEY, chatId.value)
  }
  return chatId.value
}

function findChatMessage(id: string) {
  return chatMessages.value.find((item) => item.id === id)
}

async function requestBotReply(message: string) {
  if (chatLoading.value) return

  chatLoading.value = true
  streamAbort?.abort()
  streamAbort = new AbortController()

  const botMsgId = createChatId()
  chatMessages.value.push({
    id: botMsgId,
    role: 'bot',
    text: '',
    streaming: true,
  })
  await scrollChatToBottom()

  const currentChatId = ensureChatId()

  try {
    await streamCustomerChat({
      message,
      chatId: currentChatId,
      signal: streamAbort.signal,
      onChunk: (chunk) => {
        const botMsg = findChatMessage(botMsgId)
        if (botMsg) {
          botMsg.text += chunk
          scrollChatToBottom()
        }
      },
    })

    const botMsg = findChatMessage(botMsgId)
    if (botMsg) {
      botMsg.streaming = false
      if (!botMsg.text.trim()) {
        botMsg.text = '抱歉，暂时没有获取到回复，请稍后再试。'
      }
    }
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') return

    const botMsg = findChatMessage(botMsgId)
    if (botMsg) {
      botMsg.streaming = false
      botMsg.text = err instanceof Error ? err.message : '客服暂时不可用，请稍后重试'
    }
  } finally {
    chatLoading.value = false
    streamAbort = null
    scrollChatToBottom()
  }
}

function sendQuickQuestion(question: string) {
  if (chatLoading.value) return

  chatMessages.value.push({
    id: createChatId(),
    role: 'user',
    text: question,
  })
  requestBotReply(question)
  scrollChatToBottom()
}

function sendChatMessage() {
  const text = chatInput.value.trim()
  if (!text || chatLoading.value) return

  chatMessages.value.push({
    id: createChatId(),
    role: 'user',
    text,
  })
  chatInput.value = ''
  requestBotReply(text)
  scrollChatToBottom()
}

async function loadMessages() {
  if (!checkLoggedIn()) {
    messagePreview.value = []
    return
  }

  isLoggedIn.value = true
  messageLoading.value = true
  messageError.value = ''

  try {
    const [preview] = await Promise.all([
      loadNotifyPreview(8),
      refreshUnreadStats(),
    ])
    messagePreview.value = preview
  } catch (err) {
    messageError.value = err instanceof Error ? err.message : '加载消息失败'
    messagePreview.value = []
  } finally {
    messageLoading.value = false
  }
}

function syncAuthState() {
  isLoggedIn.value = checkLoggedIn()
  currentUserInfo.value = getUserInfo()
  userAvatarLoadFailed.value = false
  if (!isLoggedIn.value) {
    messagePreview.value = []
    return
  }
  loadMessages()
}

watch(activePanel, (panel) => {
  if (panel === 'message') {
    loadMessages()
  }
})

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener(AUTH_CHANGED_EVENT, syncAuthState)
  window.addEventListener(NOTIFY_CHANGED_EVENT, loadMessages)
  handleScroll()
  loadMessages()
})

onUnmounted(() => {
  streamAbort?.abort()
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener(AUTH_CHANGED_EVENT, syncAuthState)
  window.removeEventListener(NOTIFY_CHANGED_EVENT, loadMessages)
})
</script>

<style scoped>
.float-toolbar {
  position: fixed;
  right: 0;
  top: 50%;
  z-index: 900;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  width: 56px;
  padding: 8px 0;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--home-border);
  border-right: none;
  border-radius: var(--home-card-radius-sm) 0 0 var(--home-card-radius-sm);
  box-shadow: -4px 4px 24px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12px);
  transform: translateY(-50%);
}

.float-toolbar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 10px 4px;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--text-normal);
  transition: color 0.2s, background 0.2s;
}

.float-toolbar-item:hover,
.float-toolbar-item--active {
  color: var(--accent);
  background: var(--accent-soft);
}

.float-toolbar-item--muted {
  color: var(--text-muted);
}

.float-toolbar-item--muted:hover {
  color: var(--accent);
  background: var(--accent-soft);
}

.float-toolbar-icon {
  position: relative;
  width: 22px;
  height: 22px;
  background-repeat: no-repeat;
  background-position: center;
  background-size: contain;
}

.float-toolbar-icon--service {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='1.8'%3E%3Cpath d='M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z'/%3E%3C/svg%3E");
}

.float-toolbar-item:hover .float-toolbar-icon--service,
.float-toolbar-item--active .float-toolbar-icon--service {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff5000' stroke-width='1.8'%3E%3Cpath d='M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z'/%3E%3C/svg%3E");
}

.float-toolbar-icon--message {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='1.8'%3E%3Cpath d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'/%3E%3Cpath d='M13.73 21a2 2 0 0 1-3.46 0'/%3E%3C/svg%3E");
}

.float-toolbar-item:hover .float-toolbar-icon--message,
.float-toolbar-item--active .float-toolbar-icon--message {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff5000' stroke-width='1.8'%3E%3Cpath d='M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9'/%3E%3Cpath d='M13.73 21a2 2 0 0 1-3.46 0'/%3E%3C/svg%3E");
}

.float-toolbar-icon--top {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23666' stroke-width='1.8'%3E%3Cpath d='M12 19V5M5 12l7-7 7 7'/%3E%3C/svg%3E");
}

.float-toolbar-item:hover .float-toolbar-icon--top {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%23ff5000' stroke-width='1.8'%3E%3Cpath d='M12 19V5M5 12l7-7 7 7'/%3E%3C/svg%3E");
}

.float-toolbar-badge {
  position: absolute;
  top: -6px;
  right: -8px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #ff4d4f;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 600;
}

.float-toolbar-label {
  font-size: 11px;
  line-height: 1.25;
  white-space: nowrap;
}

.float-toolbar-label--stack {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  line-height: 1.1;
  font-size: 10px;
}

.float-toolbar-divider {
  height: 1px;
  margin: 4px 10px;
  background: #f0f0f0;
}

.float-panel-mask {
  position: fixed;
  inset: 0;
  z-index: 910;
  background: rgba(0, 0, 0, 0.35);
}

.float-panel {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 920;
  display: flex;
  flex-direction: column;
  width: min(380px, 100vw);
  height: 100vh;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
}

.float-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 18px 14px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.float-panel-title {
  margin: 0 0 4px;
  font-size: 17px;
  font-weight: 600;
}

.float-panel-subtitle {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.float-panel-close {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.06);
  color: #666;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

/* ========== 淘宝风格客服面板 ========== */
.float-panel--service {
  background: #f5f5f5;
}

.service-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: var(--brand-gradient);
  color: #fff;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.22);
}

.service-header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.service-header-avatar {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #fff;
  padding: 2px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.service-header-avatar-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.service-header-info {
  min-width: 0;
}

.service-header-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.service-header-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
}

.service-header-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 1px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  font-size: 11px;
  line-height: 18px;
}

.service-header-status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.35);
}

.service-header-desc {
  margin: 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.88);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.service-header-close {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.2s;
}

.service-header-close:hover {
  background: rgba(255, 255, 255, 0.32);
}

.service-chat-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px 12px;
  background: #f5f5f5;
}

.service-welcome-card {
  margin-bottom: 16px;
  padding: 14px 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, #fff7f0 0%, #fff 100%);
  border: 1px solid #ffd6e0;
  box-shadow: 0 2px 8px rgba(255, 80, 0, 0.06);
}

.service-welcome-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.service-welcome-desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  color: #999;
}

.service-chat-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 16px;
}

.service-chat-item--user {
  flex-direction: row;
  justify-content: flex-end;
}

.service-chat-content {
  max-width: calc(100% - 88px);
}

.service-chat-item--user .service-chat-content {
  display: flex;
  justify-content: flex-end;
}

.service-chat-avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}

.service-chat-avatar--bot {
  background: #fff;
  padding: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.service-chat-avatar-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.service-chat-avatar--user {
  background: #d9d9d9;
  color: #fff;
  overflow: hidden;
}

.service-chat-avatar-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.service-chat-bubble {
  display: inline-block;
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}

.service-chat-item--bot .service-chat-bubble {
  background: #fff;
  color: #333;
  border-top-left-radius: 4px;
}

.service-chat-item--user .service-chat-bubble {
  background: var(--brand-coral-soft);
  color: var(--text-primary);
  border-top-right-radius: 4px;
}

.service-chat-bubble--streaming::after {
  content: '';
  display: inline-block;
  width: 2px;
  height: 14px;
  margin-left: 2px;
  background: var(--accent);
  vertical-align: text-bottom;
  animation: service-cursor-blink 1s step-end infinite;
}

.service-chat-typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 20px;
  padding: 0 2px;
}

.service-chat-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ccc;
  animation: service-typing-bounce 1.2s ease-in-out infinite;
}

.service-chat-typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.service-chat-typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes service-cursor-blink {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0;
  }
}

@keyframes service-typing-bounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.45;
  }

  30% {
    transform: translateY(-4px);
    opacity: 1;
  }
}

.service-quick-section {
  padding: 10px 12px 8px;
  background: #fff;
  border-top: 1px solid #ebebeb;
}

.service-quick-title {
  margin: 0 0 8px;
  padding-left: 2px;
  font-size: 12px;
  font-weight: 500;
  color: #999;
}

.service-quick-tags {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 2px;
  scrollbar-width: none;
}

.service-quick-tags::-webkit-scrollbar {
  display: none;
}

.service-quick-tag {
  flex-shrink: 0;
  padding: 6px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 16px;
  background: #fff;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
  white-space: nowrap;
}

.service-quick-tag:hover:not(:disabled) {
  color: var(--accent);
  border-color: var(--brand-coral-border);
  background: var(--accent-soft);
}

.service-quick-tag:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.service-footer {
  padding: 10px 12px calc(12px + env(safe-area-inset-bottom, 0px));
  background: #fff;
  border-top: 1px solid #ebebeb;
}

.service-input-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 6px 6px 14px;
  border-radius: 22px;
  background: #f5f5f5;
  border: 1px solid #ebebeb;
  transition: border-color 0.2s, background 0.2s;
}

.service-input-wrap:focus-within {
  background: #fff;
  border-color: var(--brand-coral-border);
  box-shadow: 0 0 0 2px rgba(255, 80, 0, 0.08);
}

.service-input {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 14px;
  outline: none;
  color: #333;
}

.service-input::placeholder {
  color: #bbb;
}

.service-input:disabled {
  opacity: 0.6;
}

.service-send-btn {
  flex-shrink: 0;
  height: 32px;
  padding: 0 14px;
  border: none;
  border-radius: 16px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s, transform 0.15s;
}

.service-send-btn:hover:not(:disabled) {
  opacity: 0.92;
}

.service-send-btn:active:not(:disabled) {
  transform: scale(0.97);
}

.service-send-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.message-panel-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px 0;
}

.message-panel-loading,
.message-panel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 24px;
  text-align: center;
  font-size: 14px;
  color: #999;
}

.message-panel-empty--error {
  color: #f5222d;
}

.message-panel-empty-icon {
  font-size: 36px;
  line-height: 1;
}

.message-panel-empty-tip {
  margin: 0;
  font-size: 12px;
  color: #bbb;
}

.message-panel-loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--accent);
  animation: float-loading-pulse 1s ease-in-out infinite;
}

@keyframes float-loading-pulse {
  0%,
  100% {
    opacity: 0.4;
    transform: scale(0.9);
  }

  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.message-preview-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.message-preview-item {
  border-bottom: 1px solid #f5f5f5;
}

.message-preview-link {
  display: block;
  padding: 14px 18px;
  color: inherit;
  text-decoration: none;
  transition: background 0.2s;
}

.message-preview-link:hover {
  background: #fafafa;
}

.message-preview-item--unread .message-preview-link {
  background: #fffaf5;
}

.message-preview-item--unread .message-preview-link:hover {
  background: #fff3eb;
}

.message-preview-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 8px;
  margin-bottom: 6px;
}

.message-preview-type {
  flex-shrink: 0;
  padding: 1px 6px;
  border-radius: 4px;
  background: #fff3eb;
  color: var(--accent);
  font-size: 11px;
  line-height: 18px;
}

.float-panel-unread {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff3eb;
  color: var(--accent);
  font-size: 12px;
  font-weight: 600;
  vertical-align: middle;
}

.message-preview-title {
  margin: 0;
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-preview-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff4d4f;
}

.message-preview-content {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.message-preview-time {
  margin: 0;
  font-size: 11px;
  color: #bbb;
}

.float-panel-footer {
  padding: 12px 18px 18px;
  border-top: 1px solid #f0f0f0;
  text-align: center;
}

.float-panel-link {
  font-size: 13px;
  color: var(--accent);
  text-decoration: none;
}

.float-panel-link:hover {
  text-decoration: underline;
}

.float-panel-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 20px;
  border-radius: 17px;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  text-decoration: none;
  border: none;
  cursor: pointer;
}

.float-panel-btn--ghost {
  background: #fff;
  color: var(--accent);
  border: 1px solid var(--accent);
}

.float-mask-fade-enter-active,
.float-mask-fade-leave-active {
  transition: opacity 0.25s ease;
}

.float-mask-fade-enter-from,
.float-mask-fade-leave-to {
  opacity: 0;
}

.float-panel-slide-enter-active,
.float-panel-slide-leave-active {
  transition: transform 0.28s ease;
}

.float-panel-slide-enter-from,
.float-panel-slide-leave-to {
  transform: translateX(100%);
}

@media (max-width: 768px) {
  .float-toolbar {
    width: 48px;
    border-radius: 6px 0 0 6px;
  }

  .float-toolbar-label {
    font-size: 10px;
  }

  .float-panel {
    width: 100vw;
  }
}
</style>
