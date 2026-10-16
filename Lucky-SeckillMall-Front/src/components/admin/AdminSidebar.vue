<template>
  <aside class="account-sidebar admin-sidebar">
    <div class="account-sidebar-head">系统管理</div>
    <nav class="account-nav">
      <div class="nav-group">
        <h3 class="nav-group-title">管理模块</h3>
        <router-link
          v-for="item in ADMIN_NAV_ITEMS"
          :key="item.routeName"
          :to="{ name: item.routeName }"
          class="nav-item"
          active-class="nav-item--active"
        >
          {{ item.label }}
        </router-link>
      </div>
    </nav>
  </aside>
</template>

<script setup lang="ts">
import { ADMIN_NAV_ITEMS } from '@/constants/admin'
</script>

<style scoped>
.admin-sidebar {
  flex: 0 0 180px;
}
</style>
