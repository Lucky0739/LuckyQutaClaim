<template>
  <header class="header">
    <!-- 顶部工具栏 -->
    <div class="header-top">
      <div class="header-container header-top-inner">
        <div class="header-top-left">
          <template v-if="isLoggedIn && userInfo">
            <UserAccountDropdown :user-info="userInfo" @logout="userInfo = null" />
          </template>
          <template v-else>
            <span class="welcome">Hi，欢迎来到秒杀商城</span>
            <span class="divider">|</span>
            <router-link to="/login" class="top-link">请登录</router-link>
            <router-link to="/register" class="top-link highlight">免费注册</router-link>
          </template>
        </div>
        <ul class="header-top-right">
          <li v-for="item in topNavLinks" :key="item.label">
            <TopNavDropdown
              v-if="item.dropdownOptions"
              :label="item.label"
              :options="item.dropdownOptions"
              :badge="item.badge"
              :icon="item.icon"
            />
            <router-link v-else-if="item.to" :to="item.to" class="top-link" :class="{ 'top-link--with-icon': item.icon }">
              <img v-if="item.icon" :src="item.icon" alt="" class="top-link-icon" />
              {{ item.label }}
              <span v-if="item.badge" class="badge">{{ item.badge }}</span>
            </router-link>
            <a v-else href="#" class="top-link" :class="{ 'top-link--with-icon': item.icon }">
              <img v-if="item.icon" :src="item.icon" alt="" class="top-link-icon" />
              {{ item.label }}
              <span v-if="item.badge" class="badge">{{ item.badge }}</span>
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- 主头部：Logo + 搜索 -->
    <div v-if="!topOnly" class="header-main">
      <div class="header-container header-main-inner">
        <router-link to="/" class="logo">
          <span class="logo-brand">秒杀商城</span>
        </router-link>

        <div class="search-area">
          <div class="search-box">
            <SearchTypeDropdown v-model="activeTab" :options="searchTabs" />
            <span class="search-tab-divider"></span>
            <input
              v-model="searchKeyword"
              type="text"
              class="search-input"
              :placeholder="activeTab === 'goods' ? '请输入商品名称' : '请输入店铺名称'"
              @keyup.enter="handleSearch"
            />
            <button type="button" class="search-btn" @click="handleSearch">搜索</button>
          </div>
        </div>

        <div class="header-spacer" aria-hidden="true"></div>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AUTH_CHANGED_EVENT,
  getUserInfo,
  isAdmin,
  isLoggedIn as checkLoggedIn,
} from '@/utils/auth'
import { getDefaultNotifyPath } from '@/constants/notify'
import { useCartCount } from '@/composables/useCartCount'
import { useNotifyUnreadStats } from '@/composables/useNotifyUnreadStats'
import { NOTIFY_CHANGED_EVENT } from '@/utils/notifyEvent'
import type { LoginUserInfo } from '@/types/user'
import SearchTypeDropdown from './SearchTypeDropdown.vue'
import TopNavDropdown from './TopNavDropdown.vue'
import UserAccountDropdown from './UserAccountDropdown.vue'
import type { TopNavDropdownOption } from './TopNavDropdown.vue'
import cartIcon from '@/assets/gouwuche.png'
import favoriteIcon from '@/assets/shoucang.png'
import adminIcon from '@/assets/xitongguanli.png'
import shopIcon from '@/assets/wodedianpu.png'
import orderIcon from '@/assets/wodedingdan.png'
import freeShopIcon from '@/assets/mianfeikaidian.png'
import messageIcon from '@/assets/wode-xiaoxi.png'
import merchantCenterIcon from '@/assets/zhongxin-shangjiazhongxin.png'
import homeIcon from '@/assets/shouye.png'

defineProps<{ topOnly?: boolean }>()

const route = useRoute()
const router = useRouter()
const userInfo = ref<LoginUserInfo | null>(checkLoggedIn() ? getUserInfo() : null)
const isLoggedIn = computed(() => checkLoggedIn() && !!userInfo.value)
const showAdminNav = computed(() => isLoggedIn.value && isAdmin())
const messageCenterPath = computed(() => getDefaultNotifyPath())
const { cartCount, formatCartBadge, refreshCartCount } = useCartCount()
const { totalUnread, formatBadgeCount, refreshUnreadStats } = useNotifyUnreadStats()
const messageUnreadBadge = computed(() => {
  if (!isLoggedIn.value || totalUnread.value <= 0) return undefined
  return formatBadgeCount(totalUnread.value)
})

const activeTab = ref<'goods' | 'shop'>('goods')
const searchKeyword = ref('')

interface TopNavLink {
  label: string
  to?: string
  badge?: string
  icon?: string
  dropdownOptions?: TopNavDropdownOption[]
}

const favoriteOptions: TopNavDropdownOption[] = [
  { label: '收藏店铺', to: '/favorites/shops' },
  { label: '收藏商品', to: '/favorites/goods' },
]

const merchantCenterOptions: TopNavDropdownOption[] = [
  { label: '我的店铺', to: '/merchant/shop' },
  { label: '商品管理', to: '/merchant/goods' },
  { label: '秒杀申请', to: '/merchant/seckill' },
  { label: '订单管理', to: '/merchant/orders' },
  { label: '发布商品', to: '/merchant/goods/publish' },
]

const baseTopNavLinks: TopNavLink[] = [
  { label: '首页', to: '/', icon: homeIcon },
  { label: '我的店铺', to: '/merchant/shop', icon: shopIcon },
  { label: '我的订单', to: '/orders', icon: orderIcon },
  { label: '购物车', to: '/cart', icon: cartIcon },
  { label: '我的收藏', dropdownOptions: favoriteOptions, icon: favoriteIcon },
  { label: '免费开店', to: '/shop/create', icon: freeShopIcon },
  { label: '我的消息', icon: messageIcon },
]

function hasMerchantRole(roles?: string[]): boolean {
  return roles?.includes('merchant') ?? false
}

const topNavLinks = computed(() => {
  const roles = userInfo.value?.roles
  const links = baseTopNavLinks
    .filter((item) => {
      if (item.label === '我的消息') return isLoggedIn.value
      if (item.label === '我的店铺') return !showAdminNav.value
      if (item.label === '免费开店') return !showAdminNav.value
      if (item.label === '我的订单') return !showAdminNav.value
      if (item.label === '购物车') return !showAdminNav.value
      return true
    })
    .map((item) => {
      if (item.label === '我的消息') {
        return {
          ...item,
          to: messageCenterPath.value,
          badge: messageUnreadBadge.value,
        }
      }
      if (item.label === '购物车' && isLoggedIn.value) {
        return {
          ...item,
          badge: formatCartBadge(cartCount.value),
        }
      }
      return item
    })
  if (isLoggedIn.value && hasMerchantRole(roles)) {
    const merchantOptions = showAdminNav.value
      ? merchantCenterOptions.filter((item) => item.label !== '我的店铺')
      : merchantCenterOptions
    if (merchantOptions.length) {
      links.push({ label: '商家中心', dropdownOptions: merchantOptions, icon: merchantCenterIcon })
    }
  }
  if (showAdminNav.value) {
    links.push({ label: '系统管理', icon: adminIcon, to: '/admin/users' })
  }
  return links
})

const searchTabs = [
  { value: 'goods' as const, label: '宝贝' },
  { value: 'shop' as const, label: '店铺' },
]

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.push({ path: '/search', query: { type: activeTab.value, q: keyword } })
}

function syncSearchFromRoute() {
  if (route.path !== '/search') return
  const type = route.query.type
  if (type === 'goods' || type === 'shop') activeTab.value = type
  const keyword = route.query.q
  if (typeof keyword === 'string') searchKeyword.value = keyword
}

function syncUserInfo() {
  userInfo.value = checkLoggedIn() ? getUserInfo() : null
}

function onAuthChanged() {
  syncUserInfo()
  void refreshUnreadStats()
  void refreshCartCount()
}

onMounted(() => {
  syncUserInfo()
  syncSearchFromRoute()
  window.addEventListener(AUTH_CHANGED_EVENT, onAuthChanged)
  window.addEventListener(NOTIFY_CHANGED_EVENT, refreshUnreadStats)
})

watch(isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    void refreshUnreadStats()
    void refreshCartCount()
  }
})

onUnmounted(() => {
  window.removeEventListener(AUTH_CHANGED_EVENT, onAuthChanged)
  window.removeEventListener(NOTIFY_CHANGED_EVENT, refreshUnreadStats)
})

watch(() => route.fullPath, () => {
  syncUserInfo()
  syncSearchFromRoute()
})

watch(activeTab, (type) => {
  if (route.path !== '/search') return
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.replace({ path: '/search', query: { type, q: keyword } })
})
</script>

<style scoped>
.header {
  position: relative;
  z-index: 110;
  width: 100%;
}

.header-container {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
}

/* 顶部工具栏 */
.header-top {
  background: #fff7fa;
  border-bottom: 1px solid rgba(255, 81, 124, 0.12);
  font-size: 12px;
  color: #666;
  line-height: 28px;
  height: 28px;
}

.header-top-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-top-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome {
  color: #999;
}

.divider {
  color: #ddd;
}

.header-top-right {
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0;
}

.header-top-right li {
  padding: 0 12px;
  position: relative;
}

.header-top-right li:not(:last-child)::after {
  content: '|';
  position: absolute;
  right: 0;
  color: #ddd;
}

.top-link {
  color: #666;
  transition: color 0.2s;
}

.top-link:hover,
.top-link.highlight {
  color: var(--accent);
}

.top-link--with-icon {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.top-link-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
}

.badge {
  color: var(--accent);
  margin-left: 2px;
}

/* 主头部 */
.header-main {
  background: linear-gradient(180deg, #fff7fb 0%, #fff 100%);
  padding: 14px 0 16px;
}

.header-main-inner {
  display: grid;
  grid-template-columns: 170px 1fr 170px;
  align-items: flex-start;
  gap: 18px;
}

.logo {
  flex-shrink: 0;
  padding-top: 2px;
}

.logo-brand {
  font-family: "ZCOOL QingKe HuangYou", "PingFang SC", sans-serif;
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 2px;
  color: var(--accent);
  text-shadow: 0 2px 0 rgba(255, 169, 93, 0.16);
}

.search-area {
  width: min(980px, 100%);
  justify-self: center;
  padding-top: 2px;
  position: relative;
  z-index: 1;
}

.header-spacer {
  width: 160px;
}

.search-box {
  display: flex;
  align-items: center;
  height: 48px;
  background: #fff;
  border: 2px solid #ff5000;
  border-radius: 15px;
  overflow: visible;
  box-shadow: none;
  transition: box-shadow 0.2s, border-color 0.2s;
}

.search-box:focus-within {
  border-color: #f24a00;
  box-shadow: 0 0 0 3px rgba(255, 80, 0, 0.08);
}

.search-tab-divider {
  width: 1px;
  height: 22px;
  background: #d8d8d8;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0 18px;
  border: none;
  outline: none;
  font-size: 15px;
  color: #333;
  background: transparent;
}

.search-input::placeholder {
  color: #bbb;
}

.search-btn {
  flex-shrink: 0;
  width: 92px;
  height: 40px;
  margin-right: 3px;
  border: none;
  background: #ff5000;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 1px;
  transition: background 0.2s;
  border-radius: 12px;
}

.search-btn:hover {
  background: #f24a00;
}

@media (max-width: 768px) {
  .header-top {
    display: none;
  }

  .header-main-inner {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 0 12px;
  }

  .logo-brand {
    font-size: 28px;
  }

  .search-area {
    width: 100%;
    justify-self: stretch;
  }

  .header-spacer {
    display: none;
  }
}
</style>
