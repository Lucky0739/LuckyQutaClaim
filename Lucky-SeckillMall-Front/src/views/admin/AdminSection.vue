<template>
  <section>
    <div class="account-tabs">
      <span class="account-tab active admin-page-tab">{{ pageTitle }}</span>
    </div>

    <div class="account-panel admin-panel">
      <p class="admin-desc">{{ pageDesc }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const pageTitle = computed(() => (route.meta.title as string) || '系统管理')
const pageDesc = computed(
  () => (route.meta.description as string) || '功能开发中，敬请期待。',
)
</script>

<style scoped>
.admin-page-tab {
  cursor: default;
}

.admin-panel {
  padding-top: 20px;
}

.admin-desc {
  margin: 0;
  font-size: 14px;
  color: #999;
  line-height: 1.8;
}
</style>
