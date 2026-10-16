<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">分类管理</span>
    </div>

    <div class="account-panel admin-user-panel admin-category-panel">
      <p v-if="errorMsg && hasCategories" class="account-msg account-msg--error">{{ errorMsg }}</p>

      <form class="admin-user-filter" @submit.prevent="handleSearch">
        <div class="admin-user-filter-grid admin-category-filter-grid">
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">分类名称</label>
            <input
              v-model="filterName"
              type="text"
              maxlength="30"
              class="account-input admin-user-filter-input"
              placeholder="模糊搜索分类名称"
            />
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">层级</label>
            <select v-model="filterLevel" class="account-input admin-user-filter-input">
              <option value="">全部</option>
              <option value="parent">父分类</option>
              <option value="child">二级分类</option>
            </select>
          </div>
          <div class="admin-user-filter-item">
            <label class="admin-user-filter-label">状态</label>
            <select v-model="filterStatus" class="account-input admin-user-filter-input">
              <option value="">全部</option>
              <option :value="CategoryStatus.ENABLED">启用</option>
              <option :value="CategoryStatus.DISABLED">禁用</option>
            </select>
          </div>
        </div>
        <div class="admin-user-filter-actions">
          <button type="submit" class="account-btn account-btn-primary" :disabled="loading">
            {{ loading ? '查询中...' : '查询' }}
          </button>
          <button type="button" class="account-btn" :disabled="loading" @click="handleReset">
            重置
          </button>
        </div>
      </form>

      <div class="admin-category-toolbar">
        <p class="admin-category-tip">
          共 {{ categories.length }} 个分类（父分类 {{ parentCount }} / 二级 {{ childCount }}），
          当前展示 {{ filteredCategories.length }} 个
        </p>
        <button type="button" class="account-btn account-btn-primary" @click="openCreateModal()">
          新增分类
        </button>
      </div>

      <div v-if="loading" class="address-loading">
        <span class="address-loading-dot"></span>
        加载分类列表...
      </div>

      <div v-else-if="errorMsg && !hasCategories" class="address-empty">
        <div class="address-empty-icon address-empty-icon--warn"></div>
        <p class="address-empty-text">{{ errorMsg }}</p>
        <button type="button" class="account-btn account-btn-primary" @click="loadList">
          重新加载
        </button>
      </div>

      <div v-else-if="!hasCategories" class="address-empty">
        <div class="address-empty-icon"></div>
        <p class="address-empty-text">暂无商品分类</p>
        <button type="button" class="account-btn account-btn-primary" @click="openCreateModal()">
          新增第一个分类
        </button>
      </div>

      <div v-else-if="!filteredCategories.length" class="address-empty">
        <div class="address-empty-icon"></div>
        <p class="address-empty-text">暂无符合条件的分类</p>
        <button type="button" class="account-btn" @click="handleReset">重置筛选</button>
      </div>

      <div v-else class="admin-category-table-wrap">
        <table class="admin-category-table">
          <thead>
            <tr>
              <th>分类名称</th>
              <th>层级</th>
              <th>父分类</th>
              <th>排序</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="category in filteredCategories" :key="category.id">
              <td>
                <div class="admin-category-name-cell">
                  <span
                    class="category-name"
                    :class="{ 'category-name--child': !isRootCategory(category.parentId) }"
                  >
                    {{ category.categoryName }}
                  </span>
                  <span class="admin-category-id">ID: {{ category.id }}</span>
                </div>
              </td>
              <td>
                <span
                  class="admin-category-level-tag"
                  :class="isRootCategory(category.parentId) ? 'admin-category-level-tag--parent' : 'admin-category-level-tag--child'"
                >
                  {{ isRootCategory(category.parentId) ? '父分类' : '二级分类' }}
                </span>
              </td>
              <td>{{ getParentName(category.parentId) }}</td>
              <td>{{ category.sort ?? 0 }}</td>
              <td>
                <span
                  class="admin-category-status"
                  :class="isEnabled(category) ? 'admin-category-status--enabled' : 'admin-category-status--disabled'"
                >
                  {{ isEnabled(category) ? '启用' : '禁用' }}
                </span>
              </td>
              <td>{{ formatDateTime(category.createTime) }}</td>
              <td>{{ formatDateTime(category.updateTime) }}</td>
              <td>
                <div class="admin-category-actions">
                  <button type="button" class="admin-user-action" @click="openEditModal(category)">
                    编辑
                  </button>
                  <template v-if="isRootCategory(category.parentId)">
                    <span class="address-action-divider">|</span>
                    <button
                      type="button"
                      class="admin-user-action"
                      @click="openCreateModal(category.id)"
                    >
                      新增子分类
                    </button>
                  </template>
                  <span class="address-action-divider">|</span>
                  <button
                    type="button"
                    class="admin-user-action"
                    :class="{
                      'admin-user-action--danger': isEnabled(category),
                      'admin-user-action--success': !isEnabled(category),
                    }"
                    :disabled="togglingId === category.id"
                    @click="openStatusConfirm(category)"
                  >
                    {{
                      togglingId === category.id
                        ? '处理中...'
                        : isEnabled(category)
                          ? '禁用'
                          : '启用'
                    }}
                  </button>
                  <span class="address-action-divider">|</span>
                  <button
                    type="button"
                    class="admin-user-action admin-user-action--danger"
                    :disabled="!canDeleteCategory(category) || deletingId === category.id"
                    :title="getDeleteDisabledReason(category) || undefined"
                    @click="openDeleteConfirm(category)"
                  >
                    {{ deletingId === category.id ? '删除中...' : '删除' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="address-toast-fade">
        <div v-if="toastMsg" class="address-success-toast">{{ toastMsg }}</div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <div v-if="showModal" class="address-modal-mask" @click.self="closeModal">
        <div class="address-modal admin-category-modal">
          <div class="address-modal-header">
            <h2 class="address-modal-title">{{ editingId ? '编辑分类' : '新增分类' }}</h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="submitting"
              @click="closeModal"
            >
              ×
            </button>
          </div>

          <form class="address-modal-body" @submit.prevent="submitForm">
            <p v-if="formErrorMsg" class="account-msg account-msg--error">{{ formErrorMsg }}</p>

            <label class="admin-category-form-item">
              <span>分类名称</span>
              <input
                v-model.trim="form.categoryName"
                type="text"
                maxlength="30"
                class="account-input"
                placeholder="请输入分类名称"
              />
            </label>

            <label class="admin-category-form-item">
              <span>父分类</span>
              <select
                v-model="form.parentId"
                class="account-input"
                :disabled="editingHasChildren"
              >
                <option value="">无，作为父分类</option>
                <option
                  v-for="parent in parentOptions"
                  :key="parent.id"
                  :value="parent.id"
                  :disabled="parent.id === editingId"
                >
                  {{ parent.categoryName }}
                </option>
              </select>
              <span v-if="editingHasChildren" class="admin-category-form-hint">
                该分类下已有子分类，不能改为二级分类
              </span>
            </label>

            <label class="admin-category-form-item">
              <span>排序</span>
              <input
                v-model.number="form.sort"
                type="number"
                min="0"
                class="account-input"
                placeholder="数字越小越靠前"
              />
            </label>

            <div class="address-modal-footer admin-category-modal-footer">
              <button type="button" class="account-btn" :disabled="submitting" @click="closeModal">
                取消
              </button>
              <button type="submit" class="account-btn account-btn-primary" :disabled="submitting">
                {{ submitting ? '保存中...' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showStatusConfirm && pendingStatusCategory"
        class="address-modal-mask"
        @click.self="closeStatusConfirm"
      >
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">
              {{ isEnabled(pendingStatusCategory) ? '禁用分类' : '启用分类' }}
            </h2>
            <button
              type="button"
              class="address-modal-close"
              aria-label="关闭"
              :disabled="!!togglingId"
              @click="closeStatusConfirm"
            >
              ×
            </button>
          </div>
          <div class="address-modal-body">
            <p class="admin-user-confirm-text">
              确定要{{ isEnabled(pendingStatusCategory) ? '禁用' : '启用' }}分类
              <strong>{{ pendingStatusCategory.categoryName }}</strong>
              吗？
            </p>
            <p
              v-if="isRootCategory(pendingStatusCategory.parentId) && isEnabled(pendingStatusCategory)"
              class="admin-category-confirm-tip"
            >
              禁用父分类后，其下子分类在前台可能无法正常展示。
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="!!togglingId" @click="closeStatusConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn"
              :class="{
                'account-btn-danger': isEnabled(pendingStatusCategory),
                'account-btn-primary': !isEnabled(pendingStatusCategory),
              }"
              :disabled="!!togglingId"
              @click="confirmToggleStatus"
            >
              {{ togglingId ? '处理中...' : '确认' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="showDeleteConfirm && pendingDeleteCategory"
        class="address-modal-mask"
        @click.self="closeDeleteConfirm"
      >
        <div class="address-delete-dialog">
          <div class="address-modal-header">
            <h2 class="address-modal-title">删除分类</h2>
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
          <div class="address-modal-body">
            <p v-if="deleteErrorMsg" class="account-msg account-msg--error">{{ deleteErrorMsg }}</p>
            <p class="admin-user-confirm-text">
              确定删除分类 <strong>{{ pendingDeleteCategory.categoryName }}</strong> 吗？此操作不可恢复。
            </p>
            <p
              v-if="getChildCount(pendingDeleteCategory.id) > 0"
              class="admin-category-confirm-tip admin-category-confirm-tip--warn"
            >
              该分类下还有 {{ getChildCount(pendingDeleteCategory.id) }} 个子分类，请先删除或迁移子分类。
            </p>
          </div>
          <div class="address-modal-footer">
            <button type="button" class="account-btn" :disabled="!!deletingId" @click="closeDeleteConfirm">
              取消
            </button>
            <button
              type="button"
              class="account-btn account-btn-danger"
              :disabled="!!deletingId || !canDeleteCategory(pendingDeleteCategory)"
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
import { computed, onMounted, reactive, ref } from 'vue'
import {
  addCategory,
  deleteCategory,
  disableCategory,
  enableCategory,
  getAllCategories,
  updateCategory,
} from '@/api/category'
import { CategoryStatus, isRootCategory, type CategoryId, type GoodsCategory } from '@/types/category'

const categories = ref<GoodsCategory[]>([])
const loading = ref(false)
const submitting = ref(false)
const togglingId = ref<CategoryId>('')
const deletingId = ref<CategoryId>('')
const errorMsg = ref('')
const formErrorMsg = ref('')
const deleteErrorMsg = ref('')
const toastMsg = ref('')
const showModal = ref(false)
const showStatusConfirm = ref(false)
const showDeleteConfirm = ref(false)
const editingId = ref<CategoryId>('')
const pendingStatusCategory = ref<GoodsCategory | null>(null)
const pendingDeleteCategory = ref<GoodsCategory | null>(null)

const filterName = ref('')
const filterLevel = ref<'parent' | 'child' | ''>('')
const filterStatus = ref<number | ''>('')

const appliedFilterName = ref('')
const appliedFilterLevel = ref<'parent' | 'child' | ''>('')
const appliedFilterStatus = ref<number | ''>('')

const form = reactive({
  categoryName: '',
  parentId: '',
  sort: 0,
})

const hasCategories = computed(() => categories.value.length > 0)

const parentCount = computed(() => categories.value.filter((item) => isRootCategory(item.parentId)).length)

const childCount = computed(() => categories.value.filter((item) => !isRootCategory(item.parentId)).length)

const childrenMap = computed(() => {
  const map = new Map<CategoryId, GoodsCategory[]>()
  for (const item of categories.value) {
    if (isRootCategory(item.parentId) || item.parentId == null) continue
    const parentId = item.parentId
    const list = map.get(parentId) ?? []
    list.push(item)
    map.set(parentId, list)
  }
  return map
})

const parentOptions = computed(() =>
  categories.value.filter((item) => isRootCategory(item.parentId) && item.id !== editingId.value),
)

const editingHasChildren = computed(() => {
  if (!editingId.value) return false
  return getChildCount(editingId.value) > 0
})

const sortedCategories = computed(() => {
  const parents = categories.value
    .filter((item) => isRootCategory(item.parentId))
    .sort(compareCategory)
  const orphanChildren = categories.value
    .filter((item) => !isRootCategory(item.parentId))
    .filter((item) => !parents.some((parent) => parent.id === item.parentId))
    .sort(compareCategory)

  return [
    ...parents.flatMap((parent) => [
      parent,
      ...(childrenMap.value.get(parent.id)?.sort(compareCategory) ?? []),
    ]),
    ...orphanChildren,
  ]
})

const filteredCategories = computed(() => {
  const keyword = appliedFilterName.value.trim().toLowerCase()
  return sortedCategories.value.filter((category) => {
    if (keyword && !category.categoryName.toLowerCase().includes(keyword)) {
      return false
    }
    if (appliedFilterLevel.value === 'parent' && !isRootCategory(category.parentId)) {
      return false
    }
    if (appliedFilterLevel.value === 'child' && isRootCategory(category.parentId)) {
      return false
    }
    if (appliedFilterStatus.value !== '') {
      const enabled = isEnabled(category)
      if (appliedFilterStatus.value === CategoryStatus.ENABLED && !enabled) return false
      if (appliedFilterStatus.value === CategoryStatus.DISABLED && enabled) return false
    }
    return true
  })
})

function compareCategory(a: GoodsCategory, b: GoodsCategory) {
  return (a.sort ?? 0) - (b.sort ?? 0) || a.categoryName.localeCompare(b.categoryName)
}

function isEnabled(category: GoodsCategory) {
  return category.status !== CategoryStatus.DISABLED
}

function getChildCount(parentId: CategoryId) {
  return childrenMap.value.get(parentId)?.length ?? 0
}

function getParentName(parentId?: CategoryId | null) {
  if (isRootCategory(parentId)) return '--'
  return categories.value.find((item) => item.id === parentId)?.categoryName ?? '--'
}

function canDeleteCategory(category: GoodsCategory) {
  if (isRootCategory(category.parentId) && getChildCount(category.id) > 0) {
    return false
  }
  return true
}

function getDeleteDisabledReason(category: GoodsCategory) {
  if (isRootCategory(category.parentId) && getChildCount(category.id) > 0) {
    return '请先删除该分类下的子分类'
  }
  return ''
}

function formatDateTime(value?: string) {
  if (!value) return '--'
  return value.replace('T', ' ').slice(0, 19)
}

function showToast(message: string) {
  toastMsg.value = message
  window.setTimeout(() => {
    if (toastMsg.value === message) toastMsg.value = ''
  }, 1800)
}

function handleSearch() {
  appliedFilterName.value = filterName.value
  appliedFilterLevel.value = filterLevel.value
  appliedFilterStatus.value = filterStatus.value
}

function handleReset() {
  filterName.value = ''
  filterLevel.value = ''
  filterStatus.value = ''
  appliedFilterName.value = ''
  appliedFilterLevel.value = ''
  appliedFilterStatus.value = ''
}

async function loadList() {
  loading.value = true
  errorMsg.value = ''
  try {
    categories.value = await getAllCategories()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '加载分类失败'
    categories.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = ''
  form.categoryName = ''
  form.parentId = ''
  form.sort = 0
  formErrorMsg.value = ''
}

function openCreateModal(parentId = '') {
  resetForm()
  form.parentId = parentId
  showModal.value = true
}

function openEditModal(category: GoodsCategory) {
  editingId.value = category.id
  form.categoryName = category.categoryName
  form.parentId = category.parentId ?? ''
  form.sort = category.sort ?? 0
  formErrorMsg.value = ''
  showModal.value = true
}

function closeModal() {
  if (submitting.value) return
  showModal.value = false
  resetForm()
}

async function submitForm() {
  if (!form.categoryName.trim()) {
    formErrorMsg.value = '请输入分类名称'
    return
  }

  submitting.value = true
  formErrorMsg.value = ''
  try {
    const payload = {
      categoryName: form.categoryName.trim(),
      parentId: editingHasChildren.value ? null : form.parentId || null,
      sort: Number(form.sort) || 0,
    }
    if (editingId.value) {
      await updateCategory({ ...payload, id: editingId.value })
      showToast('分类已更新')
    } else {
      await addCategory(payload)
      showToast('分类已新增')
    }
    showModal.value = false
    resetForm()
    await loadList()
  } catch (err) {
    formErrorMsg.value = err instanceof Error ? err.message : '保存分类失败'
  } finally {
    submitting.value = false
  }
}

function openStatusConfirm(category: GoodsCategory) {
  pendingStatusCategory.value = category
  showStatusConfirm.value = true
}

function closeStatusConfirm() {
  if (togglingId.value) return
  showStatusConfirm.value = false
  pendingStatusCategory.value = null
}

async function confirmToggleStatus() {
  const category = pendingStatusCategory.value
  if (!category) return

  togglingId.value = category.id
  errorMsg.value = ''
  try {
    if (isEnabled(category)) {
      await disableCategory(category.id)
      showToast('分类已禁用')
    } else {
      await enableCategory(category.id)
      showToast('分类已启用')
    }
    closeStatusConfirm()
    await loadList()
  } catch (err) {
    errorMsg.value = err instanceof Error ? err.message : '更新分类状态失败'
  } finally {
    togglingId.value = ''
  }
}

function openDeleteConfirm(category: GoodsCategory) {
  pendingDeleteCategory.value = category
  deleteErrorMsg.value = ''
  showDeleteConfirm.value = true
}

function closeDeleteConfirm() {
  if (deletingId.value) return
  showDeleteConfirm.value = false
  pendingDeleteCategory.value = null
  deleteErrorMsg.value = ''
}

async function confirmDelete() {
  const category = pendingDeleteCategory.value
  if (!category || !canDeleteCategory(category)) return

  deletingId.value = category.id
  deleteErrorMsg.value = ''
  try {
    await deleteCategory(category.id)
    showToast('分类已删除')
    closeDeleteConfirm()
    await loadList()
  } catch (err) {
    deleteErrorMsg.value = err instanceof Error ? err.message : '删除分类失败'
  } finally {
    deletingId.value = ''
  }
}

onMounted(loadList)
</script>

<style scoped>
.admin-page-tab {
  cursor: default;
}

.admin-category-panel {
  padding-top: 20px;
}

.admin-category-filter-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.admin-user-filter {
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  background: #fafafa;
}

.admin-user-filter-grid {
  display: grid;
  gap: 14px 20px;
}

.admin-user-filter-item {
  display: grid;
  grid-template-columns: 76px 1fr;
  align-items: center;
  gap: 10px;
}

.admin-user-filter-label {
  font-size: 13px;
  color: #666;
}

.admin-user-filter-input {
  width: 100%;
}

.admin-user-filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-left: 76px;
}

.admin-category-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.admin-category-tip {
  margin: 0;
  color: #999;
  font-size: 13px;
}

.admin-category-table-wrap {
  overflow-x: auto;
  border: 1px solid #f0f0f0;
  border-radius: 10px;
}

.admin-category-table {
  width: 100%;
  min-width: 1080px;
  border-collapse: collapse;
  background: #fff;
}

.admin-category-table th,
.admin-category-table td {
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
  color: #555;
  font-size: 13px;
  text-align: left;
  vertical-align: middle;
}

.admin-category-table th {
  background: #fafafa;
  color: #888;
  font-weight: 600;
  white-space: nowrap;
}

.admin-category-table tbody tr:last-child td {
  border-bottom: none;
}

.admin-category-table tbody tr:hover {
  background: #fffaf7;
}

.admin-category-name-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.category-name {
  color: #222;
  font-weight: 600;
}

.category-name--child {
  padding-left: 18px;
  color: #666;
  font-weight: 500;
}

.category-name--child::before {
  content: '└ ';
  color: #bbb;
}

.admin-category-id {
  font-size: 11px;
  color: #999;
}

.admin-category-level-tag {
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  line-height: 22px;
  white-space: nowrap;
}

.admin-category-level-tag--parent {
  background: #fff5eb;
  color: var(--accent);
}

.admin-category-level-tag--child {
  background: #f5f5f5;
  color: #666;
}

.admin-category-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 24px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.admin-category-status--enabled {
  background: #f6ffed;
  color: #389e0d;
}

.admin-category-status--disabled {
  background: #fff1f0;
  color: #cf1322;
}

.admin-category-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
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
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-user-action--danger {
  color: #f5222d;
}

.admin-user-action--success {
  color: #52c41a;
}

.admin-category-modal {
  width: min(480px, calc(100vw - 32px));
}

.admin-category-form-item {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
  color: #666;
  font-size: 13px;
}

.admin-category-form-hint {
  font-size: 12px;
  color: #fa8c16;
}

.admin-category-modal-footer {
  padding: 8px 0 0;
}

.admin-user-confirm-text {
  margin: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.admin-user-confirm-text strong {
  color: #333;
}

.admin-category-confirm-tip {
  margin: 12px 0 0;
  font-size: 13px;
  color: #999;
  line-height: 1.5;
}

.admin-category-confirm-tip--warn {
  color: #fa8c16;
}

@media (max-width: 900px) {
  .admin-category-filter-grid {
    grid-template-columns: 1fr;
  }

  .admin-user-filter-item {
    grid-template-columns: 1fr;
  }

  .admin-user-filter-actions {
    padding-left: 0;
  }
}
</style>
