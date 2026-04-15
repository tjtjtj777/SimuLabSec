import { describe, expect, it, vi } from 'vitest'
import { useSubRecipeCenter } from '@/views/subrecipe/useSubRecipeCenter'

vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
  },
}))

vi.mock('@/api/modules/subrecipe', () => ({
  subRecipeApi: {
    getList: vi.fn().mockResolvedValue([{ id: 1, subRecipeCode: 'SUB-1', status: 'READY', generationType: 'AUTO' }]),
    getDetail: vi.fn().mockResolvedValue({ id: 1, subRecipeCode: 'SUB-1' }),
    getUploadTicket: vi.fn().mockResolvedValue({ action: 'UPLOAD', objectPath: '/upload/demo' }),
    getDownloadTicket: vi.fn().mockResolvedValue({ action: 'DOWNLOAD', objectPath: '/download/demo' }),
    getExportTicket: vi.fn().mockResolvedValue({ action: 'EXPORT', objectPath: '/export/demo' }),
  },
}))

describe('useSubRecipeCenter', () => {
  it('loads list and handles ticket actions', async () => {
    const vm = useSubRecipeCenter()
    await vm.loadList()
    await vm.openDetail(1)
    const upload = await vm.requestUploadTicket('a.json', 'application/json')
    const download = await vm.requestDownloadTicket(1)
    const exp = await vm.requestExportTicket(1)

    expect(vm.items.value.length).toBe(1)
    expect(vm.selectedDetail.value?.subRecipeCode).toBe('SUB-1')
    expect(upload.action).toBe('UPLOAD')
    expect(download.action).toBe('DOWNLOAD')
    expect(exp.action).toBe('EXPORT')
  })
})
