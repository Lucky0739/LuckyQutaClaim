<template>
  <nav class="favorites-nav" aria-label="收藏分类">
    <router-link
      to="/favorites/goods"
      class="favorites-nav-item"
      active-class="favorites-nav-item--active"
    >
      收藏商品
      <span v-if="(goodsCount ?? 0) > 0" class="favorites-nav-badge">{{ formatCount(goodsCount ?? 0) }}</span>
    </router-link>
    <router-link
      to="/favorites/shops"
      class="favorites-nav-item"
      active-class="favorites-nav-item--active"
    >
      收藏店铺
    </router-link>
  </nav>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    goodsCount?: number
  }>(),
  { goodsCount: 0 },
)

function formatCount(count: number) {
  return count > 99 ? '99+' : String(count)
}
</script>

<style scoped>
.favorites-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.favorites-nav-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 18px;
  border-radius: 999px;
  background: #f5f5f5;
  color: #666;
  font-size: 14px;
  text-decoration: none;
  transition: background 0.18s, color 0.18s;
}

.favorites-nav-item:hover {
  color: var(--accent);
  background: #fff3eb;
}

.favorites-nav-item--active {
  color: #fff;
  background: var(--accent);
  font-weight: 600;
}

.favorites-nav-item--active:hover {
  color: #fff;
}

.favorites-nav-badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.28);
  font-size: 11px;
  line-height: 18px;
  text-align: center;
}

.favorites-nav-item:not(.favorites-nav-item--active) .favorites-nav-badge {
  background: var(--accent);
  color: #fff;
}
</style>
