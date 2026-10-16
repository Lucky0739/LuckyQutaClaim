<template>
  <div ref="rootRef" class="search-type-dropdown">
    <button
      type="button"
      class="dropdown-trigger"
      :class="{ open: isOpen }"
      @click.stop="toggle"
    >
      <span class="dropdown-label">{{ currentLabel }}</span>
      <span class="dropdown-arrow"></span>
    </button>

    <Transition name="dropdown-fade">
      <ul v-show="isOpen" class="dropdown-menu">
        <li
          v-for="item in options"
          :key="item.value"
          class="dropdown-item"
          :class="{ active: modelValue === item.value }"
          @click.stop="select(item.value)"
        >
          {{ item.label }}
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

export interface SearchTypeOption<T extends string = string> {
  label: string
  value: T
}

const props = defineProps<{
  modelValue: string
  options: SearchTypeOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const isOpen = ref(false)
const rootRef = ref<HTMLElement | null>(null)

const currentLabel = computed(
  () => props.options.find((item) => item.value === props.modelValue)?.label ?? '',
)

function toggle() {
  isOpen.value = !isOpen.value
}

function select(value: string) {
  emit('update:modelValue', value)
  isOpen.value = false
}

function handleClickOutside(event: MouseEvent) {
  if (!rootRef.value?.contains(event.target as Node)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.search-type-dropdown {
  position: relative;
  height: 100%;
  flex-shrink: 0;
  z-index: 2;
  border-radius: 13px 0 0 13px;
  overflow: visible;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 100%;
  min-width: 86px;
  padding: 0 14px 0 18px;
  border: none;
  background: #fff;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
  font-size: 15px;
  color: #222;
  outline: none;
  border-radius: 13px 0 0 13px;
}

.dropdown-trigger:hover,
.dropdown-trigger.open {
  background: #fff7f2;
  color: #222;
}

.dropdown-label {
  font-weight: 600;
  white-space: nowrap;
}

.dropdown-arrow {
  width: 0;
  height: 0;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
  border-top: 5px solid currentColor;
  transition: transform 0.2s;
  opacity: 0.75;
}

.dropdown-trigger.open .dropdown-arrow {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  z-index: 120;
  min-width: 92px;
  margin: 0;
  padding: 6px 0;
  list-style: none;
  background: #fff;
  border-radius: 10px;
  box-shadow:
    0 10px 24px rgba(0, 0, 0, 0.1),
    0 2px 6px rgba(0, 0, 0, 0.06);
  border: 1px solid #f2f2f2;
}

.dropdown-item {
  padding: 9px 18px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.15s, background 0.15s;
  white-space: nowrap;
}

.dropdown-item:hover {
  color: #ff5000;
  background: #fff7f2;
}

.dropdown-item.active {
  color: #ff5000;
  background: #fff7f2;
  font-weight: 600;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
