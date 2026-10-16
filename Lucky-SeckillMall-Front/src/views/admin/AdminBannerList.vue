<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">背景图管理</span>
    </div>

    <div class="account-panel admin-banner-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <div class="admin-banner-toolbar">
        <p class="admin-banner-toolbar-tip">
          共 {{ banners.length }} 张背景图，按排序值从小到大展示于首页轮播
        </p>
        <label class="account-btn account-btn-primary admin-banner-upload-btn">
          {{ uploading || saving ? '上传中...' : '上传背景图' }}
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            class="admin-banner-upload-input"
            :disabled="isBusy"
            @change="handleUploadChange"
          />
        </label>
      </div>

      <p class="admin-banner-tip">支持 JPG、PNG、WebP、GIF，单张不超过 5MB</p>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载背景图列表...
      </div>

      <div v-else-if="errorMsg && !hasBanners" class="address-empty">
        <div class="address-empty-icon address-empty-icon--warn"></div>
        <p class="address-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasBanners" class="address-empty">
        <div class="address-empty-icon"></div>
        <p class="address-empty-text">暂无背景图，请上传首页轮播图</p>
      </div>

      <div v-else class="admin-banner-grid">
        <article v-for="(banner, index) in banners" :key="banner.id" class="admin-banner-card">
          <div class="admin-banner-preview">
            <img
              :src="resolveFileUrl(banner.bannerUrl)"
              alt=""
              class="admin-banner-image"
              referrerpolicy="no-referrer"
            />
            <span class="admin-banner-sort">第 {{ index + 1 }} 张</span>
          </div>
          <div class="admin-banner-meta">
            <p class="admin-banner-url" :title="banner.bannerUrl">{{ banner.bannerUrl }}</p>
            <div class="admin-banner-actions">
              <button
                type="button"
                class="admin-user-action"
                :disabled="isBusy || index === 0"
                @click="moveUp(index)"
              >
                上移
              </button>
              <span class="address-action-divider">|</span>
              <button
                type="button"
                class="admin-user-action"
                :disabled="isBusy || index === banners.length - 1"
                @click="moveDown(index)"
              >
                下移
              </button>
              <span class="address-action-divider">|</span>
              <button
                type="button"
                class="admin-user-action admin-user-action--danger"
                :disabled="isBusy"
                @click="openDeleteConfirm(banner)"
              >
                删除
              </button>
            </div>
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
        v-if="showDeleteConfirm && pendingDeleteBanner"
        class="address-modal-mask"
        @click.self="closeDeleteConfirm"
      >
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">删除背景图</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="!!deletingUrl"
              @click="closeDeleteConfirm"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p v-if="deleteErrorMsg" class="account-msg account-msg--error">{{ deleteErrorMsg }}</p>
            <p class="admin-user-confirm-text">
              确定删除第
              <strong>{{ deleteBannerIndex }}</strong>
              张背景图吗？删除后将自动重排剩余图片顺序。
            </p>
            <div class="admin-banner-delete-preview">
              <img
                :src="resolveFileUrl(pendingDeleteBanner.bannerUrl)"
                alt=""
                class="admin-banner-delete-image"
                referrerpolicy="no-referrer"
              />
            </div>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="!!deletingUrl" @click="closeDeleteConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-danger"
              :disabled="!!deletingUrl"
              @click="confirmDelete"
            >
              {{ deletingUrl ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAdminBannerList } from '@/composables/useAdminBannerList'
import { resolveFileUrl } from '@/utils/file'

const {
  loading,
  errorMsg,
  toastMsg,
  banners,
  uploading,
  saving,
  deletingUrl,
  showDeleteConfirm,
  pendingDeleteBanner,
  deleteErrorMsg,
  hasBanners,
  isBusy,
  loadList,
  uploadAndAdd,
  openDeleteConfirm,
  closeDeleteConfirm,
  confirmDelete,
  moveUp,
  moveDown,
} = useAdminBannerList()

const deleteBannerIndex = computed(() => {
  const banner = pendingDeleteBanner.value
  if (!banner) return 0
  const index = banners.value.findIndex((item) => item.bannerUrl === banner.bannerUrl)
  return index >= 0 ? index + 1 : 0
})

function handleUploadChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  void uploadAndAdd(file)
}
</script>

<style scoped>
.admin-page-tab {
  cursor: default;
}

.admin-banner-panel {
  padding-top: 20px;
}

.admin-banner-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 8px;
}

.admin-banner-toolbar-tip {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.admin-banner-tip {
  margin: 0 0 20px;
  font-size: 12px;
  color: #bbb;
}

.admin-banner-upload-btn {
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.admin-banner-upload-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.admin-banner-upload-input:disabled {
  cursor: not-allowed;
}

.admin-banner-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.admin-banner-card {
  border: 1px solid #e5e5e5;
  background: #fff;
  overflow: hidden;
}

.admin-banner-preview {
  position: relative;
  aspect-ratio: 16 / 6;
  background: #f5f5f5;
}

.admin-banner-image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-banner-sort {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 0 8px;
  line-height: 22px;
  font-size: 12px;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
}

.admin-banner-meta {
  padding: 12px 14px 14px;
}

.admin-banner-url {
  margin: 0 0 10px;
  font-size: 11px;
  color: #999;
  line-height: 1.5;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.admin-banner-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
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
  opacity: 0.45;
  cursor: not-allowed;
}

.admin-user-action--danger {
  color: #f5222d;
}

.admin-user-confirm-text {
  margin: 0 0 16px;
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

.admin-user-confirm-text strong {
  color: #333;
}

.admin-banner-delete-preview {
  border: 1px solid #f0f0f0;
  background: #fafafa;
}

.admin-banner-delete-image {
  display: block;
  width: 100%;
  max-height: 180px;
  object-fit: cover;
}

@media (max-width: 640px) {
  .admin-banner-toolbar {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
