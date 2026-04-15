import type { PageResult } from '@/types/api'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  displayName?: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  displayName: string
  roles: string[]
}

export interface CurrentUser {
  userId: number
  username: string
  displayName: string
  roles: string[]
}

export interface DashboardOverview {
  totalLots: number
  totalWafers: number
  runningTasks: number
  successRate: number
  passRate: number
  avgOverlay: number
  maxOverlay: number
  releasedRecipeCount: number
}

export interface LotItem {
  id: number
  lotNo: string
  lotStatus: string
  priorityLevel: string
  sourceType: string
  waferCount: number
  dataScope?: string
  editable?: boolean
  deletable?: boolean
}

export interface WaferItem {
  id: number
  lotId: number
  waferNo: string
  waferStatus: string
  slotNo: number
  diameterMm: number
  dataScope?: string
}

export interface LayerItem {
  id: number
  layerCode: string
  layerName: string
  layerType: string
  sequenceNo: number
  status: string
  dataScope?: string
}

export interface SimulationTaskItem {
  id: string | number
  taskNo: string
  taskName: string
  scenarioType: string
  status: string
  lotId: number
  layerId: number
  recipeVersionId: number
  priorityLevel: string
  errorMessage: string
  createdAt: string
  dataScope?: string
}

export interface SimulationTaskStatusSummary {
  status: string
  count: number
}

export interface SimulationTaskDetail {
  id: string | number
  taskNo: string
  taskName: string
  lotId: number
  layerId: number
  recipeVersionId: number
  scenarioType: string
  status: string
  priorityLevel: string
  idempotencyKey: string
  inputSnapshotJson: string
  executionContextJson: string
  resultSummaryJson: string
  errorMessage: string
  requestedBy: number
  createdAt: string
  updatedAt: string
}

export interface RecipeItem {
  id: number
  recipeCode: string
  recipeName: string
  recipeType: string
  status: string
  dataScope?: string
}

export interface RecipeVersionItem {
  id: number
  recipeId: number
  versionNo: number
  versionLabel: string
  status: string
  changeSummary: string
}

export interface RecipeParamDiff {
  paramName: string
  leftValue: string | null
  rightValue: string | null
}

export interface RecipeVersionCompare {
  leftVersionId: number
  rightVersionId: number
  leftVersionLabel: string
  rightVersionLabel: string
  diffs: RecipeParamDiff[]
}

export interface DemoDatasetItem {
  id: number
  datasetCode: string
  datasetName: string
  scenarioType: string
  status: string
  description: string
}

export interface MeasurementRunItem {
  id: string | number
  runNo: string
  lotId: number
  waferId: number
  layerId: number
  measurementType: string
  stage: string
  sourceType: string
  samplingCount: number
  status: string
  dataScope?: string
}

export interface WaferImportFieldMapping {
  xCoordColumn: string
  yCoordColumn: string
  targetCodeColumn?: string
  overlayXColumn?: string
  overlayYColumn?: string
  overlayMagnitudeColumn?: string
  residualColumn?: string
  focusColumn?: string
  doseColumn?: string
  confidenceColumn?: string
  outlierColumn?: string
}

export interface WaferAnalysisImportConfig {
  lotNo: string
  lotStatus: string
  priorityLevel: string
  lotRemark?: string
  waferNo: string
  waferStatus: string
  slotNo: number
  diameterMm: number
  layerId: number
  runNo?: string
  measurementType: string
  stage: string
  toolName?: string
  hasHeader: boolean
  generateMagnitudeWhenMissing: boolean
  outlierThreshold?: number
  fieldMapping: WaferImportFieldMapping
}

export interface WaferAnalysisImportResult {
  imported: boolean
  status: string
  message: string
  lotId: number
  waferId: number
  measurementRunId: number
  measurementRunNo: string
  totalRows: number
  insertedRows: number
  skippedOutsideRows: number
  failedRows: number
  elapsedMs: number
  errors: string[]
}

export interface OverlayHeatmapPoint {
  targetCode: string
  xCoord: number
  yCoord: number
  xcoord?: number
  ycoord?: number
  metricValue: number
  confidence: number
  outlier: number
}

export interface OverlayHeatmapBatchItem {
  measurementRunId: string | number
  success: boolean
  error?: string
  points: OverlayHeatmapPoint[]
}

export interface OverlayScatterPoint {
  targetCode: string
  xValue: number
  yValue: number
  overlayMagnitude: number
  outlier: number
}

export interface OverlayHistogramBin {
  rangeStart: number
  rangeEnd: number
  count: number
}

export interface OverlayTrendPoint {
  date: string
  label?: string
  passRate: number
  meanOverlay: number
  p95Overlay: number
}

export interface WaferConfigValidationRule {
  field: string
  label: string
  rule: string
  recommended: string
}

export interface WaferConfigItem {
  id?: string | number
  configNo?: string
  configName: string
  description?: string
  lotNo: string
  waferNo: string
  layerId: number
  measurementType: string
  stage: string
  scannerCorrectionGain: number
  overlayBaseNm: number
  edgeGradient: number
  localHotspotStrength: number
  noiseLevel: number
  gridStep: number
  outlierThreshold: number
  dataScope?: string
  editable?: number
  deletable?: number
  lastMeasurementRunId?: string | number
  lastTaskId?: string | number
  updatedAt?: string
  validationRules?: WaferConfigValidationRule[]
}

export interface WaferGenerateResult {
  configId?: string | number
  taskId: string | number
  taskNo: string
  measurementRunId: string | number
  measurementRunNo: string
  lotId?: string | number
  waferId?: string | number
  layerId?: string | number
  generatedPoints: number
  meanOverlay: number
  p95Overlay: number
  maxOverlay: number
  stdOverlay: number
  passRate: number
  overallQuality: string
  overlayStability: string
  edgeRisk: string
  outlierDensity: string
  parameterSensitivity: string
  recommendedAction: string
  summaryText: string
  reuseHit?: boolean
  reusedRunId?: string | number
  reusedTaskId?: string | number
  validationErrors: string[]
  elapsedMs: number
}

export interface WaferHeatmapBatchTaskStart {
  taskId: string | number
  taskNo: string
  status: string
  requestedRuns: number
}

export interface SubRecipeItem {
  id: number
  subRecipeCode: string
  recipeVersionId: number
  sourceTaskId: number
  lotId: number
  waferId: number
  status: string
  generationType: string
  exportFormat: string
  dataScope?: string
}

export interface SubRecipeDetail {
  id: number
  subRecipeCode: string
  recipeVersionId: number
  sourceTaskId: number
  lotId: number
  waferId: number
  status: string
  generationType: string
  exportFormat: string
  paramDeltaJson: string
  paramSetJson: string
  createdAt: string
}

export interface SubRecipeFileTicket {
  subRecipeId?: number
  fileName: string
  fileType: string
  action: 'UPLOAD' | 'DOWNLOAD' | 'EXPORT'
  objectPath: string
  expireAt: string
}

export interface PageQuery {
  pageNo?: number
  pageSize?: number
}

export type LotPage = PageResult<LotItem>
export type WaferPage = PageResult<WaferItem>
export type LayerPage = PageResult<LayerItem>
