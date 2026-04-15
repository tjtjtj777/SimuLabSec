import { storage } from '@/utils/storage'

const MULTI_WAFER_ASYNC_STATE_PREFIX = 'simulab.multiWafer.async'

export type MultiWaferAsyncStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'

export type MultiWaferAsyncState = {
  taskId: string | number
  status: MultiWaferAsyncStatus
  selectedRunIds: Array<string | number>
  completionNotified: boolean
  updatedAt: number
}

function buildKey() {
  const username = storage.getUsername() || 'anonymous'
  return `${MULTI_WAFER_ASYNC_STATE_PREFIX}:${username}`
}

export function getMultiWaferAsyncState(): MultiWaferAsyncState | null {
  const raw = localStorage.getItem(buildKey())
  if (!raw) {
    return null
  }
  try {
    const parsed = JSON.parse(raw) as MultiWaferAsyncState
    if (!parsed?.taskId || !parsed?.status) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

export function setMultiWaferAsyncState(state: MultiWaferAsyncState) {
  localStorage.setItem(buildKey(), JSON.stringify(state))
}

export function clearMultiWaferAsyncState() {
  localStorage.removeItem(buildKey())
}
