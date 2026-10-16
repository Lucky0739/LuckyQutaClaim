<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active address-page-tab">收货地址</span>
    </div>

    <div class="account-panel address-panel">
      <p v-if="errorMsg" class="account-msg account-msg--error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="account-msg account-msg--success">{{ successMsg }}</p>

      <div class="address-toolbar">
        <button type="button" class="address-add-btn" @click="openCreate">
          <span class="address-add-icon">+</span>
          添加收货地址
        </button>
        <p class="address-toolbar-tip">您已创建 {{ addressCount }} 个收货地址，最多可创建 25 个</p>
      </div>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载地址列表...
      </div>

      <div v-else-if="errorMsg && !hasAddresses" class="address-empty">
        <div class="address-empty-icon address-empty-icon--warn"></div>
        <p class="address-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadAddresses">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasAddresses" class="address-empty">
        <div class="address-empty-icon"></div>
        <p class="address-empty-text">您还没有收货地址，赶快添加一个吧</p>
        <button type="button" class="account-btn account-btn-primary" @click="openCreate">
          添加收货地址
        </button>
      </div>

      <div v-else class="address-list">
        <article
          v-for="item in addresses"
          :key="item.id"
          class="address-item"
          :class="{ 'address-item--default': isDefaultAddress(item) }"
        >
          <div class="address-item-body">
            <div class="address-item-head">
              <span class="address-receiver">{{ item.receiver }}</span>
              <span class="address-mobile">{{ maskAddressMobile(item.mobile) }}</span>
              <span v-if="isDefaultAddress(item)" class="address-default-tag">默认地址</span>
            </div>
            <p class="address-detail">{{ formatFullAddress(item) }}</p>
          </div>

          <div class="address-item-footer">
            <div class="address-item-default">
              <label
                v-if="isDefaultAddress(item)"
                class="address-default-label address-default-label--active"
              >
                <span class="address-radio address-radio--checked"></span>
                默认地址
              </label>
              <button
                v-else
                type="button"
                class="address-default-label"
                @click="setDefaultAddress(item)"
              >
                <span class="address-radio"></span>
                设为默认
              </button>
            </div>

            <div class="address-item-actions">
              <button type="button" class="address-action" @click="openEdit(item)">编辑</button>
              <span class="address-action-divider">|</span>
              <button
                type="button"
                class="address-action"
                :disabled="deletingId === item.id"
                @click="openDeleteConfirm(item)"
              >
                {{ deletingId === item.id ? '删除中...' : '删除' }}
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
      <div v-if="showModal" class="address-modal-mask" @click.self="closeModal">
        <form class="address-modal" @submit.prevent="submitAddress">
          <div class="address-modal-header">
            <h2 class="address-modal-title">{{ modalTitle }}</h2>
            <button type="button" class="address-modal-close" aria-label="关闭" @click="closeModal">
              ×
            </button>
          </div>

          <div class="address-modal-body">
            <p v-if="modalErrorMsg" class="account-msg account-msg--error">{{ modalErrorMsg }}</p>

            <div class="account-form-row">
              <label class="account-form-label"><span class="address-required">*</span>收货人</label>
              <div class="account-form-field">
                <input
                  v-model="form.receiver"
                  type="text"
                  maxlength="20"
                  class="account-input"
                  :class="{ 'account-input--error': formErrors.receiver }"
                  placeholder="请输入收货人姓名"
                />
                <p v-if="formErrors.receiver" class="form-field-error">{{ formErrors.receiver }}</p>
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label"><span class="address-required">*</span>手机号码</label>
              <div class="account-form-field">
                <input
                  v-model="form.mobile"
                  type="tel"
                  maxlength="11"
                  class="account-input"
                  :class="{ 'account-input--error': formErrors.mobile }"
                  placeholder="请输入11位手机号"
                />
                <p v-if="formErrors.mobile" class="form-field-error">{{ formErrors.mobile }}</p>
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label"><span class="address-required">*</span>所在地区</label>
              <div class="account-form-field">
                <AddressRegionPicker
                  v-model:province="form.province"
                  v-model:city="form.city"
                  v-model:area="form.area"
                  :error="formErrors.region"
                />
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label"><span class="address-required">*</span>详细地址</label>
              <div class="account-form-field">
                <textarea
                  v-model="form.detailAddress"
                  class="account-textarea"
                  :class="{ 'account-input--error': formErrors.detailAddress }"
                  maxlength="120"
                  placeholder="请输入详细地址，如街道、门牌号、楼层等"
                />
                <p v-if="formErrors.detailAddress" class="form-field-error">{{ formErrors.detailAddress }}</p>
              </div>
            </div>

            <div class="account-form-row">
              <label class="account-form-label"></label>
              <div class="account-form-field">
                <label class="address-checkbox-row">
                  <input
                    v-model="isDefaultChecked"
                    type="checkbox"
                    :disabled="!hasAddresses && !isEditing"
                  />
                  设为默认收货地址
                </label>
                <p v-if="!hasAddresses && !isEditing" class="account-tip">首个地址将自动设为默认</p>
              </div>
            </div>
          </div>

          <div class="address-modal-footer">
            <button type="button" class="account-btn" @click="closeModal">取消</button>
            <button type="submit" class="account-btn account-btn-primary" :disabled="submitting">
              {{ submitting ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>

      <div v-if="showDeleteModal && pendingDeleteAddress" class="address-modal-mask" @click.self="closeDeleteConfirm">
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">删除收货地址</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="!!deletingId"
              @click="closeDeleteConfirm"
            >
              ×
            </button>
          </div>

          <div class="address-delete-body">
            <p class="address-delete-tip">确定要删除以下收货地址吗？删除后无法恢复。</p>
            <div class="address-delete-preview">
              <div class="address-item-head">
                <span class="address-receiver">{{ pendingDeleteAddress.receiver }}</span>
                <span class="address-mobile">{{ maskAddressMobile(pendingDeleteAddress.mobile) }}</span>
                <span v-if="isDefaultAddress(pendingDeleteAddress)" class="address-default-tag">默认地址</span>
              </div>
              <p class="address-detail">{{ formatFullAddress(pendingDeleteAddress) }}</p>
            </div>
          </div>

          <div class="address-modal-footer">
            <button
              type="button"
              class="account-btn"
              :disabled="!!deletingId"
              @click="closeDeleteConfirm"
            >
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-danger"
              :disabled="!!deletingId"
              @click="confirmDelete"
            >
              {{ deletingId ? '删除中...' : '确认删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import AddressRegionPicker from '@/components/account/AddressRegionPicker.vue'
import { useUserAddress } from '@/composables/useUserAddress'
import { AddressDefault } from '@/types/address'

const route = useRoute()

const {
  loading,
  submitting,
  deletingId,
  showModal,
  showDeleteModal,
  pendingDeleteAddress,
  isEditing,
  modalTitle,
  hasAddresses,
  addressCount,
  errorMsg,
  successMsg,
  toastMsg,
  modalErrorMsg,
  formErrors,
  addresses,
  form,
  loadAddresses,
  openCreate,
  openEdit,
  openEditById,
  closeModal,
  submitAddress,
  setDefaultAddress,
  openDeleteConfirm,
  closeDeleteConfirm,
  confirmDelete,
  formatFullAddress,
  maskAddressMobile,
  isDefaultAddress,
} = useUserAddress()

const isDefaultChecked = computed({
  get: () => form.value.isDefault === AddressDefault.YES,
  set: (checked: boolean) => {
    form.value.isDefault = checked ? AddressDefault.YES : AddressDefault.NO
  },
})

async function loadAndOpenQueryAddress() {
  await loadAddresses()
  const addressId = route.query.addressId
  if (typeof addressId === 'string' && addressId) {
    await openEditById(addressId)
  }
}

onMounted(loadAndOpenQueryAddress)

watch(() => route.query.addressId, (addressId) => {
  if (typeof addressId === 'string' && addressId) {
    openEditById(addressId)
  }
})
</script>
