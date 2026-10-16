<template>
  <nav class="home-quick-nav" aria-label="快捷入口">
    <a
      v-for="item in quickLinks"
      :key="item.label"
      href="#"
      class="home-quick-item"
      @click.prevent
    >
      <span class="home-quick-icon" :style="{ background: item.bg, color: item.color }">{{ item.icon }}</span>
      <span class="home-quick-label">{{ item.label }}</span>
    </a>
  </nav>
</template>

<script setup lang="ts">
const quickLinks = [
  { label: '限时秒杀', icon: '秒', bg: 'var(--accent-soft)', color: 'var(--accent)' },
  { label: '免费开店', icon: '店', bg: '#fef3c7', color: '#d97706' },
  { label: '热卖榜单', icon: '榜', bg: '#fce7f3', color: '#db2777' },
  { label: '品牌优选', icon: '品', bg: '#eff6ff', color: '#2563eb' },
  { label: '天天低价', icon: '低', bg: '#ecfdf5', color: '#059669' },
  { label: '品质好物', icon: '优', bg: '#ecfeff', color: '#0891b2' },
  { label: '领券中心', icon: '券', bg: '#fff7ed', color: '#ea580c' },
  { label: '更多频道', icon: '…', bg: '#f5f3ff', color: '#7c3aed' },
]
</script>

<style scoped>
.home-quick-nav {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 0;
  margin: 0 0 16px;
  padding: 14px 18px;
  background: var(--home-surface);
  border: 1px solid var(--home-border);
  border-radius: 8px;
  box-shadow: var(--home-card-shadow);
}

.home-quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-radius: 6px;
  transition: background 0.18s, color 0.18s;
}

.home-quick-item:hover {
  background: var(--accent-soft);
}

.home-quick-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 700;
  box-shadow: none;
}

.home-quick-label {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.home-quick-item:hover .home-quick-label {
  color: var(--accent);
}

@media (max-width: 992px) {
  .home-quick-nav {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-quick-nav {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    padding: 12px;
    gap: 8px;
  }

  .home-quick-icon {
    width: 40px;
    height: 40px;
    font-size: 14px;
  }
}
</style>
