<template>
  <Teleport to="body">
    <div
      v-if="modelValue && goods"
      class="apply-shelf-mask"
      role="dialog"
      aria-modal="true"
      aria-labelledby="apply-shelf-title"
      @click.self="handleCancel"
    >
      <div class="apply-shelf-dialog">
        <header class="apply-shelf-header">
          <div class="apply-shelf-header-main">
            <span class="apply-shelf-icon" aria-hidden="true">↑</span>
            <h2 id="apply-shelf-title" class="apply-shelf-title">申请上架</h2>
          </div>
          <button
            type="button"
            class="apply-shelf-close"
            aria-label="关闭"
            :disabled="loading"
            @click="handleCancel"
          >
            ×
          </button>
        </header>

        <div class="apply-shelf-body">
          <p class="apply-shelf-tip">
            确认提交上架申请吗？提交后商品将进入<strong>待审核</strong>状态，管理员审核通过后将自动上架售卖。
          </p>

          <div class="apply-shelf-preview">
            <img
              v-if="goods.goodsImg"
              :src="resolveFileUrl(goods.goodsImg)"
              alt=""
              class="apply-shelf-preview-img"
              referrerpolicy="no-referrer"
            />
            <span v-else class="apply-shelf-preview-img apply-shelf-preview-img--placeholder">图</span>
            <div class="apply-shelf-preview-meta">
              <p class="apply-shelf-preview-name" :title="goods.goodsName">{{ goods.goodsName }}</p>
              <p class="apply-shelf-preview-price">¥{{ formatGoodsPrice(goods.goodsPrice) }}</p>
              <p class="apply-shelf-preview-id">ID: {{ goods.id }}</p>
            </div>
          </div>
        </div>

        <footer class="apply-shelf-footer">
          <button
            type="button"
            class="apply-shelf-btn apply-shelf-btn--ghost"
            :disabled="loading"
            @click="handleCancel"
          >
            取消
          </button>
          <button
            type="button"
            class="apply-shelf-btn apply-shelf-btn--primary"
            :disabled="loading"
            @click="emit('confirm')"
          >
            {{ loading ? '提交中...' : '确认申请' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { formatGoodsPrice, type GoodsListVO } from '@/types/goods'
import { resolveFileUrl } from '@/utils/file'

defineProps<{
  modelValue: boolean
  goods: GoodsListVO | null
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
  cancel: []
}>()

function handleCancel() {
  emit('update:modelValue', false)
  emit('cancel')
}
</script>

<style scoped>
.apply-shelf-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.45);
  animation: apply-shelf-fade-in 0.2s ease;
}

.apply-shelf-dialog {
  width: 100%;
  max-width: 440px;
  overflow: hidden;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  animation: apply-shelf-slide-up 0.24s ease;
}

.apply-shelf-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #fff7f0 0%, #fff 100%);
  border-bottom: 1px solid #ffe8d6;
}

.apply-shelf-header-main {
  display: flex;
  align-items: center;
  gap: 10px;
}

.apply-shelf-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--brand-gradient);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  box-shadow: 0 4px 10px rgba(255, 80, 0, 0.25);
}

.apply-shelf-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #333;
}

.apply-shelf-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.04);
  font-size: 20px;
  line-height: 1;
  color: #999;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.apply-shelf-close:hover:not(:disabled) {
  background: rgba(0, 0, 0, 0.08);
  color: #333;
}

.apply-shelf-close:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.apply-shelf-body {
  padding: 20px 22px 8px;
}

.apply-shelf-tip {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #666;
}

.apply-shelf-tip strong {
  color: #d48806;
  font-weight: 600;
}

.apply-shelf-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.apply-shelf-preview-img {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  border-radius: 8px;
  object-fit: cover;
  border: 1px solid #eee;
  background: #f5f5f5;
}

.apply-shelf-preview-img--placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 13px;
}

.apply-shelf-preview-meta {
  min-width: 0;
}

.apply-shelf-preview-name {
  margin: 0 0 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.apply-shelf-preview-price {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--accent);
}

.apply-shelf-preview-id {
  margin: 0;
  font-size: 11px;
  color: #999;
}

.apply-shelf-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 22px 20px;
}

.apply-shelf-btn {
  min-width: 96px;
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s, background 0.15s;
}

.apply-shelf-btn--ghost {
  border: 1px solid #ddd;
  background: #fff;
  color: #666;
}

.apply-shelf-btn--ghost:hover:not(:disabled) {
  border-color: #ccc;
  background: #fafafa;
}

.apply-shelf-btn--primary {
  border: none;
  background: var(--brand-gradient);
  color: #fff;
  box-shadow: 0 6px 14px rgba(255, 80, 0, 0.22);
}

.apply-shelf-btn--primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(255, 80, 0, 0.28);
}

.apply-shelf-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
  transform: none;
}

@keyframes apply-shelf-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes apply-shelf-slide-up {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
