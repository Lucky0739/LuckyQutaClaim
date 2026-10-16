<template>
  <div class="page">
    <Header top-only />

    <main class="seckill-apply-page">
      <div class="seckill-apply-inner">
        <nav class="account-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span>商家中心</span>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span class="account-breadcrumb-current">秒杀商品申请</span>
        </nav>

        <section class="seckill-apply-panel">
          <div class="seckill-apply-head">
            <div>
              <h1 class="seckill-apply-title">秒杀商品申请</h1>
              <p class="seckill-apply-desc">
                选择未开始的秒杀活动，提交自家上架商品参与秒杀；审核通过后活动开始即可售卖。
              </p>
            </div>
          </div>

          <div class="seckill-apply-tabs">
            <button
              type="button"
              class="seckill-apply-tab"
              :class="{ 'seckill-apply-tab--active': activeTab === 'apply' }"
              @click="switchTab('apply')"
            >
              提交申请
            </button>
            <button
              type="button"
              class="seckill-apply-tab"
              :class="{ 'seckill-apply-tab--active': activeTab === 'records' }"
              @click="switchTab('records')"
            >
              我的申请
            </button>
          </div>

          <div v-if="pageLoading" class="seckill-apply-state">加载页面数据...</div>
          <div v-else-if="pageError" class="seckill-apply-state seckill-apply-state--error">
            <p>{{ pageError }}</p>
            <button type="button" class="filter-btn filter-btn--primary" @click="loadPageData">
              重新加载
            </button>
          </div>

          <template v-else-if="activeTab === 'apply'">
            <div v-if="!applyableActivities.length" class="apply-empty-banner">
              <p>当前没有「未开始」的秒杀活动，暂时无法提交申请。</p>
              <p class="apply-empty-banner-tip">请联系管理员在「秒杀活动管理」中创建活动并保持未开始状态。</p>
            </div>

            <form class="apply-form" @submit.prevent="submitApply">
              <p v-if="applyErrorMsg" class="apply-msg apply-msg--error">{{ applyErrorMsg }}</p>

              <div v-if="selectedActivity" class="apply-activity-info">
                <span class="apply-activity-info-label">当前活动</span>
                <span>{{ selectedActivity.activityName }}</span>
                <span v-if="selectedActivity.beginTime" class="apply-activity-info-time">
                  {{ formatDateTime(selectedActivity.beginTime) }}
                  <template v-if="selectedActivity.endTime"> ~ {{ formatDateTime(selectedActivity.endTime) }}</template>
                </span>
              </div>

              <div class="account-form-row">
                <label class="account-form-label"><span class="required">*</span>秒杀活动</label>
                <div class="account-form-field apply-field-wide">
                  <select
                    v-model="formActivityId"
                    class="account-input"
                    :disabled="applySubmitting || activityApplicationsLoading"
                  >
                    <option value="">请选择活动</option>
                    <option
                      v-for="activity in applyableActivities"
                      :key="activity.id"
                      :value="activity.id"
                    >
                      {{ formatActivityOption(activity) }}
                    </option>
                  </select>
                </div>
              </div>

              <div class="account-form-row">
                <label class="account-form-label"><span class="required">*</span>选择商品</label>
                <div class="account-form-field apply-field-wide">
                  <select
                    v-model="formGoodsId"
                    class="account-input"
                    :disabled="applySubmitting || !eligibleGoods.length"
                    @change="formSeckillTitle = ''"
                  >
                    <option value="">请选择已上架且审核通过的商品</option>
                    <option v-for="goods in eligibleGoods" :key="goods.id" :value="goods.id">
                      {{ goods.goodsName }}（ID: {{ goods.id }}）
                    </option>
                  </select>
                  <p v-if="!eligibleGoods.length" class="account-tip">
                    暂无符合条件的商品，请先发布商品并通过上架审核。
                  </p>
                </div>
              </div>

              <div class="account-form-row">
                <label class="account-form-label"><span class="required">*</span>商品规格</label>
                <div class="account-form-field apply-field-wide">
                  <select
                    v-model="formSkuId"
                    class="account-input"
                    :disabled="applySubmitting || skuLoading || !formGoodsId || !skuList.length"
                  >
                    <option value="">
                      {{
                        skuLoading
                          ? '加载规格中...'
                          : !formGoodsId
                            ? '请先选择商品'
                            : !skuList.length
                              ? '该商品暂无可用规格'
                              : !availableSkuList.length
                                ? '该活动下规格均已申请'
                                : '请选择 SKU'
                      }}
                    </option>
                    <option v-for="sku in availableSkuList" :key="sku.id" :value="sku.id">
                      {{ sku.skuSpecName || '默认规格' }}
                      <template v-if="sku.salePrice != null"> · ¥{{ formatSeckillPrice(sku.salePrice) }}</template>
                      <template v-if="sku.stockNum != null"> · 库存 {{ sku.stockNum }}</template>
                    </option>
                  </select>
                  <p v-if="selectedSku?.salePrice != null" class="account-tip">
                    SKU 售价 ¥{{ formatSeckillPrice(selectedSku.salePrice) }}，秒杀价须低于售价
                  </p>
                  <p v-if="selectedSku?.stockNum != null" class="account-tip">
                    SKU 可用库存 {{ selectedSku.stockNum }}，秒杀库存不可超过该值
                  </p>
                </div>
              </div>

              <div class="account-form-row">
                <label class="account-form-label">秒杀标题</label>
                <div class="account-form-field apply-field-wide">
                  <input
                    v-model="formSeckillTitle"
                    type="text"
                    class="account-input"
                    maxlength="100"
                    placeholder="留空则使用商品名称"
                    :disabled="applySubmitting"
                  />
                </div>
              </div>

              <div class="apply-form-grid">
                <div class="account-form-row">
                  <label class="account-form-label"><span class="required">*</span>秒杀价</label>
                  <div class="account-form-field">
                    <div class="apply-price-input">
                      <span>¥</span>
                      <input
                        v-model="formSeckillPrice"
                        type="text"
                        inputmode="decimal"
                        class="account-input"
                        placeholder="0.00"
                        :disabled="applySubmitting"
                      />
                    </div>
                  </div>
                </div>

                <div class="account-form-row">
                  <label class="account-form-label"><span class="required">*</span>秒杀库存</label>
                  <div class="account-form-field">
                    <input
                      v-model="formSeckillTotal"
                      type="text"
                      inputmode="numeric"
                      class="account-input"
                      placeholder="请输入库存"
                      :disabled="applySubmitting"
                    />
                  </div>
                </div>

                <div class="account-form-row">
                  <label class="account-form-label"><span class="required">*</span>限购数量</label>
                  <div class="account-form-field">
                    <input
                      v-model="formSeckillLimit"
                      type="text"
                      inputmode="numeric"
                      class="account-input"
                      :disabled="applySubmitting"
                    />
                  </div>
                </div>

                <div class="account-form-row">
                  <label class="account-form-label">排序值</label>
                  <div class="account-form-field">
                    <input
                      v-model="formSort"
                      type="text"
                      inputmode="numeric"
                      class="account-input"
                      :disabled="applySubmitting"
                    />
                  </div>
                </div>
              </div>

              <div v-if="selectedGoods" class="apply-preview">
                <img
                  v-if="seckillImgPreview"
                  :src="resolveFileUrl(seckillImgPreview)"
                  alt=""
                  class="apply-preview-img"
                  referrerpolicy="no-referrer"
                />
                <div v-else class="apply-preview-img apply-preview-img--placeholder">图</div>
                <div>
                  <p class="apply-preview-title">{{ formSeckillTitle || selectedGoods.goodsName }}</p>
                  <p class="apply-preview-meta">
                    商品 ID {{ selectedGoods.id }}
                    <template v-if="selectedSku?.skuSpecName"> · {{ selectedSku.skuSpecName }}</template>
                  </p>
                  <p v-if="seckillImgPreview" class="apply-preview-meta">将使用 SKU/商品主图作为秒杀展示图</p>
                </div>
              </div>

              <div class="apply-actions">
                <p v-if="applyFormHint && !applyErrorMsg" class="apply-msg apply-msg--hint">{{ applyFormHint }}</p>
                <button type="button" class="filter-btn" :disabled="applySubmitting" @click="resetApplyForm">
                  重置
                </button>
                <button type="submit" class="filter-btn filter-btn--primary" :disabled="applySubmitting">
                  {{ applySubmitting ? '提交中...' : '提交申请' }}
                </button>
              </div>
            </form>
          </template>

          <template v-else>
            <form class="records-filter" @submit.prevent="handleRecordsSearch">
              <label class="records-filter-item">
                <span>活动</span>
                <select v-model="filterActivityId" class="account-input">
                  <option value="">全部活动</option>
                  <option v-for="activity in activities" :key="activity.id" :value="activity.id">
                    {{ activity.activityName }}
                  </option>
                </select>
              </label>
              <label class="records-filter-item">
                <span>审核状态</span>
                <select v-model="filterAuditStatus" class="account-input">
                  <option value="">全部</option>
                  <option :value="SeckillGoodsAuditStatus.PENDING">待审核</option>
                  <option :value="SeckillGoodsAuditStatus.APPROVED">已通过</option>
                  <option :value="SeckillGoodsAuditStatus.REJECTED">已拒绝</option>
                </select>
              </label>
              <div class="records-filter-actions">
                <button type="submit" class="filter-btn filter-btn--primary" :disabled="recordsLoading">
                  查询
                </button>
                <button type="button" class="filter-btn" :disabled="recordsLoading" @click="handleRecordsReset">
                  重置
                </button>
              </div>
            </form>

            <p v-if="recordsErrorMsg" class="apply-msg apply-msg--error">{{ recordsErrorMsg }}</p>

            <div v-if="recordsLoading" class="seckill-apply-state">加载申请记录...</div>
            <div v-else-if="!hasRecords" class="seckill-apply-state">
              <p>暂无秒杀商品申请记录</p>
              <button type="button" class="filter-btn filter-btn--primary" @click="switchTab('apply')">
                去提交申请
              </button>
            </div>

            <div v-else class="records-table-wrap">
              <table class="records-table">
                <thead>
                  <tr>
                    <th>秒杀商品</th>
                    <th>活动</th>
                    <th>秒杀价</th>
                    <th>库存</th>
                    <th>审核状态</th>
                    <th>申请时间</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in records" :key="item.id">
                    <td>
                      <div class="records-goods-cell">
                        <img
                          v-if="item.seckillImg"
                          :src="resolveFileUrl(item.seckillImg)"
                          alt=""
                          class="records-goods-img"
                          referrerpolicy="no-referrer"
                        />
                        <span v-else class="records-goods-img records-goods-img--placeholder">图</span>
                        <div>
                          <p class="records-goods-name">{{ item.seckillTitle || '--' }}</p>
                          <p class="records-goods-meta">
                            商品 {{ item.goodsId }} · {{ item.skuSpecName || '默认规格' }}
                          </p>
                          <p v-if="item.auditRemark" class="records-goods-remark">备注：{{ item.auditRemark }}</p>
                        </div>
                      </div>
                    </td>
                    <td>{{ item.activityName || item.activityId }}</td>
                    <td class="records-price">¥{{ formatSeckillPrice(item.seckillPrice) }}</td>
                    <td>{{ item.seckillStock ?? item.seckillTotal ?? '--' }}</td>
                    <td>
                      <span class="audit-tag" :class="getAuditClass(item.auditStatus)">
                        {{ formatSeckillGoodsAuditStatus(item.auditStatus) }}
                      </span>
                    </td>
                    <td>{{ formatDateTime(item.createTime) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div v-if="hasRecords && totalPages > 1" class="records-pagination">
              <button type="button" class="filter-btn" :disabled="!hasPrevPage || recordsLoading" @click="goPrevPage">
                上一页
              </button>
              <span>第 {{ pageNum }} / {{ totalPages }} 页，共 {{ total }} 条</span>
              <button type="button" class="filter-btn" :disabled="!hasNextPage || recordsLoading" @click="goNextPage">
                下一页
              </button>
            </div>
          </template>
        </section>
      </div>
    </main>

    <Teleport to="body">
      <Transition name="address-toast-fade">
        <div v-if="toastMsg" class="address-success-toast">{{ toastMsg }}</div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import Header from '@/components/Header.vue'
import { useMerchantSeckillApply } from '@/composables/useMerchantSeckillApply'
import { resolveFileUrl } from '@/utils/file'

const {
  activeTab,
  pageLoading,
  pageError,
  toastMsg,
  activities,
  applyableActivities,
  eligibleGoods,
  skuList,
  availableSkuList,
  skuLoading,
  activityApplicationsLoading,
  formActivityId,
  formGoodsId,
  formSkuId,
  formSeckillTitle,
  formSeckillPrice,
  formSeckillTotal,
  formSeckillLimit,
  formSort,
  applySubmitting,
  applyErrorMsg,
  applyFormHint,
  selectedActivity,
  selectedGoods,
  selectedSku,
  seckillImgPreview,
  recordsLoading,
  recordsErrorMsg,
  records,
  pageNum,
  total,
  totalPages,
  filterActivityId,
  filterAuditStatus,
  hasRecords,
  hasPrevPage,
  hasNextPage,
  loadPageData,
  submitApply,
  resetApplyForm,
  switchTab,
  handleRecordsSearch,
  handleRecordsReset,
  goPrevPage,
  goNextPage,
  formatDateTime,
  formatActivityOption,
  formatSeckillGoodsAuditStatus,
  formatSeckillPrice,
  SeckillGoodsAuditStatus,
} = useMerchantSeckillApply()

function getAuditClass(status?: number) {
  return {
    'audit-tag--pending': status === SeckillGoodsAuditStatus.PENDING,
    'audit-tag--approved': status === SeckillGoodsAuditStatus.APPROVED,
    'audit-tag--rejected': status === SeckillGoodsAuditStatus.REJECTED,
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}

.seckill-apply-page {
  padding: 16px 0 48px;
}

.seckill-apply-inner {
  width: var(--layout-width);
  max-width: 100%;
  margin: 0 auto;
  padding: 0 10px;
}

.seckill-apply-panel {
  padding: 24px 28px 32px;
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 10px;
}

.seckill-apply-head {
  margin-bottom: 18px;
}

.seckill-apply-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.seckill-apply-desc {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #999;
}

.seckill-apply-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.seckill-apply-tab {
  height: 34px;
  padding: 0 18px;
  border: 1px solid #e5e5e5;
  border-radius: 999px;
  background: #fff;
  color: #666;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.seckill-apply-tab--active {
  border-color: transparent;
  background: var(--brand-gradient);
  color: #fff;
}

.seckill-apply-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 220px;
  color: #999;
  text-align: center;
}

.seckill-apply-state--error {
  color: #f5222d;
}

.apply-form,
.records-filter {
  max-width: 760px;
}

.apply-field-wide {
  max-width: none;
}

.apply-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 24px;
}

.apply-price-input {
  display: flex;
  align-items: center;
  gap: 8px;
}

.apply-price-input span {
  color: #666;
}

.apply-preview {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 8px 0 20px;
  padding: 14px;
  border-radius: 10px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
}

.apply-preview-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
}

.apply-preview-img--placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  color: #bbb;
}

.apply-preview-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.apply-preview-meta {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.apply-actions,
.records-filter-actions,
.records-pagination {
  display: flex;
  align-items: center;
  gap: 10px;
}

.apply-actions {
  margin-top: 8px;
  padding-left: 108px;
}

.apply-msg {
  margin: 0 0 14px;
  font-size: 13px;
}

.apply-msg--error {
  color: #f5222d;
}

.apply-msg--hint {
  flex: 1 1 100%;
  margin: 0 0 4px;
  color: #999;
}

.apply-empty-banner {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #fff7e6;
  border: 1px solid #ffe7ba;
  color: #ad6800;
  font-size: 13px;
  line-height: 1.7;
}

.apply-empty-banner p {
  margin: 0;
}

.apply-empty-banner-tip {
  margin-top: 4px !important;
  color: #8c6d1f;
}

.apply-actions {
  flex-wrap: wrap;
}

.apply-activity-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0 0 16px 108px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff7e6;
  border: 1px solid #ffe7ba;
  font-size: 13px;
  color: #ad6800;
}

.apply-activity-info-label {
  font-weight: 600;
}

.apply-activity-info-time {
  color: #8c6d1f;
}

.required {
  color: #f5222d;
}

.records-filter {
  display: grid;
  grid-template-columns: 1fr 180px auto;
  gap: 14px;
  align-items: end;
  margin-bottom: 18px;
  padding: 16px;
  border-radius: 10px;
  background: #fafafa;
}

.records-filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.records-table-wrap {
  overflow-x: auto;
  border: 1px solid #eee;
  border-radius: 10px;
}

.records-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
  font-size: 12px;
}

.records-table th,
.records-table td {
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  vertical-align: middle;
}

.records-table th {
  background: #fafafa;
  color: #666;
  font-weight: 600;
}

.records-goods-cell {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.records-goods-img {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
}

.records-goods-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #bbb;
}

.records-goods-name {
  margin: 0 0 4px;
  font-weight: 600;
  color: #333;
}

.records-goods-meta,
.records-goods-remark {
  margin: 0;
  color: #999;
  line-height: 1.6;
}

.records-goods-remark {
  color: #f5222d;
}

.records-price {
  color: var(--accent);
  font-weight: 700;
}

.audit-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 58px;
  height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.audit-tag--pending {
  border: 1px solid #ffe58f;
  background: #fffbe6;
  color: #d48806;
}

.audit-tag--approved {
  border: 1px solid #b7eb8f;
  background: #f6ffed;
  color: #52c41a;
}

.audit-tag--rejected {
  border: 1px solid #ffccc7;
  background: #fff2f0;
  color: #f5222d;
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

.filter-btn--primary {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.records-pagination {
  justify-content: center;
  margin-top: 18px;
  color: #666;
}

@media (max-width: 768px) {
  .apply-form-grid,
  .records-filter {
    grid-template-columns: 1fr;
  }

  .apply-actions,
  .apply-activity-info {
    padding-left: 0;
    margin-left: 0;
  }
}
</style>
