<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

type HeatPoint = { xCoord: number; yCoord: number; metricValue: number; outlier?: number }

const props = withDefaults(
  defineProps<{
    points?: HeatPoint[]
    width?: number
    height?: number
  }>(),
  {
    points: () => [],
    width: 420,
    height: 320,
  },
)

const canvasRef = ref<HTMLCanvasElement>()
const axisMin = 0
const axisMax = 250
const center = 125
const radius = 125
const padding = { top: 16, right: 24, bottom: 34, left: 40 }

const normalizedPoints = computed<HeatPoint[]>(() =>
  props.points
    .map((point) => ({
      xCoord: Number(point.xCoord),
      yCoord: Number(point.yCoord),
      metricValue: Number(point.metricValue),
      outlier: point.outlier ?? 0,
    }))
    .filter((point) => Number.isFinite(point.xCoord) && Number.isFinite(point.yCoord) && Number.isFinite(point.metricValue)),
)

const metricRange = computed(() => {
  if (!normalizedPoints.value.length) {
    return { min: 0, max: 0 }
  }
  let min = Number.POSITIVE_INFINITY
  let max = Number.NEGATIVE_INFINITY
  for (const point of normalizedPoints.value) {
    if (point.metricValue < min) min = point.metricValue
    if (point.metricValue > max) max = point.metricValue
  }
  return { min, max }
})

const visiblePoints = computed(() =>
  normalizedPoints.value.filter((point) => {
    if (point.xCoord < axisMin || point.xCoord > axisMax || point.yCoord < axisMin || point.yCoord > axisMax) {
      return false
    }
    const dx = point.xCoord - center
    const dy = point.yCoord - center
    return (dx * dx + dy * dy) <= (radius * radius)
  }),
)

const palette = Array.from({ length: 256 }, (_, idx) => {
  const ratio = idx / 255
  const hue = 220 - ratio * 220
  return `hsl(${hue}, 78%, 55%)`
})
let rafId = 0

function draw() {
  const drawStarted = performance.now()
  const canvas = canvasRef.value
  if (!canvas) {
    return
  }
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return
  }
  const dpr = window.devicePixelRatio || 1
  canvas.width = Math.floor(props.width * dpr)
  canvas.height = Math.floor(props.height * dpr)
  canvas.style.width = `${props.width}px`
  canvas.style.height = `${props.height}px`
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)

  const plotWidth = props.width - padding.left - padding.right
  const plotHeight = props.height - padding.top - padding.bottom
  const plotSize = Math.min(plotWidth, plotHeight)
  const plotLeft = padding.left + (plotWidth - plotSize) / 2
  const plotTop = padding.top + (plotHeight - plotSize) / 2

  const toPixelX = (value: number) => plotLeft + ((value - axisMin) / (axisMax - axisMin)) * plotSize
  const toPixelY = (value: number) => plotTop + plotSize - ((value - axisMin) / (axisMax - axisMin)) * plotSize
  const circleCenterX = toPixelX(center)
  const circleCenterY = toPixelY(center)
  const circleRadius = (radius / (axisMax - axisMin)) * plotSize

  ctx.clearRect(0, 0, props.width, props.height)

  ctx.fillStyle = '#f7fbff'
  ctx.fillRect(0, 0, props.width, props.height)

  ctx.beginPath()
  ctx.arc(circleCenterX, circleCenterY, circleRadius, 0, Math.PI * 2)
  ctx.fillStyle = '#eef4fc'
  ctx.fill()
  ctx.lineWidth = 2
  ctx.strokeStyle = '#7e9cbc'
  ctx.stroke()
  ctx.save()
  ctx.clip()

  const { min, max } = metricRange.value
  const span = max === min ? 1 : max - min
  for (const point of visiblePoints.value) {
    // 轻量像素对齐：减少子像素绘制带来的细线/锯齿伪影，不改变点位数量与颜色策略。
    const px = Math.round(toPixelX(point.xCoord))
    const py = Math.round(toPixelY(point.yCoord))
    const ratio = (point.metricValue - min) / span
    const index = Math.max(0, Math.min(255, Math.round(ratio * 255)))
    ctx.fillStyle = palette[index]
    if ((point.outlier ?? 0) === 1) {
      ctx.fillRect(px - 1.5, py - 1.5, 3, 3)
      ctx.strokeStyle = '#7a1616'
      ctx.lineWidth = 1
      ctx.strokeRect(px - 2.5, py - 2.5, 5, 5)
    } else {
      ctx.fillRect(px - 1, py - 1, 2, 2)
    }
  }
  ctx.restore()

  ctx.strokeStyle = '#4a6484'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(plotLeft, plotTop + plotSize)
  ctx.lineTo(plotLeft + plotSize, plotTop + plotSize)
  ctx.moveTo(plotLeft, plotTop)
  ctx.lineTo(plotLeft, plotTop + plotSize)
  ctx.stroke()

  ctx.fillStyle = '#304965'
  ctx.font = '12px Segoe UI'
  ctx.fillText('0', plotLeft - 4, plotTop + plotSize + 14)
  ctx.fillText('250', plotLeft + plotSize - 16, plotTop + plotSize + 14)
  ctx.fillText('0', plotLeft - 18, plotTop + plotSize + 4)
  ctx.fillText('250', plotLeft - 24, plotTop + 4)
  ctx.fillText('X: 0-250', plotLeft + plotSize - 58, plotTop + plotSize + 28)
  ctx.save()
  ctx.translate(plotLeft - 28, plotTop + 42)
  ctx.rotate(-Math.PI / 2)
  ctx.fillText('Y: 0-250', 0, 0)
  ctx.restore()
  console.info('[wafer-heatmap][frontend][draw]', {
    points: visiblePoints.value.length,
    elapsedMs: Number((performance.now() - drawStarted).toFixed(2)),
  })
}

onMounted(draw)
watch(
  () => [visiblePoints.value.length, metricRange.value.min, metricRange.value.max, props.width, props.height],
  () => {
    cancelAnimationFrame(rafId)
    rafId = requestAnimationFrame(draw)
  },
  { deep: false },
)
</script>

<template>
  <div class="heatmap-wrap">
    <canvas ref="canvasRef" />
    <div class="meta">
      <span>{{ metricRange.min.toFixed(3) }}</span>
      <div class="legend-bar" />
      <span>{{ metricRange.max.toFixed(3) }}</span>
    </div>
  </div>
</template>

<style scoped>
.heatmap-wrap {
  width: 100%;
}

.meta {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #385272;
  font-size: 12px;
}

.legend-bar {
  height: 8px;
  flex: 1;
  border-radius: 999px;
  background: linear-gradient(90deg, hsl(220, 78%, 55%) 0%, hsl(160, 78%, 55%) 33%, hsl(60, 78%, 55%) 66%, hsl(0, 78%, 55%) 100%);
}
</style>
