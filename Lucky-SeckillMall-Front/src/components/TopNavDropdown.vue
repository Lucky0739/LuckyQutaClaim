<template>
  <div ref="rootRef" class="top-nav-dropdown">
    <button
      type="button"
      class="dropdown-trigger top-link"
      :class="{ open: isOpen, 'top-link--with-icon': !!icon }"
      @click="toggle"
    >
      <img v-if="icon" :src="icon" alt="" class="top-link-icon" />
      {{ label }}
      <span v-if="badge" class="badge">{{ badge }}</span>
      <span class="arrow">▼</span>
    </button>

    <Transition name="dropdown-fade">
      <ul v-show="isOpen" class="dropdown-menu">
        <li v-for="item in options" :key="item.to">
          <router-link :to="item.to" class="dropdown-item" @click="close">
            {{ item.label }}
          </router-link>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'

export interface TopNavDropdownOption {
  label: string
  to: string
}

defineProps<{
  label: string
  options: TopNavDropdownOption[]
  badge?: string
  icon?: string
}>()

const isOpen = ref(false)
const rootRef = ref<HTMLElement | null>(null)

function toggle() {
  isOpen.value = !isOpen.value
}

function close() {
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
.top-nav-dropdown {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.dropdown-trigger {
  display: inline-flex;
  align-items: center;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: inherit;
  line-height: inherit;
  outline: none;
}

.dropdown-trigger:hover,
.dropdown-trigger.open {
  color: var(--taobao-orange);
}

.top-link--with-icon {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.top-link-icon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  flex-shrink: 0;
}

.badge {
  color: var(--taobao-orange);
  margin-left: 2px;
}

.arrow {
  font-size: 10px;
  margin-left: 2px;
  color: var(--text-muted);
  transition: transform 0.2s;
}

.dropdown-trigger.open .arrow {
  transform: rotate(180deg);
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  min-width: 108px;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 4px;
  box-shadow:
    0 8px 20px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04);
}

.dropdown-item {
  display: block;
  padding: 8px 16px;
  font-size: 12px;
  color: var(--text-normal);
  white-space: nowrap;
  transition: color 0.15s, background 0.15s;
}

.dropdown-item:hover {
  color: var(--taobao-orange);
  background: #fff7f0;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-6px);
}

.dropdown-fade-enter-to,
.dropdown-fade-leave-from {
  transform: translateX(-50%) translateY(0);
}
</style>
