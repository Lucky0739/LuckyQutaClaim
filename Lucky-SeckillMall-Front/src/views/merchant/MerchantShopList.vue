<template>
  <div class="page">
    <Header top-only />

    <main class="merchant-page">
      <div class="merchant-page-inner">
        <nav class="account-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span>商家中心</span>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span class="account-breadcrumb-current">我的店铺</span>
        </nav>

        <div class="merchant-panel">
          <div class="merchant-panel-head">
            <div>
              <h1 class="merchant-panel-title">我的店铺</h1>
              <p class="merchant-panel-desc">查看您申请或经营中的全部店铺</p>
            </div>
            <router-link to="/shop/create" class="account-btn account-btn-primary merchant-create-btn">
              申请开店
            </router-link>
          </div>

          <div v-if="!loading && !errorMsg" class="merchant-shop-toolbar">
            <p class="merchant-shop-toolbar-tip">共 {{ total }} 家店铺</p>
          </div>

          <div v-if="loading" class="address-loading">
            <span class="address-loading-dot"></span>
            加载店铺列表...
          </div>

          <div v-else-if="errorMsg && !shops.length" class="address-empty">
            <div class="address-empty-icon address-empty-icon--warn"></div>
            <p class="address-empty-text">{{ errorMsg }}</p>
            <button type="button" class="account-btn account-btn-primary" @click="loadList">
              重新加载
            </button>
          </div>

          <div v-else-if="!shops.length" class="address-empty">
            <div class="address-empty-icon"></div>
            <p class="address-empty-text">您还没有店铺，先去申请开店吧</p>
            <router-link to="/shop/create" class="account-btn account-btn-primary">免费开店</router-link>
          </div>

          <ul v-else class="merchant-shop-list">
            <li v-for="shop in shops" :key="shop.id" class="merchant-shop-card">
              <div class="merchant-shop-card-main">
                <div class="merchant-shop-avatar-wrap">
                  <img
                    v-if="shop.shopAvatar && !avatarErrors[shop.id]"
                    :src="resolveFileUrl(shop.shopAvatar)"
                    alt=""
                    class="merchant-shop-avatar"
                    referrerpolicy="no-referrer"
                    @error="avatarErrors[shop.id] = true"
                  />
                  <span v-else class="merchant-shop-avatar merchant-shop-avatar--placeholder">
                    {{ shop.shopName?.charAt(0) || '店' }}
                  </span>
                </div>

                <div class="merchant-shop-info">
                  <div class="merchant-shop-info-head">
                    <h2 class="merchant-shop-name">{{ shop.shopName }}</h2>
                    <span class="merchant-shop-id">ID: {{ shop.id }}</span>
                  </div>

                  <p class="merchant-shop-desc">{{ shop.shopDesc || '暂无店铺简介' }}</p>

                  <div class="merchant-shop-tags">
                    <span
                      class="merchant-shop-tag merchant-shop-tag--audit"
                      :class="getAuditTagClass(shop.auditStatus)"
                    >
                      {{ formatAuditStatus(shop.auditStatus, shop.auditStatusDesc) }}
                    </span>
                    <span
                      v-if="shop.auditStatus === ShopAuditStatus.APPROVED"
                      class="merchant-shop-tag merchant-shop-tag--status"
                      :class="{
                        'merchant-shop-tag--enabled': isShopEnabled(shop.shopStatus),
                        'merchant-shop-tag--disabled': !isShopEnabled(shop.shopStatus),
                      }"
                    >
                      {{ formatShopStatus(shop.shopStatus, shop.shopStatusDesc) }}
                    </span>
                  </div>

                  <dl class="merchant-shop-meta">
                    <div class="merchant-shop-meta-item">
                      <dt>申请时间</dt>
                      <dd>{{ formatDateTime(shop.createTime) }}</dd>
                    </div>
                    <div class="merchant-shop-meta-item">
                      <dt>更新时间</dt>
                      <dd>{{ formatDateTime(shop.updateTime) }}</dd>
                    </div>
                  </dl>

                  <p v-if="shop.auditStatus === ShopAuditStatus.REJECTED && shop.auditRemark" class="merchant-shop-remark">
                    审核备注：{{ shop.auditRemark }}
                  </p>
                </div>
              </div>

              <div class="merchant-shop-actions">
                <router-link
                  v-if="shop.auditStatus === ShopAuditStatus.APPROVED && isShopEnabled(shop.shopStatus)"
                  to="/merchant/goods/publish"
                  class="merchant-shop-publish-btn"
                >
                  <span class="merchant-shop-publish-icon">+</span>
                  发布商品
                </router-link>
                <router-link
                  v-else-if="shop.auditStatus === ShopAuditStatus.REJECTED"
                  to="/shop/create"
                  class="account-btn merchant-shop-action-btn"
                >
                  重新申请
                </router-link>
                <span v-else-if="shop.auditStatus === ShopAuditStatus.PENDING" class="merchant-shop-action-tip">
                  审核中，请耐心等待
                </span>
                <span
                  v-else-if="shop.auditStatus === ShopAuditStatus.APPROVED && !isShopEnabled(shop.shopStatus)"
                  class="merchant-shop-action-tip merchant-shop-action-tip--warn"
                >
                  店铺已禁用，请联系平台处理
                </span>
                <button
                  type="button"
                  class="merchant-shop-delete-btn"
                  :disabled="deletingId === shop.id"
                  @click="handleDeleteShop(shop)"
                >
                  {{ deletingId === shop.id ? '删除中...' : '删除店铺' }}
                </button>
              </div>
            </li>
          </ul>

          <p
            v-if="actionMsg"
            class="merchant-shop-action-msg"
            :class="{ 'merchant-shop-action-msg--error': actionMsgType === 'error' }"
          >
            {{ actionMsg }}
          </p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import Header from '@/components/Header.vue'
import { formatAuditStatus, formatShopStatus } from '@/composables/useAdminShopList'
import { formatDateTime } from '@/composables/useAdminUserList'
import { useMerchantShopList } from '@/composables/useMerchantShopList'
import { isShopEnabled, ShopAuditStatus, type ShopApplyVO } from '@/types/shop'
import { resolveFileUrl } from '@/utils/file'

const {
  shops,
  total,
  loading,
  deletingId,
  errorMsg,
  actionMsg,
  actionMsgType,
  loadList,
  deleteShop,
} = useMerchantShopList()
const avatarErrors = reactive<Record<string, boolean>>({})

function handleDeleteShop(shop: ShopApplyVO) {
  void deleteShop(shop)
}

function getAuditTagClass(status: ShopAuditStatus) {
  return {
    'merchant-shop-tag--pending': status === ShopAuditStatus.PENDING,
    'merchant-shop-tag--approved': status === ShopAuditStatus.APPROVED,
    'merchant-shop-tag--rejected': status === ShopAuditStatus.REJECTED,
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f4f4f4;
}

.merchant-page {
  padding: 16px 0 48px;
}

.merchant-page-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 0 10px;
}

.merchant-panel {
  background: #fff;
  border: 1px solid #e5e5e5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  padding: 24px 32px 32px;
}

.merchant-panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.merchant-panel-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.merchant-panel-desc {
  margin: 0;
  font-size: 13px;
  color: #999;
}

.merchant-create-btn {
  flex-shrink: 0;
  text-decoration: none;
}

.merchant-shop-toolbar {
  margin-bottom: 16px;
}

.merchant-shop-toolbar-tip {
  margin: 0;
  font-size: 13px;
  color: #666;
}

.merchant-shop-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.merchant-shop-card {
  border: 1px solid #eee;
  border-radius: 8px;
  background: #fafafa;
  overflow: hidden;
}

.merchant-shop-card-main {
  display: flex;
  gap: 20px;
  padding: 20px;
}

.merchant-shop-avatar-wrap {
  flex-shrink: 0;
}

.merchant-shop-avatar {
  display: block;
  width: 88px;
  height: 88px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
  background: #fff;
}

.merchant-shop-avatar--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 600;
  color: #fff;
  background: var(--brand-gradient);
}

.merchant-shop-info {
  flex: 1;
  min-width: 0;
}

.merchant-shop-info-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}

.merchant-shop-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.merchant-shop-id {
  font-size: 12px;
  color: #999;
}

.merchant-shop-desc {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.6;
  color: #666;
}

.merchant-shop-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.merchant-shop-tag {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1;
}

.merchant-shop-tag--pending {
  color: #d48806;
  background: #fffbe6;
  border: 1px solid #ffe58f;
}

.merchant-shop-tag--approved {
  color: #389e0d;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
}

.merchant-shop-tag--rejected {
  color: #cf1322;
  background: #fff1f0;
  border: 1px solid #ffa39e;
}

.merchant-shop-tag--enabled {
  color: #096dd9;
  background: #e6f7ff;
  border: 1px solid #91d5ff;
}

.merchant-shop-tag--disabled {
  color: #999;
  background: #f5f5f5;
  border: 1px solid #d9d9d9;
}

.merchant-shop-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  margin: 0;
}

.merchant-shop-meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.merchant-shop-meta-item dt {
  margin: 0;
  font-size: 12px;
  color: #999;
  font-weight: 400;
}

.merchant-shop-meta-item dd {
  margin: 0;
  font-size: 12px;
  color: #666;
}

.merchant-shop-remark {
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 4px;
  background: #fff1f0;
  border: 1px solid #ffccc7;
  font-size: 12px;
  line-height: 1.6;
  color: #cf1322;
}

.merchant-shop-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 20px;
  border-top: 1px solid #eee;
  background: linear-gradient(180deg, #fff 0%, #fffaf7 100%);
}

.merchant-shop-action-btn {
  text-decoration: none;
}

.merchant-shop-publish-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-width: 118px;
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
  text-decoration: none;
  box-shadow: 0 6px 14px rgba(255, 80, 0, 0.2);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.18s ease;
}

.merchant-shop-publish-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(255, 80, 0, 0.28);
  opacity: 0.95;
}

.merchant-shop-publish-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.24);
  font-size: 16px;
  line-height: 1;
}

.merchant-shop-action-tip {
  font-size: 13px;
  color: #999;
}

.merchant-shop-action-tip--warn {
  color: #fa8c16;
}

.merchant-shop-delete-btn {
  flex-shrink: 0;
  height: 36px;
  padding: 0 16px;
  border: 1px solid #ffa39e;
  border-radius: 999px;
  background: #fff;
  color: #cf1322;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.18s, border-color 0.18s;
}

.merchant-shop-delete-btn:hover:not(:disabled) {
  background: #fff1f0;
  border-color: #ff7875;
}

.merchant-shop-delete-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.merchant-shop-action-msg {
  margin: 16px 0 0;
  padding: 10px 14px;
  border-radius: 6px;
  background: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #389e0d;
  font-size: 13px;
  text-align: center;
}

.merchant-shop-action-msg--error {
  background: #fff1f0;
  border-color: #ffa39e;
  color: #cf1322;
}

@media (max-width: 768px) {
  .merchant-panel {
    padding: 20px 16px 24px;
  }

  .merchant-panel-head {
    flex-direction: column;
  }

  .merchant-shop-card-main {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .merchant-shop-info-head,
  .merchant-shop-tags,
  .merchant-shop-meta {
    justify-content: center;
  }

  .merchant-shop-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
