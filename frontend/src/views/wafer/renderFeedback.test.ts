import { describe, expect, it } from 'vitest'
import { estimateRenderedPointsK, normalizeElapsedMs } from '@/views/wafer/renderFeedback'

describe('renderFeedback helpers', () => {
  it('formats point count with k unit based on wafer count', () => {
    expect(estimateRenderedPointsK(1)).toBe('12.0k')
    expect(estimateRenderedPointsK(4)).toBe('48.0k')
  })

  it('normalizes elapsed ms to non-negative integer', () => {
    expect(normalizeElapsedMs(1280.4)).toBe(1280)
    expect(normalizeElapsedMs(-1)).toBe(0)
    expect(normalizeElapsedMs(Number.NaN)).toBe(0)
  })
})
