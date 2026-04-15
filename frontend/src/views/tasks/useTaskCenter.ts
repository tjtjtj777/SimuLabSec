import { ref } from 'vue'
import { simulationTaskApi } from '@/api/modules/simulation-task'
import type { SimulationTaskDetail, SimulationTaskItem, SimulationTaskStatusSummary } from '@/types/domain'

export function useTaskCenter() {
  const loading = ref(false)
  const detailLoading = ref(false)
  const tasks = ref<SimulationTaskItem[]>([])
  const pagedTasks = ref<SimulationTaskItem[]>([])
  const statusSummary = ref<SimulationTaskStatusSummary[]>([])
  const selectedTaskDetail = ref<SimulationTaskDetail>()
  const detailVisible = ref(false)
  const tablePageNo = ref(1)
  const tablePageSize = ref(10)
  const tableTotal = ref(0)
  const filters = ref({
    keyword: '',
    status: '',
    scenarioType: '',
  })

  async function loadTasks() {
    loading.value = true
    try {
      const params = {
        keyword: filters.value.keyword || undefined,
        status: filters.value.status || undefined,
        scenarioType: filters.value.scenarioType || undefined,
      }
      const [list, summary] = await Promise.all([
        simulationTaskApi.getList(params),
        simulationTaskApi.getStatusSummary(params),
      ])
      tasks.value = list.slice().sort((a, b) => {
        const scopeA = a.dataScope ?? 'DEMO'
        const scopeB = b.dataScope ?? 'DEMO'
        if (scopeA !== scopeB) {
          return scopeA === 'DEMO' ? -1 : 1
        }
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })
      tableTotal.value = list.length
      if ((tablePageNo.value - 1) * tablePageSize.value >= tableTotal.value) {
        tablePageNo.value = 1
      }
      const start = (tablePageNo.value - 1) * tablePageSize.value
      pagedTasks.value = tasks.value.slice(start, start + tablePageSize.value)
      statusSummary.value = summary
    } finally {
      loading.value = false
    }
  }

  function onTablePageChange(pageNo: number) {
    tablePageNo.value = pageNo
    const start = (tablePageNo.value - 1) * tablePageSize.value
    pagedTasks.value = tasks.value.slice(start, start + tablePageSize.value)
    console.info('[task-list][frontend][table-render]', {
      pageNo: tablePageNo.value,
      pageSize: tablePageSize.value,
      renderedRows: pagedTasks.value.length,
      totalRows: tableTotal.value,
    })
  }

  async function openTaskDetail(taskId: string | number) {
    detailLoading.value = true
    detailVisible.value = true
    try {
      selectedTaskDetail.value = await simulationTaskApi.getDetail(taskId)
    } finally {
      detailLoading.value = false
    }
  }

  return {
    loading,
    detailLoading,
    tasks,
    pagedTasks,
    statusSummary,
    selectedTaskDetail,
    detailVisible,
    tablePageNo,
    tablePageSize,
    tableTotal,
    filters,
    loadTasks,
    openTaskDetail,
    onTablePageChange,
  }
}
