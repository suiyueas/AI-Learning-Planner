<template>
  <div class="app-container">
    <template v-if="isAuthPage">
      <main class="auth-main">
        <router-view v-slot="{ Component }">
          <keep-alive :max="10">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </template>
    <template v-else>
      <AppLayout>
        <router-view v-slot="{ Component }">
          <keep-alive :max="10">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </AppLayout>
    </template>
    <GlobalLoading />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import GlobalLoading from '@/components/GlobalLoading.vue'
import AppLayout from '@/layouts/AppLayout.vue'

const route = useRoute()
const authStore = useAuthStore()

const isAuthPage = computed(() => route.meta.layout === 'auth')

onMounted(() => {
  authStore.initAuth()
})
</script>

<style lang="scss" scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.auth-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>