<script setup lang="ts">
import { RouterView } from 'vue-router'
import ToastNotification from './components/ToastNotification.vue'
import { useToast } from './composables/useToast'
import { ref, onMounted } from 'vue'

const { setRef, currentToast } = useToast()
const toastComponent = ref(null)

onMounted(() => {
  if (toastComponent.value) {
    setRef(toastComponent.value)
  }
})
</script>

<template>
  <RouterView />
  <ToastNotification 
    ref="toastComponent"
    :message="currentToast.message"
    :type="currentToast.type"
    :duration="currentToast.duration"
  />
</template>

<style scoped>
</style>
