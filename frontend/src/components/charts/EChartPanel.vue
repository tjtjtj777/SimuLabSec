<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  title: string
  option: echarts.EChartsOption
  height?: number
}>()

const containerRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!containerRef.value) {
    return
  }
  chart ??= echarts.init(containerRef.value)
  chart.setOption(props.option, true)
}

const onResize = () => chart?.resize()

onMounted(() => {
  render()
  window.addEventListener('resize', onResize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<template>
  <el-card>
    <template #header>{{ title }}</template>
    <div ref="containerRef" :style="{ height: `${height ?? 260}px` }" />
  </el-card>
</template>
