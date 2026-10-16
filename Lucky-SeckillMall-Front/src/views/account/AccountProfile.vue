<template>
  <section>
    <div class="account-tabs">
      <button
        type="button"
        class="account-tab"
        :class="{ active: activeTab === 'profile' }"
        @click="activeTab = 'profile'"
      >
        个人资料
      </button>
      <button
        type="button"
        class="account-tab"
        :class="{ active: activeTab === 'avatar' }"
        @click="goToAvatarTab"
      >
        头像照片
      </button>
    </div>

    <div class="account-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="account-msg account-msg--success">{{ successMsg }}</p>
      <p v-if="isDisabled" class="account-msg account-msg--error">当前账号已被禁用，部分功能可能不可用</p>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载中...
      </div>

      <!-- 个人资料 -->
      <div v-else-if="activeTab === 'profile'">
        <div class="account-tip-box">
          <i class="account-tip-icon"></i>
          <span>亲爱的 <em>{{ maskedMobile }}</em>，填写真实的资料，有助于好友找到你哦。</span>
        </div>

        <form class="account-form" @submit.prevent="handleSave">
          <div class="account-form-row">
            <label class="account-form-label">当前头像</label>
            <div class="account-form-field">
              <button type="button" class="profile-avatar-trigger" @click="goToAvatarTab">
                <img
                  v-if="avatarUrl && !avatarLoadFailed"
                  :src="avatarUrl"
                  alt=""
                  class="profile-avatar-img"
                  referrerpolicy="no-referrer"
                  @error="avatarLoadFailed = true"
                />
                <span v-else class="profile-avatar-img profile-avatar-img--placeholder">{{ avatarInitial }}</span>
                <span class="profile-avatar-mask">修改头像</span>
              </button>
            </div>
          </div>

          <div class="account-form-row">
            <label class="account-form-label">昵称</label>
            <div class="account-form-field">
              <input
                v-model="nickname"
                type="text"
                maxlength="20"
                class="account-input"
                placeholder="请输入昵称"
              />
              <p class="account-tip">昵称不能为空，保存后将同步到账号信息</p>
            </div>
          </div>

          <div class="account-form-row">
            <label class="account-form-label">手机号</label>
            <div class="account-form-field">
              <input :value="maskedMobile" type="text" class="account-input" readonly />
              <p class="account-tip">手机号暂不支持修改</p>
            </div>
          </div>

          <div class="account-form-row">
            <label class="account-form-label">账户余额</label>
            <div class="account-form-field">
              <div class="profile-balance-row">
                <span v-if="balanceLoading" class="profile-balance-amount profile-balance-amount--muted">
                  加载中...
                </span>
                <span v-else class="profile-balance-amount">¥ {{ balanceText }}</span>
                <router-link to="/account/balance" class="profile-balance-link">查看详情</router-link>
              </div>
              <p v-if="balanceErrorMsg" class="profile-balance-error">{{ balanceErrorMsg }}</p>
              <p v-else class="account-tip">余额可用于商城内支付，可在账户余额页充值</p>
            </div>
          </div>

          <div class="account-actions">
            <button type="submit" class="account-btn account-btn-primary" :disabled="saving">
              {{ saving ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>

      <!-- 头像照片 -->
      <div v-else>
        <div class="account-tip-box">
          <i class="account-tip-icon"></i>
          <span>亲爱的 <em>{{ maskedMobile }}</em>，上传清晰头像，让好友更容易认出你。</span>
        </div>

        <div class="account-form">
          <div class="account-form-row">
            <label class="account-form-label">当前头像</label>
            <div class="account-form-field">
              <div class="avatar-upload">
                <img
                  v-if="avatarUrl && !avatarLoadFailed"
                  :src="avatarUrl"
                  alt=""
                  class="avatar-preview"
                  referrerpolicy="no-referrer"
                  @error="avatarLoadFailed = true"
                />
                <span v-else class="avatar-preview avatar-preview--placeholder">{{ avatarInitial }}</span>
                <label class="account-btn avatar-upload-btn">
                  选择图片
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    :disabled="uploading"
                    @change="handleAvatarSelect"
                  />
                </label>
              </div>
              <p v-if="uploading" class="account-tip">头像上传中...</p>
              <p v-else class="account-tip">支持 JPG、PNG、WEBP、GIF，不超过 5MB，选择后可裁剪并上传</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <AvatarCropper
      :visible="cropperVisible"
      :image-src="cropperImageSrc"
      @confirm="handleCropConfirm"
      @cancel="handleCropCancel"
    />
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import AvatarCropper from '@/components/account/AvatarCropper.vue'
import { AVATAR_MAX_SIZE, useUserProfile } from '@/composables/useUserProfile'
import { useUserBalance } from '@/composables/useUserBalance'

type TabType = 'profile' | 'avatar'

const activeTab = ref<TabType>('profile')
const avatarLoadFailed = ref(false)
const cropperVisible = ref(false)
const cropperImageSrc = ref('')

const {
  loading,
  saving,
  uploading,
  errorMsg,
  successMsg,
  nickname,
  avatarUrl,
  maskedMobile,
  isDisabled,
  avatarInitial,
  loadProfile,
  saveProfile,
  uploadUserAvatar,
  clearMessages,
} = useUserProfile()

const {
  loading: balanceLoading,
  errorMsg: balanceErrorMsg,
  balanceText,
  loadBalance,
} = useUserBalance()

watch(avatarUrl, () => {
  avatarLoadFailed.value = false
})

function goToAvatarTab() {
  clearMessages()
  activeTab.value = 'avatar'
}

function revokeCropperSrc() {
  if (cropperImageSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(cropperImageSrc.value)
  }
  cropperImageSrc.value = ''
}

function handleAvatarSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  if (!file.type.startsWith('image/')) {
    errorMsg.value = '请选择图片文件'
    return
  }

  if (file.size > AVATAR_MAX_SIZE) {
    errorMsg.value = '图片大小不能超过 5MB'
    return
  }

  clearMessages()
  revokeCropperSrc()
  cropperImageSrc.value = URL.createObjectURL(file)
  cropperVisible.value = true
}

function handleCropCancel() {
  cropperVisible.value = false
  revokeCropperSrc()
}

async function handleCropConfirm(file: File) {
  cropperVisible.value = false
  revokeCropperSrc()
  await uploadUserAvatar(file)
}

async function handleSave() {
  await saveProfile()
}

onMounted(async () => {
  await Promise.allSettled([loadProfile(), loadBalance()])
})
</script>

<style scoped>
.profile-balance-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 32px;
}

.profile-balance-amount {
  font-size: 16px;
  font-weight: 600;
  color: var(--accent);
}

.profile-balance-amount--muted {
  font-size: 12px;
  font-weight: 400;
  color: #999;
}

.profile-balance-link {
  font-size: 12px;
  color: #666;
  transition: color 0.2s;
}

.profile-balance-link:hover {
  color: var(--accent);
}

.profile-balance-error {
  margin: 6px 0 0;
  font-size: 12px;
  color: #cf1322;
}
</style>
