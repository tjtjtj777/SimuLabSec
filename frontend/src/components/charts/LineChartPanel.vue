<script setup lang="ts">
import * as echarts from 'echarts'
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{
  title: string
  categories: string[]
  values: number[]
}>()

const containerRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

function render() {
  if (!containerRef.value) {
    return
  }
  chart ??= echarts.init(containerRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: props.categories, boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'line',
        smooth: true,
        data: props.values,
        areaStyle: { color: 'rgba(70, 129, 204, 0.15)' },
        itemStyle: { color: '#3f7ec8' },
      },
    ],
  })
}

const onResize = () => chart?.resize()

onMounted(() => {
  render()
  window.addEventListener('resize', onResize)
})

watch(() => [props.categories, props.values], render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<template>
  <el-card>
    <template #header>{{ title }}</template>
    <div ref="containerRef" class="chart" />
  </el-card>
</template>

<style scoped>
.chart {
  height: 260px;
}
</style>
