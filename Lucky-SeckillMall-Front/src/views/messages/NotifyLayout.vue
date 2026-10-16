<template>
  <div class="page">
    <Header top-only />

    <main class="account-page">
      <div class="account-page-inner">
        <nav class="account-breadcrumb">
          <router-link to="/">首页</router-link>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span>消息中心</span>
          <span class="account-breadcrumb-sep">&gt;</span>
          <span class="account-breadcrumb-current">{{ pageTitle }}</span>
        </nav>

        <div class="account-container">
          <NotifySidebar />
          <div class="account-content">
            <router-view />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from '@/components/Header.vue'
import NotifySidebar from '@/components/messages/NotifySidebar.vue'

const route = useRoute()
const pageTitle = computed(() => (route.meta.title as string) || '消息中心')
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f4f4f4;
}
</style>
