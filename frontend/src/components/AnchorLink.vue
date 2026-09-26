<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

defineOptions({ inheritAttrs: false })
defineProps<{ to: RouteLocationRaw }>()
</script>

<template>
  <RouterLink v-slot="{ href, navigate }" :to="to" custom>
    <a :href="href" v-bind="$attrs" @click="navigate"><slot /></a>
  </RouterLink>
</template>
