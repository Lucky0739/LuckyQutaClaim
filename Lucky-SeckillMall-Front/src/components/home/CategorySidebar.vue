<template>
  <div class="category-sidebar-wrap" @mouseleave="handleSidebarLeave">
    <aside class="category-sidebar">
      <div class="category-header">
        <span class="category-header-title">商品分类</span>
      </div>

      <div v-if="loading" class="category-state">加载中...</div>
      <div v-else-if="loadError" class="category-state category-state--error">{{ loadError }}</div>
      <div v-else-if="!categoryList.length" class="category-state">暂无分类</div>

      <ul v-else class="category-list">
        <li
          class="category-item category-item--all"
          :class="{ active: isAllSelected }"
        >
          <a href="#" class="category-link" @click.prevent="selectAll">
            <span class="category-name">全部商品</span>
          </a>
        </li>
        <li
          v-for="category in categoryList"
          :key="category.id"
          class="category-item"
          :class="{
            active: activeParentId === category.id,
            'category-item--selected': isParentSelected(category.id),
          }"
          @mouseenter="handleParentEnter(category.id)"
        >
          <a href="#" class="category-link" @click.prevent="selectParent(category)">
            <span class="category-name">{{ category.name }}</span>
            <span class="category-arrow">›</span>
          </a>

          <Transition name="popup-fade">
            <div
              v-if="activeParentId === category.id"
              class="category-popup"
            >
              <div v-if="category.childrenLoading" class="subcategory-state">加载中...</div>
              <div
                v-else-if="category.childrenError"
                class="subcategory-state subcategory-state--error"
              >
                {{ category.childrenError }}
              </div>
              <div v-else-if="!category.children.length" class="subcategory-state">暂无子分类</div>
              <ul v-else class="subcategory-list">
                <li v-for="child in category.children" :key="child.id">
                  <a
                    href="#"
                    class="subcategory-link"
                    :class="{ 'subcategory-link--active': isChildSelected(child.id) }"
                    @click.prevent="selectChild(category, child)"
                  >
                    {{ child.categoryName }}
                  </a>
                </li>
              </ul>
            </div>
          </Transition>
        </li>
      </ul>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getCategoryChildren, getParentCategories } from '@/api/category'
import type { GoodsCategory } from '@/types/category'
import {
  isCategoryFilterActive,
  type GoodsCategorySelection,
} from '@/types/goods'

const props = defineProps<{
  selected?: GoodsCategorySelection | null
}>()

const emit = defineEmits<{
  select: [filter: GoodsCategorySelection]
  clear: []
}>()

const isAllSelected = computed(() => !isCategoryFilterActive(props.selected))

function isParentSelected(parentId: string) {
  return props.selected?.parentCategoryId === parentId && !props.selected?.categoryId
}

function isChildSelected(childId: string) {
  return props.selected?.categoryId === childId
}

function selectAll() {
  emit('clear')
}

function selectParent(category: CategoryDisplayItem) {
  emit('select', {
    parentCategoryId: category.id,
    parentName: category.name,
  })
}

function selectChild(parent: CategoryDisplayItem, child: GoodsCategory) {
  emit('select', {
    parentCategoryId: parent.id,
    parentName: parent.name,
    categoryId: child.id,
    categoryName: child.categoryName,
  })
}

interface CategoryDisplayItem {
  id: string
  name: string
  children: GoodsCategory[]
  childrenLoaded: boolean
  childrenLoading: boolean
  childrenError: string
}

const categoryList = ref<CategoryDisplayItem[]>([])
const loading = ref(true)
const loadError = ref('')
const activeParentId = ref<string | null>(null)

function patchCategory(parentId: string, patch: Partial<CategoryDisplayItem>) {
  const index = categoryList.value.findIndex((item) => item.id === parentId)
  if (index === -1) return
  categoryList.value[index] = {
    ...categoryList.value[index],
    ...patch,
  }
}

async function loadParentCategories() {
  loading.value = true
  loadError.value = ''
  try {
    const parents = await getParentCategories()
    categoryList.value = parents.map((item) => ({
      id: item.id,
      name: item.categoryName,
      children: [],
      childrenLoaded: false,
      childrenLoading: false,
      childrenError: '',
    }))
    activeParentId.value = null
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '加载分类失败'
    categoryList.value = []
  } finally {
    loading.value = false
  }
}

async function loadChildren(parentId: string) {
  const category = categoryList.value.find((item) => item.id === parentId)
  if (!category || category.childrenLoading) return
  if (category.childrenLoaded) return

  patchCategory(parentId, {
    childrenLoading: true,
    childrenError: '',
  })

  try {
    const children = await getCategoryChildren(parentId)
    patchCategory(parentId, {
      children,
      childrenLoaded: true,
      childrenLoading: false,
      childrenError: '',
    })
  } catch (error) {
    patchCategory(parentId, {
      children: [],
      childrenLoaded: false,
      childrenLoading: false,
      childrenError: error instanceof Error ? error.message : '加载子分类失败',
    })
  }
}

function handleParentEnter(parentId: string) {
  activeParentId.value = parentId
  loadChildren(parentId)
}

function handleSidebarLeave() {
  activeParentId.value = null
}

onMounted(() => {
  loadParentCategories()
})
</script>

<style scoped>
.category-sidebar-wrap {
  position: relative;
  height: 100%;
  overflow: visible;
}

.category-sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #fff;
  border-radius: var(--home-card-radius);
  overflow: visible;
  box-shadow: 0 6px 18px rgba(255, 80, 0, 0.06);
}

.category-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 0 16px;
  height: 42px;
  border-bottom: 1px solid #f2f2f2;
}

.category-header-title {
  font-size: 14px;
  font-weight: 600;
  color: #111;
}

.category-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  font-size: 13px;
  color: #999;
  text-align: center;
}

.category-state--error {
  color: #f5222d;
}

.category-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 4px 10px 8px;
  list-style: none;
  min-height: 0;
  overflow: visible;
}

.category-item.active {
  z-index: 10;
}

.category-item.active .category-link,
.category-item--selected .category-link,
.category-item--all.active .category-link {
  color: var(--accent);
  font-weight: 600;
  background: #fff3eb;
}

.category-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0 6px;
  height: 100%;
  border-radius: 8px;
  font-size: 13px;
  color: #333;
  transition: color 0.18s, background 0.18s;
}

.category-link:hover {
  color: var(--accent);
  background: #fff7f2;
}

.category-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-item {
  position: relative;
  flex: 1;
  display: flex;
  align-items: stretch;
  min-height: 0;
  overflow: visible;
}

.category-item.active .category-popup {
  z-index: 11;
}

.category-item.active .category-arrow {
  color: var(--accent);
}

.category-arrow {
  flex-shrink: 0;
  margin-left: 4px;
  font-size: 14px;
  color: #aaa;
}

.category-popup {
  position: absolute;
  top: 50%;
  left: 100%;
  z-index: 11;
  min-width: 168px;
  max-width: 280px;
  margin-left: 8px;
  padding: 8px;
  background: var(--home-surface);
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12);
  transform: translateY(-50%);
  pointer-events: auto;
}

.category-popup::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: -10px;
  width: 10px;
}

.subcategory-state {
  padding: 6px 14px;
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.subcategory-state--error {
  color: #f5222d;
  white-space: normal;
  line-height: 1.5;
}

.subcategory-list {
  margin: 0;
  padding: 0;
  list-style: none;
  max-height: 240px;
  overflow-y: auto;
}

.subcategory-list li {
  margin: 0;
}

.subcategory-link {
  display: block;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: color 0.18s, background 0.18s;
}

.subcategory-link:hover,
.subcategory-link--active {
  color: var(--accent);
  font-weight: 600;
  background: #fff3eb;
}

.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.12s ease, transform 0.12s ease;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
  transform: translateY(-50%) translateX(-4px);
}
</style>
