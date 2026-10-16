<template>
  <div class="auth-page">
    <div class="auth-header">
      <router-link to="/" class="auth-logo">
        <span class="logo-brand">秒杀商城</span>
      </router-link>
      <router-link to="/" class="auth-header-link">返回首页</router-link>
    </div>

    <div class="auth-body">
      <div class="auth-card create-shop-card">
        <h2 class="auth-title">免费开店</h2>
        <p class="auth-subtitle">填写店铺信息提交申请，审核通过后即可经营</p>

        <div v-if="successMsg" class="create-shop-success">
          <p class="success-title">开店申请已提交</p>
          <p class="success-desc">{{ successMsg }}</p>
          <router-link to="/" class="submit-btn success-btn">返回首页</router-link>
        </div>

        <form v-else class="auth-form" @submit.prevent="handleSubmit">
          <div class="form-item">
            <label class="form-label"><span class="required">*</span>店铺名称</label>
            <input
              v-model="form.shopName"
              type="text"
              :maxlength="SHOP_NAME_MAX_LENGTH"
              class="form-input"
              placeholder="请输入店铺名称"
            />
          </div>

          <div class="form-item">
            <label class="form-label">店铺 LOGO</label>
            <div class="shop-logo-field">
              <button
                type="button"
                class="shop-logo-preview"
                :disabled="uploadingLogo || submitting"
                @click="triggerLogoSelect"
              >
                <img
                  v-if="logoPreview && !logoLoadFailed"
                  :src="logoPreview"
                  alt=""
                  class="shop-logo-img"
                  referrerpolicy="no-referrer"
                  @error="logoLoadFailed = true"
                />
                <span v-else class="shop-logo-img shop-logo-img--placeholder">
                  <span class="shop-logo-plus">+</span>
                  <span class="shop-logo-placeholder-text">上传 LOGO</span>
                </span>
                <span v-if="logoPreview && !logoLoadFailed" class="shop-logo-mask">更换 LOGO</span>
              </button>

              <div class="shop-logo-actions">
                <label class="upload-btn">
                  {{ uploadingLogo ? '上传中...' : '选择图片' }}
                  <input
                    ref="logoInputRef"
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    :disabled="uploadingLogo || submitting"
                    @change="handleLogoSelect"
                  />
                </label>
                <button
                  v-if="form.shopAvatar"
                  type="button"
                  class="clear-btn"
                  :disabled="uploadingLogo || submitting"
                  @click="clearLogo"
                >
                  移除
                </button>
                <p v-if="logoSuccessMsg" class="form-success">{{ logoSuccessMsg }}</p>
                <p v-if="logoErrorMsg" class="form-error-inline">{{ logoErrorMsg }}</p>
                <p class="form-tip">选填，建议上传 1:1 方形 LOGO，支持 JPG、PNG、WEBP、GIF，不超过 5MB</p>
              </div>
            </div>
          </div>

          <div class="form-item">
            <label class="form-label">店铺简介</label>
            <textarea
              v-model="form.shopDesc"
              class="form-textarea"
              :maxlength="SHOP_DESC_MAX_LENGTH"
              rows="4"
              placeholder="简单介绍您的店铺特色（选填）"
            />
            <p class="form-tip">{{ form.shopDesc.length }}/{{ SHOP_DESC_MAX_LENGTH }}</p>
          </div>

          <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

          <button type="submit" class="submit-btn" :disabled="submitting || uploadingLogo">
            {{ submitting ? '提交中...' : '提交开店申请' }}
          </button>
        </form>
      </div>
    </div>

    <AvatarCropper
      :visible="cropperVisible"
      :image-src="cropperImageSrc"
      title="裁剪店铺 LOGO"
      description="拖动或缩放图片，选择合适区域作为店铺 LOGO"
      confirm-text="上传 LOGO"
      file-name="shop-logo.jpg"
      @confirm="handleCropConfirm"
      @cancel="handleCropCancel"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import AvatarCropper from '@/components/account/AvatarCropper.vue'
import { createShop, uploadShopAvatar } from '@/api/shop'
import {
  buildCreateShopRequest,
  SHOP_AVATAR_MAX_SIZE,
  SHOP_DESC_MAX_LENGTH,
  SHOP_NAME_MAX_LENGTH,
  validateCreateShopForm,
} from '@/types/shop'
import { resolveFileUrl } from '@/utils/file'

const form = ref({
  shopName: '',
  shopAvatar: '',
  shopDesc: '',
})

const logoInputRef = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const uploadingLogo = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const logoErrorMsg = ref('')
const logoSuccessMsg = ref('')
const logoLoadFailed = ref(false)
const cropperVisible = ref(false)
const cropperImageSrc = ref('')

const logoPreview = computed(() => resolveFileUrl(form.value.shopAvatar))

function triggerLogoSelect() {
  if (uploadingLogo.value || submitting.value) return
  logoInputRef.value?.click()
}

function revokeCropperSrc() {
  if (cropperImageSrc.value.startsWith('blob:')) {
    URL.revokeObjectURL(cropperImageSrc.value)
  }
  cropperImageSrc.value = ''
}

function validateLogoFile(file: File): string | null {
  if (!file.type.startsWith('image/')) {
    return '请选择图片文件'
  }
  if (file.size > SHOP_AVATAR_MAX_SIZE) {
    return '图片大小不能超过 5MB'
  }
  return null
}

function handleLogoSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const validationError = validateLogoFile(file)
  if (validationError) {
    logoErrorMsg.value = validationError
    logoSuccessMsg.value = ''
    return
  }

  logoErrorMsg.value = ''
  logoSuccessMsg.value = ''
  logoLoadFailed.value = false
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

  const validationError = validateLogoFile(file)
  if (validationError) {
    logoErrorMsg.value = validationError
    return
  }

  logoErrorMsg.value = ''
  logoSuccessMsg.value = ''

  try {
    uploadingLogo.value = true
    const result = await uploadShopAvatar(file)
    form.value.shopAvatar = result.imageUrl
    logoLoadFailed.value = false
    logoSuccessMsg.value = '店铺 LOGO 上传成功'
  } catch (err) {
    logoErrorMsg.value = err instanceof Error ? err.message : '店铺 LOGO 上传失败'
  } finally {
    uploadingLogo.value = false
  }
}

function clearLogo() {
  form.value.shopAvatar = ''
  logoLoadFailed.value = false
  logoErrorMsg.value = ''
  logoSuccessMsg.value = ''
}

async function handleSubmit() {
  errorMsg.value = ''

  const validationError = validateCreateShopForm(form.value)
  if (validationError) {
    errorMsg.value = validationError
    return
  }

  try {
    submitting.value = true
    await createShop(buildCreateShopRequest(form.value))
    successMsg.value = '我们已收到您的开店申请，请耐心等待平台审核。'
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '开店申请提交失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
@import '@/styles/auth.css';

.create-shop-card {
  max-width: 560px;
}

.required {
  color: var(--taobao-orange);
  margin-right: 2px;
}

.shop-logo-field {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.shop-logo-preview {
  position: relative;
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 4px;
  overflow: hidden;
}

.shop-logo-preview:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.shop-logo-img {
  display: block;
  width: 120px;
  height: 120px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px dashed #ddd;
  background: #fafafa;
}

.shop-logo-img--placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #fafafa;
  color: #999;
}

.shop-logo-plus {
  font-size: 28px;
  line-height: 1;
  color: #ccc;
}

.shop-logo-placeholder-text {
  font-size: 12px;
}

.shop-logo-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.2s;
}

.shop-logo-preview:hover .shop-logo-mask {
  opacity: 1;
}

.shop-logo-actions {
  flex: 1;
  min-width: 0;
  padding-top: 8px;
}

.upload-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 34px;
  padding: 0 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  overflow: hidden;
}

.upload-btn:hover {
  color: var(--taobao-orange);
  border-color: var(--taobao-orange);
}

.upload-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.clear-btn {
  margin-left: 8px;
  padding: 0;
  border: none;
  background: none;
  font-size: 13px;
  color: #999;
  cursor: pointer;
}

.clear-btn:hover:not(:disabled) {
  color: var(--taobao-orange);
}

.clear-btn:disabled,
.upload-btn:has(input:disabled),
.shop-logo-preview:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

.form-success {
  margin: 8px 0 0;
  font-size: 12px;
  color: #52c41a;
  line-height: 1.5;
}

.form-error-inline {
  margin: 8px 0 0;
  font-size: 12px;
  color: #f5222d;
  line-height: 1.5;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}

.form-textarea:focus {
  border-color: var(--taobao-orange);
}

.create-shop-success {
  text-align: center;
  padding: 12px 0 8px;
}

.success-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.success-desc {
  margin: 0 0 28px;
  font-size: 14px;
  color: #666;
  line-height: 1.7;
}

.success-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 180px;
  text-decoration: none;
}
</style>
