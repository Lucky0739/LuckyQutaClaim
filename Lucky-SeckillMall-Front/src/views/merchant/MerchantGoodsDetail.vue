<template>
  <div class="page">
    <Header top-only />

    <main class="goods-detail-page">
      <div class="goods-detail-inner">
        <nav class="account-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <router-link to="/merchant/goods">商品管理</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span class="account-breadcrumb-current">商品详情</span>
        </nav>

        <section v-if="loading" class="goods-detail-state">加载商品详情...</section>
        <section v-else-if="errorMsg && !goods" class="goods-detail-state goods-detail-state--error">
          <p>{{ errorMsg }}</p>
          <button type="button" class="filter-btn" @click="loadDetail">重新加载</button>
        </section>

        <section v-else-if="goods" class="goods-detail-panel">
          <header class="goods-detail-head">
            <div>
              <h1 class="goods-detail-title">{{ goods.goodsName }}</h1>
              <p class="goods-detail-id">商品 ID：{{ goods.id }}</p>
              <div class="goods-detail-tags">
                <span class="status-tag" :class="getStatusClass(goods.status)">
                  {{ formatGoodsStatus(goods.status) }}
                </span>
                <span class="audit-tag" :class="getAuditClass(goods.auditStatus)">
                  {{ formatMerchantGoodsAuditStatus(goods.auditStatus) }}
                </span>
              </div>
            </div>
            <div class="goods-detail-actions">
              <router-link to="/merchant/goods" class="detail-btn detail-btn--ghost">返回列表</router-link>
              <button
                v-if="canMerchantApplyOnShelf(goods)"
                type="button"
                class="detail-btn detail-btn--apply"
                :disabled="actioning"
                @click="showApplyDialog = true"
              >
                <span class="detail-btn-icon" aria-hidden="true">↑</span>
                申请上架
              </button>
              <button
                v-if="canMerchantOffShelf(goods)"
                type="button"
                class="detail-btn detail-btn--off"
                :disabled="actioning"
                @click="handleOffShelf"
              >
                {{ actioning ? '处理中...' : '下架商品' }}
              </button>
              <button
                type="button"
                class="detail-btn detail-btn--danger"
                :disabled="deleting || actioning || goods.status === GoodsStatus.ON_SHELF"
                :title="goods.status === GoodsStatus.ON_SHELF ? '请先下架再删除' : ''"
                @click="handleDelete"
              >
                {{ deleting ? '删除中...' : '删除商品' }}
              </button>
            </div>
          </header>

          <p v-if="errorMsg" class="goods-detail-msg goods-detail-msg--error">{{ errorMsg }}</p>
          <p v-if="successMsg" class="goods-detail-msg goods-detail-msg--success">{{ successMsg }}</p>

          <div class="goods-detail-body">
            <div class="goods-detail-gallery">
              <div v-if="imageList.length" class="goods-detail-images">
                <img
                  v-for="(img, index) in imageList"
                  :key="`${img}-${index}`"
                  :src="resolveFileUrl(img)"
                  alt=""
                  class="goods-detail-img"
                  referrerpolicy="no-referrer"
                />
              </div>
              <div v-else class="goods-detail-images goods-detail-images--empty">暂无图片</div>
              <p class="goods-detail-price">
                售价 <strong>¥{{ formatGoodsPrice(goods.goodsPrice) }}</strong>
              </p>
            </div>

            <div class="goods-detail-main">
              <section v-if="goods.skuList?.length" class="goods-detail-block">
                <h2>规格与库存</h2>
                <table class="sku-table">
                  <thead>
                    <tr>
                      <th>规格</th>
                      <th>市场价</th>
                      <th>售价</th>
                      <th>库存</th>
                      <th>销量</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(sku, index) in goods.skuList" :key="sku.id || index">
                      <td>
                        <div class="sku-cell">
                          <img
                            v-if="sku.skuImg"
                            :src="resolveFileUrl(sku.skuImg)"
                            alt=""
                            class="sku-thumb"
                            referrerpolicy="no-referrer"
                          />
                          <span>{{ sku.skuSpecName || '默认规格' }}</span>
                        </div>
                      </td>
                      <td>¥{{ formatGoodsPrice(sku.marketPrice) }}</td>
                      <td>¥{{ formatGoodsPrice(sku.salePrice ?? goods.goodsPrice) }}</td>
                      <td>{{ sku.stockNum ?? 0 }}</td>
                      <td>{{ sku.salesNum ?? 0 }}</td>
                    </tr>
                  </tbody>
                </table>
              </section>

              <section class="goods-detail-block">
                <h2>商品详情</h2>
                <p v-if="goods.detailContent" class="goods-detail-content">{{ goods.detailContent }}</p>
                <p v-else class="goods-detail-content goods-detail-content--empty">暂无详情描述</p>
              </section>
            </div>
          </div>
        </section>
      </div>
    </main>

    <ApplyOnShelfConfirmDialog
      v-model="showApplyDialog"
      :goods="goods"
      :loading="actioning"
      @confirm="confirmApplyOnShelf"
      @cancel="showApplyDialog = false"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ApplyOnShelfConfirmDialog from '@/components/merchant/ApplyOnShelfConfirmDialog.vue'
import Header from '@/components/Header.vue'
import { applyOnShelf, deleteMyGoods, getMyGoodsDetail, offShelf } from '@/api/goods'
import {
  canMerchantApplyOnShelf,
  canMerchantOffShelf,
  formatGoodsPrice,
  formatMerchantGoodsAuditStatus,
  GoodsStatus,
  MerchantGoodsAuditStatus,
  type GoodsDetailVO,
} from '@/types/goods'
import { resolveFileUrl } from '@/utils/file'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const deleting = ref(false)
const actioning = ref(false)
const showApplyDialog = ref(false)
const errorMsg = ref('')
const successMsg = ref('')
const goods = ref<GoodsDetailVO | null>(null)

const goodsId = computed(() => String(route.params.id ?? ''))

const imageList = computed(() => {
  if (!goods.value) return []
  const list = goods.value.goodsImgList?.filter(Boolean) ?? []
  if (list.length) return list
  return goods.value.goodsImg ? [goods.value.goodsImg] : []
})

async function loadDetail() {
  const id = goodsId.value
  if (!id) {
    errorMsg.value = '商品 ID 无效'
    goods.value = null
    return
  }

  loading.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    goods.value = await getMyGoodsDetail(id)
  } catch (err) {
    goods.value = null
    errorMsg.value = err instanceof Error ? err.message : '加载商品详情失败'
  } finally {
    loading.value = false
  }
}

async function confirmApplyOnShelf() {
  if (!goods.value || actioning.value || !canMerchantApplyOnShelf(goods.value)) return

  actioning.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await applyOnShelf(goods.value.id)
    showApplyDialog.value = false
    successMsg.value = '上架申请已提交，请等待管理员审核'
    await loadDetail()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '申请上架失败'
  } finally {
    actioning.value = false
  }
}

async function handleOffShelf() {
  if (!goods.value || actioning.value || !canMerchantOffShelf(goods.value)) return
  const name = goods.value.goodsName || '该商品'
  if (!window.confirm(`确定将「${name}」下架吗？下架后买家将无法购买。`)) return

  actioning.value = true
  errorMsg.value = ''
  successMsg.value = ''
  try {
    await offShelf(goods.value.id)
    successMsg.value = '商品已下架'
    await loadDetail()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '下架失败'
  } finally {
    actioning.value = false
  }
}

async function handleDelete() {
  if (!goods.value || deleting.value) return
  if (goods.value.status === GoodsStatus.ON_SHELF) return

  const name = goods.value.goodsName || '该商品'
  if (!window.confirm(`确定删除「${name}」吗？删除后不可恢复。`)) return

  deleting.value = true
  errorMsg.value = ''
  try {
    await deleteMyGoods(goods.value.id)
    await router.push({ name: 'MerchantGoods' })
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '删除商品失败'
  } finally {
    deleting.value = false
  }
}

function formatGoodsStatus(status?: number) {
  if (status === GoodsStatus.ON_SHELF) return '上架'
  if (status === GoodsStatus.OFF_SHELF) return '下架'
  return '--'
}

function getStatusClass(status?: number) {
  return {
    'status-tag--on': status === GoodsStatus.ON_SHELF,
    'status-tag--off': status !== GoodsStatus.ON_SHELF,
  }
}

function getAuditClass(status?: number) {
  return {
    'audit-tag--pending':
      status === MerchantGoodsAuditStatus.PENDING ||
      status === MerchantGoodsAuditStatus.NOT_APPLIED,
    'audit-tag--approved': status === MerchantGoodsAuditStatus.PASS,
    'audit-tag--rejected': status === MerchantGoodsAuditStatus.REJECT,
  }
}

watch(goodsId, loadDetail)
onMounted(loadDetail)
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.goods-detail-page {
  padding: 16px 0 48px;
}

.goods-detail-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 0 10px;
}

.goods-detail-panel {
  padding: 24px 28px 32px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
}

.goods-detail-state {
  padding: 56px 0;
  text-align: center;
  color: #999;
}

.goods-detail-state--error {
  color: #f5222d;
}

.goods-detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.goods-detail-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #333;
}

.goods-detail-id {
  margin: 0 0 12px;
  font-size: 12px;
  color: #999;
}

.goods-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.goods-detail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.goods-detail-body {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 28px;
}

.goods-detail-images {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.goods-detail-images--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  color: #bbb;
}

.goods-detail-img {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eee;
}

.goods-detail-price {
  margin: 14px 0 0;
  font-size: 14px;
  color: #666;
}

.goods-detail-price strong {
  color: var(--accent);
  font-size: 20px;
}

.goods-detail-block {
  margin-bottom: 24px;
}

.goods-detail-block h2 {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.sku-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.sku-table th,
.sku-table td {
  padding: 10px 12px;
  border: 1px solid #f0f0f0;
  text-align: left;
}

.sku-table th {
  background: #fafafa;
  color: #666;
}

.sku-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sku-thumb {
  width: 36px;
  height: 36px;
  border-radius: 6px;
  object-fit: cover;
}

.goods-detail-content {
  margin: 0;
  line-height: 1.7;
  color: #555;
  white-space: pre-wrap;
}

.goods-detail-content--empty {
  color: #999;
}

.filter-btn {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #dddddd;
  border-radius: 6px;
  background: #fff;
  color: #555;
  cursor: pointer;
}

.detail-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  cursor: pointer;
  transition:
    transform 0.15s,
    box-shadow 0.15s,
    border-color 0.15s,
    background 0.15s;
}

.detail-btn-icon {
  font-size: 14px;
}

.detail-btn--ghost {
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
}

.detail-btn--ghost:hover {
  border-color: #ccc;
  background: #fafafa;
}

.detail-btn--apply {
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  box-shadow: 0 6px 14px rgba(255, 80, 0, 0.22);
}

.detail-btn--apply:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(255, 80, 0, 0.28);
}

.detail-btn--off {
  border: 1px solid #ffd8bf;
  background: #fff;
  color: #fa8c16;
}

.detail-btn--off:hover:not(:disabled) {
  background: #fff7e6;
}

.detail-btn--danger {
  border: 1px solid #ffccc7;
  background: #fff;
  color: #f5222d;
}

.detail-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none;
}

.goods-detail-msg {
  margin: 0 0 16px;
  font-size: 13px;
}

.goods-detail-msg--error {
  color: #f5222d;
}

.goods-detail-msg--success {
  color: #52c41a;
}

.filter-btn--danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.status-tag,
.audit-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
}

.status-tag--on,
.audit-tag--approved {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.status-tag--off {
  border: 1px solid #d9d9d9;
  background: #f5f5f5;
  color: #999;
}

.audit-tag--pending {
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #d48806;
}

.audit-tag--rejected {
  border: 1px solid #ffccc7;
  background: #fff2f0;
  color: #f5222d;
}

@media (max-width: 900px) {
  .goods-detail-body {
    grid-template-columns: 1fr;
  }

  .goods-detail-head {
    flex-direction: column;
  }
}
</style>
