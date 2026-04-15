import { describe, expect, it, vi } from 'vitest'
import { useTaskCenter } from '@/views/tasks/useTaskCenter'

vi.mock('@/api/modules/simulation-task', () => ({
  simulationTaskApi: {
    getList: vi.fn().mockResolvedValue([
      { id: 2, taskNo: 'T-2', taskName: 'Mine newer', scenarioType: 'NORMAL', status: 'SUCCESS', createdAt: '2026-04-12 10:00:00', dataScope: 'MINE' },
      { id: 1, taskNo: 'T-1', taskName: 'Demo older', scenarioType: 'NORMAL', status: 'SUCCESS', createdAt: '2026-04-11 10:00:00', dataScope: 'DEMO' },
    ]),
    getStatusSummary: vi.fn().mockResolvedValue([{ status: 'SUCCESS', count: 2 }]),
    getDetail: vi.fn().mockResolvedValue({ id: 1, taskNo: 'T-1', taskName: 'Demo', status: 'SUCCESS' }),
  },
}))

describe('useTaskCenter', () => {
  it('loads task list and task detail', async () => {
    const vm = useTaskCenter()
    await vm.loadTasks()
    await vm.openTaskDetail(1)

    expect(vm.tasks.value.length).toBe(2)
    expect(vm.tasks.value[0].dataScope).toBe('DEMO')
    expect(vm.statusSummary.value[0].status).toBe('SUCCESS')
    expect(vm.selectedTaskDetail.value?.taskNo).toBe('T-1')
  })
})
