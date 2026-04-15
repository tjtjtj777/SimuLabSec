import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import { subRecipeApi } from '@/api/modules/subrecipe'
import type { SubRecipeDetail, SubRecipeFileTicket, SubRecipeItem } from '@/types/domain'

export function useSubRecipeCenter() {
  const loading = ref(false)
  const detailLoading = ref(false)
  const items = ref<SubRecipeItem[]>([])
  const selectedDetail = ref<SubRecipeDetail>()
  const detailVisible = ref(false)
  const ticketVisible = ref(false)
  const latestTicket = ref<SubRecipeFileTicket>()
  const exportFormat = ref('JSON')
  const filters = ref({
    status: '',
    generationType: '',
  })

  async function loadList() {
    loading.value = true
    try {
      items.value = await subRecipeApi.getList({
        status: filters.value.status || undefined,
        generationType: filters.value.generationType || undefined,
      })
    } finally {
      loading.value = false
    }
  }

  async function openDetail(id: number) {
    detailLoading.value = true
    detailVisible.value = true
    try {
      selectedDetail.value = await subRecipeApi.getDetail(id)
    } finally {
      detailLoading.value = false
    }
  }

  async function requestUploadTicket(fileName: string, fileType: string) {
    const ticket = await subRecipeApi.getUploadTicket({ fileName, fileType })
    latestTicket.value = ticket
    ticketVisible.value = true
    return ticket
  }

  async function requestDownloadTicket(id: number) {
    const ticket = await subRecipeApi.getDownloadTicket(id)
    latestTicket.value = ticket
    ticketVisible.value = true
    return ticket
  }

  async function requestExportTicket(id: number) {
    const ticket = await subRecipeApi.getExportTicket(id, { exportFormat: exportFormat.value })
    latestTicket.value = ticket
    ticketVisible.value = true
    return ticket
  }

  async function copyTicketPath() {
    if (!latestTicket.value?.objectPath) {
      return
    }
    await navigator.clipboard.writeText(latestTicket.value.objectPath)
    ElMessage.success('Path copied')
  }

  return {
    loading,
    detailLoading,
    items,
    selectedDetail,
    detailVisible,
    ticketVisible,
    latestTicket,
    exportFormat,
    filters,
    loadList,
    openDetail,
    requestUploadTicket,
    requestDownloadTicket,
    requestExportTicket,
    copyTicketPath,
  }
}
