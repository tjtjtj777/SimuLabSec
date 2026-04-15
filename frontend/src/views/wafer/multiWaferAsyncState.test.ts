import { beforeEach, describe, expect, it } from 'vitest'
import {
  clearMultiWaferAsyncState,
  getMultiWaferAsyncState,
  setMultiWaferAsyncState,
} from '@/views/wafer/multiWaferAsyncState'

describe('multiWaferAsyncState', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('simulab.username', 'demo')
  })

  it('persists and restores async task state', () => {
    setMultiWaferAsyncState({
      taskId: 9001,
      status: 'RUNNING',
      selectedRunIds: [1, 2, 3, 4, 5],
      completionNotified: false,
      updatedAt: 123,
    })
    const restored = getMultiWaferAsyncState()
    expect(restored?.taskId).toBe(9001)
    expect(restored?.status).toBe('RUNNING')
    expect(restored?.selectedRunIds.length).toBe(5)
  })

  it('clears async task state', () => {
    setMultiWaferAsyncState({
      taskId: 9002,
      status: 'SUCCESS',
      selectedRunIds: [1],
      completionNotified: true,
      updatedAt: 456,
    })
    clearMultiWaferAsyncState()
    expect(getMultiWaferAsyncState()).toBeNull()
  })
})
