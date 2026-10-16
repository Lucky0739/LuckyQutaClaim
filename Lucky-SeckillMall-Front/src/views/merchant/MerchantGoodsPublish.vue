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
          <span class="account-breadcrumb-current">发布商品</span>
        </nav>

        <div class="merchant-panel">
          <h1 class="merchant-panel-title">发布商品</h1>
          <p class="merchant-panel-desc">填写商品信息并上传图片，提交后等待平台审核</p>

          <div v-if="pageLoading" class="address-loading">
            <span class="address-loading-dot"></span>
            加载店铺与分类信息...
          </div>

          <div v-else-if="pageError" class="address-empty">
            <div class="address-empty-icon address-empty-icon--warn"></div>
            <p class="address-empty-text">{{ pageError }}</p>
            <button type="button" class="account-btn account-btn-primary" @click="loadPageData">
              重新加载
            </button>
          </div>

          <div v-else-if="successMsg" class="publish-success">
            <p class="publish-success-title">商品发布成功</p>
            <p class="publish-success-desc">{{ successMsg }}</p>
            <div class="publish-success-actions">
              <button type="button" class="account-btn account-btn-primary" @click="resetForm">
                继续发布
              </button>
              <router-link to="/merchant/goods" class="account-btn account-btn-secondary">
                商品管理
              </router-link>
            </div>
          </div>

          <form v-else class="publish-form" @submit.prevent="handleSubmit">
            <section class="publish-section">
              <h2 class="publish-section-title">基本信息</h2>

              <div class="account-form-row">
                <label class="account-form-label"><span class="required">*</span>商品名称</label>
                <div class="account-form-field publish-field-wide">
                  <input
                    v-model="form.goodsName"
                    type="text"
                    class="account-input"
                    :maxlength="GOODS_NAME_MAX_LENGTH"
                    placeholder="请输入商品名称"
                    :disabled="submitting"
                  />
                  <p class="account-tip">{{ form.goodsName.length }}/{{ GOODS_NAME_MAX_LENGTH }}</p>
                </div>
              </div>

              <div class="account-form-row">
                <label class="account-form-label"><span class="required">*</span>商品价格</label>
                <div class="account-form-field">
                  <div class="publish-price-input">
                    <span class="publish-price-prefix">¥</span>
                    <input
                      v-model="form.goodsPrice"
                      type="text"
                      inputmode="decimal"
                      class="account-input publish-price-field"
                      placeholder="0.00"
                      :disabled="submitting"
                    />
                  </div>
                  <p class="account-tip">展示价，SKU 未单独设置售价时将沿用此价格</p>
                </div>
              </div>

              <div class="account-form-row">
                <label class="account-form-label"><span class="required">*</span>所属店铺</label>
                <div class="account-form-field publish-field-wide">
                  <select
                    v-model="form.shopId"
                    class="account-input publish-select"
                    :disabled="submitting || shopOptions.length <= 1"
                  >
                    <option v-if="!shopOptions.length" value="">暂无店铺</option>
                    <option v-for="shop in shopOptions" :key="shop.id" :value="shop.id">
                      {{ shop.label }}
                    </option>
                  </select>
                  <p v-if="!shopOptions.length" class="account-tip account-tip--warn">
                    您还没有店铺，请先
                    <router-link to="/shop/create" class="publish-link">申请开店</router-link>
                  </p>
                  <p v-else class="account-tip">从您的店铺列表中选择商品所属店铺</p>
                </div>
              </div>

              <div class="account-form-row">
                <label class="account-form-label">商品分类</label>
                <div class="account-form-field publish-field-wide">
                  <div class="publish-category-row">
                    <select
                      v-model="parentCategoryId"
                      class="account-input publish-select"
                      :disabled="submitting || loadingCategories"
                      @change="handleParentCategoryChange"
                    >
                      <option value="">请选择一级分类</option>
                      <option v-for="item in parentCategories" :key="item.id" :value="item.id">
                        {{ item.categoryName }}
                      </option>
                    </select>
                    <select
                      v-model="form.categoryId"
                      class="account-input publish-select"
                      :disabled="submitting || !parentCategoryId || loadingChildren"
                    >
                      <option value="">请选择二级分类</option>
                      <option v-for="item in childCategories" :key="item.id" :value="item.id">
                        {{ item.categoryName }}
                      </option>
                    </select>
                  </div>
                  <p class="account-tip">选填，便于买家按分类浏览商品</p>
                </div>
              </div>
            </section>

            <section class="publish-section">
              <h2 class="publish-section-title">商品主图</h2>
              <p class="publish-section-tip">
                支持 JPG、PNG、WebP、GIF，单张不超过 5MB，最多 {{ GOODS_IMG_MAX_COUNT }} 张
              </p>

              <div class="publish-image-grid">
                <div
                  v-for="(imgUrl, index) in form.goodsImgList"
                  :key="`${imgUrl}-${index}`"
                  class="publish-image-item"
                >
                  <img
                    :src="resolveFileUrl(imgUrl)"
                    alt=""
                    class="publish-image-preview"
                    referrerpolicy="no-referrer"
                  />
                  <button
                    type="button"
                    class="publish-image-remove"
                    :disabled="submitting || uploadingMainIndex !== null"
                    @click="removeMainImage(index)"
                  >
                    删除
                  </button>
                </div>

                <label
                  v-if="form.goodsImgList.length < GOODS_IMG_MAX_COUNT"
                  class="publish-image-upload"
                  :class="{ 'publish-image-upload--disabled': submitting || uploadingMainIndex !== null }"
                >
                  <span class="publish-image-plus">+</span>
                  <span class="publish-image-upload-text">
                    {{ uploadingMainIndex !== null ? '上传中...' : '上传主图' }}
                  </span>
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp,image/gif"
                    :disabled="submitting || uploadingMainIndex !== null"
                    @change="handleMainImageSelect"
                  />
                </label>
              </div>
              <p v-if="mainImageError" class="account-msg account-msg--error publish-inline-error">
                {{ mainImageError }}
              </p>
            </section>

            <section class="publish-section">
              <div class="publish-section-head">
                <h2 class="publish-section-title">SKU 规格</h2>
                <button
                  type="button"
                  class="account-btn account-btn-secondary publish-add-sku-btn"
                  :disabled="submitting || uploadingSkuKey !== null"
                  @click="addSkuRow"
                >
                  添加规格
                </button>
              </div>
              <p class="publish-section-tip">选填，可配置不同规格的价格、库存与图片</p>

              <div v-if="!form.skuList.length" class="publish-sku-empty">
                暂无 SKU，可点击「添加规格」或直接发布单规格商品
              </div>

              <div v-for="(sku, index) in form.skuList" :key="sku.key" class="publish-sku-card">
                <div class="publish-sku-card-head">
                  <span class="publish-sku-card-title">规格 {{ index + 1 }}</span>
                  <button
                    type="button"
                    class="publish-sku-remove"
                    :disabled="submitting || uploadingSkuKey === sku.key"
                    @click="removeSkuRow(index)"
                  >
                    删除
                  </button>
                </div>

                <div class="publish-sku-grid">
                  <label class="publish-sku-field">
                    <span class="publish-sku-label">规格名称</span>
                    <input
                      v-model="sku.skuSpecName"
                      type="text"
                      class="account-input"
                      placeholder="如：红色 / XL"
                      :disabled="submitting"
                    />
                  </label>

                  <label class="publish-sku-field">
                    <span class="publish-sku-label">市场价</span>
                    <input
                      v-model="sku.marketPrice"
                      type="text"
                      inputmode="decimal"
                      class="account-input"
                      placeholder="选填"
                      :disabled="submitting"
                    />
                  </label>

                  <label class="publish-sku-field">
                    <span class="publish-sku-label">售价</span>
                    <input
                      v-model="sku.salePrice"
                      type="text"
                      inputmode="decimal"
                      class="account-input"
                      placeholder="选填"
                      :disabled="submitting"
                    />
                  </label>

                  <label class="publish-sku-field">
                    <span class="publish-sku-label">库存</span>
                    <input
                      v-model="sku.stockNum"
                      type="text"
                      inputmode="numeric"
                      class="account-input"
                      placeholder="0"
                      :disabled="submitting"
                    />
                  </label>
                </div>

                <div class="publish-sku-image-row">
                  <div v-if="sku.skuImg" class="publish-sku-image-preview">
                    <img
                      :src="resolveFileUrl(sku.skuImg)"
                      alt=""
                      referrerpolicy="no-referrer"
                    />
                  </div>
                  <label
                    class="publish-sku-upload-btn"
                    :class="{ 'publish-sku-upload-btn--disabled': submitting || uploadingSkuKey === sku.key }"
                  >
                    {{ uploadingSkuKey === sku.key ? '上传中...' : sku.skuImg ? '更换图片' : '上传 SKU 图' }}
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,image/gif"
                      :disabled="submitting || uploadingSkuKey === sku.key"
                      @change="handleSkuImageSelect($event, sku.key)"
                    />
                  </label>
                  <button
                    v-if="sku.skuImg"
                    type="button"
                    class="publish-sku-clear-image"
                    :disabled="submitting || uploadingSkuKey === sku.key"
                    @click="clearSkuImage(sku.key)"
                  >
                    移除图片
                  </button>
                </div>
              </div>
              <p v-if="skuImageError" class="account-msg account-msg--error publish-inline-error">
                {{ skuImageError }}
              </p>
            </section>

            <section class="publish-section">
              <h2 class="publish-section-title">商品详情</h2>
              <div class="account-form-row publish-detail-row">
                <label class="account-form-label">详情描述</label>
                <div class="account-form-field publish-field-wide">
                  <textarea
                    v-model="form.detailContent"
                    class="publish-textarea"
                    rows="8"
                    :maxlength="GOODS_DETAIL_MAX_LENGTH"
                    placeholder="介绍商品卖点、材质、尺寸等信息（选填）"
                    :disabled="submitting"
                  />
                  <p class="account-tip">{{ form.detailContent.length }}/{{ GOODS_DETAIL_MAX_LENGTH }}</p>
                </div>
              </div>
            </section>

            <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>

            <div class="publish-actions">
              <button
                type="submit"
                class="account-btn account-btn-primary"
                :disabled="submitting || uploadingMainIndex !== null || uploadingSkuKey !== null || !shopOptions.length"
              >
                {{ submitting ? '提交中...' : '发布商品' }}
              </button>
              <router-link to="/merchant/goods" class="account-btn account-btn-secondary">
                取消
              </router-link>
            </div>
          </form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Header from '@/components/Header.vue'
import { addGoods, uploadGoodsImg } from '@/api/goods'
import { getCategoryChildren, getParentCategories } from '@/api/category'
import { getMyShopList } from '@/api/shop'
import type { GoodsCategory } from '@/types/category'
import {
  buildAddGoodsRequest,
  createEmptySkuFormItem,
  GOODS_DETAIL_MAX_LENGTH,
  GOODS_IMG_MAX_COUNT,
  GOODS_NAME_MAX_LENGTH,
  validateGoodsImageFile,
  validatePublishGoodsForm,
  type GoodsSkuFormItem,
} from '@/types/goods'
import { resolveFileUrl } from '@/utils/file'

const form = ref({
  goodsName: '',
  goodsPrice: '',
  shopId: '',
  categoryId: '',
  goodsImgList: [] as string[],
  detailContent: '',
  skuList: [] as GoodsSkuFormItem[],
})

const parentCategoryId = ref('')
const parentCategories = ref<GoodsCategory[]>([])
const childCategories = ref<GoodsCategory[]>([])
const shopOptions = ref<{ id: string; label: string }[]>([])

const pageLoading = ref(true)
const pageError = ref('')
const loadingCategories = ref(false)
const loadingChildren = ref(false)
const submitting = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const mainImageError = ref('')
const skuImageError = ref('')
const uploadingMainIndex = ref<number | null>(null)
const uploadingSkuKey = ref<string | null>(null)

function resetFormState() {
  form.value = {
    goodsName: '',
    goodsPrice: '',
    shopId: shopOptions.value[0]?.id ?? '',
    categoryId: '',
    goodsImgList: [],
    detailContent: '',
    skuList: [],
  }
  parentCategoryId.value = ''
  childCategories.value = []
  errorMsg.value = ''
  mainImageError.value = ''
  skuImageError.value = ''
}

function resetForm() {
  successMsg.value = ''
  resetFormState()
}

function buildShopOptionLabel(shop: {
  shopName: string
  auditStatusDesc?: string
  shopStatusDesc?: string
}): string {
  const tags = [shop.auditStatusDesc, shop.shopStatusDesc].filter(Boolean)
  if (!tags.length) return shop.shopName
  return `${shop.shopName}（${tags.join(' / ')}）`
}

async function loadShops() {
  const page = await getMyShopList()
  shopOptions.value = page.records.map((shop) => ({
    id: shop.id,
    label: buildShopOptionLabel(shop),
  }))

  if (!form.value.shopId && shopOptions.value.length) {
    form.value.shopId = shopOptions.value[0].id
  }
}

async function loadCategories() {
  loadingCategories.value = true
  try {
    parentCategories.value = await getParentCategories()
  } finally {
    loadingCategories.value = false
  }
}

async function loadPageData() {
  pageLoading.value = true
  pageError.value = ''
  try {
    await Promise.all([loadShops(), loadCategories()])
  } catch (err) {
    pageError.value = err instanceof Error ? err.message : '加载页面数据失败'
  } finally {
    pageLoading.value = false
  }
}

async function handleParentCategoryChange() {
  form.value.categoryId = ''
  childCategories.value = []
  if (!parentCategoryId.value) return

  loadingChildren.value = true
  try {
    childCategories.value = await getCategoryChildren(parentCategoryId.value)
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载子分类失败'
  } finally {
    loadingChildren.value = false
  }
}

async function uploadImage(file: File) {
  const result = await uploadGoodsImg(file)
  return result.imageUrl
}

async function handleMainImageSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const validationError = validateGoodsImageFile(file)
  if (validationError) {
    mainImageError.value = validationError
    return
  }

  if (form.value.goodsImgList.length >= GOODS_IMG_MAX_COUNT) {
    mainImageError.value = `最多上传 ${GOODS_IMG_MAX_COUNT} 张主图`
    return
  }

  mainImageError.value = ''
  uploadingMainIndex.value = form.value.goodsImgList.length

  try {
    const imageUrl = await uploadImage(file)
    form.value.goodsImgList.push(imageUrl)
  } catch (err) {
    mainImageError.value = err instanceof Error ? err.message : '主图上传失败'
  } finally {
    uploadingMainIndex.value = null
  }
}

function removeMainImage(index: number) {
  form.value.goodsImgList.splice(index, 1)
  mainImageError.value = ''
}

async function handleSkuImageSelect(event: Event, skuKey: string) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  const validationError = validateGoodsImageFile(file)
  if (validationError) {
    skuImageError.value = validationError
    return
  }

  skuImageError.value = ''
  uploadingSkuKey.value = skuKey

  try {
    const imageUrl = await uploadImage(file)
    const sku = form.value.skuList.find((item) => item.key === skuKey)
    if (sku) sku.skuImg = imageUrl
  } catch (err) {
    skuImageError.value = err instanceof Error ? err.message : 'SKU 图片上传失败'
  } finally {
    uploadingSkuKey.value = null
  }
}

function clearSkuImage(skuKey: string) {
  const sku = form.value.skuList.find((item) => item.key === skuKey)
  if (sku) sku.skuImg = ''
  skuImageError.value = ''
}

function addSkuRow() {
  form.value.skuList.push(createEmptySkuFormItem())
}

function removeSkuRow(index: number) {
  form.value.skuList.splice(index, 1)
  skuImageError.value = ''
}

async function handleSubmit() {
  errorMsg.value = ''

  const validationError = validatePublishGoodsForm(form.value)
  if (validationError) {
    errorMsg.value = validationError
    return
  }

  try {
    submitting.value = true
    const payload = buildAddGoodsRequest(form.value)
    const goodsId = await addGoods(payload)
    successMsg.value = goodsId
      ? `商品已提交审核，商品 ID：${goodsId}`
      : '商品已提交审核，请等待平台审核通过后上架。'
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '发布商品失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPageData()
})
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
  padding: 24px 32px 40px;
}

.merchant-panel-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.merchant-panel-desc {
  margin: 0 0 24px;
  font-size: 13px;
  color: #999;
}

.required {
  color: var(--accent);
  margin-right: 2px;
}

.publish-form {
  max-width: 860px;
}

.publish-section {
  margin-bottom: 32px;
  padding-bottom: 28px;
  border-bottom: 1px solid #f0f0f0;
}

.publish-section:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.publish-section-title {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.publish-section-tip {
  margin: 0 0 16px;
  font-size: 12px;
  color: #999;
}

.publish-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.publish-field-wide {
  max-width: 100%;
}

.publish-price-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.publish-price-prefix {
  font-size: 14px;
  color: #666;
}

.publish-price-field {
  max-width: 180px;
}

.publish-select {
  width: 100%;
  max-width: 360px;
  cursor: pointer;
}

.publish-category-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.publish-category-row .publish-select {
  flex: 1 1 200px;
}

.publish-link {
  color: var(--accent);
  text-decoration: none;
}

.publish-link:hover {
  text-decoration: underline;
}

.account-tip--warn {
  color: #fa8c16;
}

.publish-image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.publish-image-item,
.publish-image-upload {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 4px;
  overflow: hidden;
}

.publish-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 1px solid #eee;
  background: #fafafa;
}

.publish-image-remove {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 28px;
  border: none;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  font-size: 12px;
  cursor: pointer;
}

.publish-image-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px dashed #ddd;
  background: #fafafa;
  color: #999;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s;
}

.publish-image-upload:hover:not(.publish-image-upload--disabled) {
  border-color: var(--accent);
  color: var(--accent);
}

.publish-image-upload--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.publish-image-upload input {
  display: none;
}

.publish-image-plus {
  font-size: 28px;
  line-height: 1;
}

.publish-image-upload-text {
  font-size: 12px;
}

.publish-sku-empty {
  padding: 16px;
  font-size: 13px;
  color: #999;
  background: #fafafa;
  border: 1px dashed #e5e5e5;
  border-radius: 4px;
}

.publish-sku-card {
  margin-top: 12px;
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 4px;
  background: #fafafa;
}

.publish-sku-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.publish-sku-card-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.publish-sku-remove,
.publish-sku-clear-image {
  border: none;
  background: none;
  font-size: 12px;
  color: #999;
  cursor: pointer;
}

.publish-sku-remove:hover:not(:disabled),
.publish-sku-clear-image:hover:not(:disabled) {
  color: var(--accent);
}

.publish-sku-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.publish-sku-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.publish-sku-label {
  font-size: 12px;
  color: #666;
}

.publish-sku-image-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
}

.publish-sku-image-preview {
  width: 72px;
  height: 72px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #eee;
}

.publish-sku-image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.publish-sku-upload-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 14px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  overflow: hidden;
}

.publish-sku-upload-btn:hover:not(.publish-sku-upload-btn--disabled) {
  color: var(--accent);
  border-color: var(--accent);
}

.publish-sku-upload-btn--disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.publish-sku-upload-btn input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.publish-detail-row {
  max-width: 100%;
}

.publish-textarea {
  width: 100%;
  min-height: 180px;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  line-height: 1.6;
  resize: vertical;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}

.publish-textarea:focus {
  border-color: var(--accent);
}

.publish-inline-error {
  margin-top: 12px;
}

.publish-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.publish-add-sku-btn {
  height: 32px;
  padding: 0 14px;
  font-size: 12px;
}

.publish-success {
  padding: 48px 0 24px;
  text-align: center;
}

.publish-success-title {
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.publish-success-desc {
  margin: 0 0 28px;
  font-size: 14px;
  color: #666;
}

.publish-success-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 768px) {
  .merchant-panel {
    padding: 20px 16px 32px;
  }

  .publish-sku-grid {
    grid-template-columns: 1fr;
  }
}
</style>
