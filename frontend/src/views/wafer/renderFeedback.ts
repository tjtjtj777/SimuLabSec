const SINGLE_WAFER_POINT_COUNT = 12_000

export function estimateRenderedPointsK(waferCount: number): string {
  const totalPoints = Math.max(0, waferCount) * SINGLE_WAFER_POINT_COUNT
  return `${(totalPoints / 1000).toFixed(1)}k`
}

export function normalizeElapsedMs(elapsedMs: number): number {
  if (!Number.isFinite(elapsedMs) || elapsedMs < 0) {
    return 0
  }
  return Math.round(elapsedMs)
}
