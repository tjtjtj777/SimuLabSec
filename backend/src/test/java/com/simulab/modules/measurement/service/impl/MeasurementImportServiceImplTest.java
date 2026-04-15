package com.simulab.modules.measurement.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.simulab.common.security.SecurityUser;
import com.simulab.modules.fileimport.entity.ImportFileRecord;
import com.simulab.modules.fileimport.mapper.ImportFileRecordMapper;
import com.simulab.modules.layer.entity.Layer;
import com.simulab.modules.layer.mapper.LayerMapper;
import com.simulab.modules.lot.entity.Lot;
import com.simulab.modules.lot.mapper.LotMapper;
import com.simulab.modules.measurement.dto.WaferAnalysisImportConfigDto;
import com.simulab.modules.measurement.dto.WaferImportFieldMappingDto;
import com.simulab.modules.measurement.entity.MeasurementRun;
import com.simulab.modules.measurement.mapper.MeasurementRunMapper;
import com.simulab.modules.measurement.vo.WaferAnalysisImportResultVo;
import com.simulab.modules.overlayanalysis.entity.OverlayMeasurementPoint;
import com.simulab.modules.overlayanalysis.mapper.OverlayMeasurementPointMapper;
import com.simulab.modules.wafer.entity.Wafer;
import com.simulab.modules.wafer.mapper.WaferMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MeasurementImportServiceImplTest {

    @Mock
    private LotMapper lotMapper;
    @Mock
    private WaferMapper waferMapper;
    @Mock
    private LayerMapper layerMapper;
    @Mock
    private MeasurementRunMapper measurementRunMapper;
    @Mock
    private OverlayMeasurementPointMapper overlayMeasurementPointMapper;
    @Mock
    private ImportFileRecordMapper importFileRecordMapper;

    @BeforeEach
    void setupAuth() {
        SecurityUser securityUser = SecurityUser.builder().userId(1001L).username("u1").build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void importWaferAnalysisCsvShouldImportAndSummarizeRows() {
        MeasurementImportServiceImpl service = new MeasurementImportServiceImpl(
            lotMapper, waferMapper, layerMapper, measurementRunMapper, overlayMeasurementPointMapper, importFileRecordMapper);

        Layer layer = new Layer();
        layer.setId(101L);
        layer.setDeleted(0);
        layer.setCreatedBy(0L);
        when(layerMapper.selectById(101L)).thenReturn(layer);
        when(lotMapper.selectOne(any())).thenReturn(null);
        when(waferMapper.selectOne(any())).thenReturn(null);
        when(measurementRunMapper.selectCount(any())).thenReturn(0L);
        when(lotMapper.insert(any(Lot.class))).thenAnswer(invocation -> {
            Lot lot = invocation.getArgument(0);
            lot.setId(2001L);
            return 1;
        });
        when(waferMapper.insert(any(Wafer.class))).thenAnswer(invocation -> {
            Wafer wafer = invocation.getArgument(0);
            wafer.setId(3001L);
            return 1;
        });
        when(importFileRecordMapper.insert(any(ImportFileRecord.class))).thenAnswer(invocation -> {
            ImportFileRecord record = invocation.getArgument(0);
            record.setId(4001L);
            return 1;
        });
        when(measurementRunMapper.insert(any(MeasurementRun.class))).thenAnswer(invocation -> {
            MeasurementRun run = invocation.getArgument(0);
            run.setId(5001L);
            return 1;
        });
        when(overlayMeasurementPointMapper.insert(any(OverlayMeasurementPoint.class))).thenReturn(1);

        WaferAnalysisImportConfigDto config = new WaferAnalysisImportConfigDto();
        config.setLotNo("LOT-U-001");
        config.setWaferNo("W01");
        config.setLayerId(101L);
        config.setDiameterMm(new BigDecimal("300"));
        WaferImportFieldMappingDto mapping = new WaferImportFieldMappingDto();
        mapping.setTargetCodeColumn("target_code");
        mapping.setXCoordColumn("x_coord");
        mapping.setYCoordColumn("y_coord");
        mapping.setOverlayXColumn("overlay_x");
        mapping.setOverlayYColumn("overlay_y");
        config.setFieldMapping(mapping);

        String csv = "target_code,x_coord,y_coord,overlay_x,overlay_y\n"
            + "P001,125,125,1.2,-0.8\n"
            + "P002,250,250,0.1,0.2\n"
            + "P003,abc,120,0.3,0.1\n";
        MockMultipartFile file = new MockMultipartFile("file", "points.csv", "text/csv", csv.getBytes());

        WaferAnalysisImportResultVo result = service.importWaferAnalysisCsv(file, config);

        assertTrue(result.isImported());
        assertEquals("IMPORTED", result.getStatus());
        assertEquals(3, result.getTotalRows());
        assertEquals(1, result.getInsertedRows());
        assertEquals(1, result.getSkippedOutsideRows());
        assertEquals(1, result.getFailedRows());
        assertEquals(5001L, result.getMeasurementRunId());
    }

    @Test
    void buildImportTemplateCsvShouldContainHeader() {
        MeasurementImportServiceImpl service = new MeasurementImportServiceImpl(
            lotMapper, waferMapper, layerMapper, measurementRunMapper, overlayMeasurementPointMapper, importFileRecordMapper);
        String template = service.buildImportTemplateCsv();
        assertTrue(template.startsWith("target_code,x_coord,y_coord"));
    }

    @Test
    void importWaferAnalysisCsvShouldFallbackToDefaultMappingWhenColumnsMissingInConfig() {
        MeasurementImportServiceImpl service = new MeasurementImportServiceImpl(
            lotMapper, waferMapper, layerMapper, measurementRunMapper, overlayMeasurementPointMapper, importFileRecordMapper);

        Layer layer = new Layer();
        layer.setId(101L);
        layer.setDeleted(0);
        layer.setCreatedBy(0L);
        when(layerMapper.selectById(101L)).thenReturn(layer);
        when(lotMapper.selectOne(any())).thenReturn(null);
        when(waferMapper.selectOne(any())).thenReturn(null);
        when(measurementRunMapper.selectCount(any())).thenReturn(0L);
        when(lotMapper.insert(any(Lot.class))).thenAnswer(invocation -> {
            Lot lot = invocation.getArgument(0);
            lot.setId(2001L);
            return 1;
        });
        when(waferMapper.insert(any(Wafer.class))).thenAnswer(invocation -> {
            Wafer wafer = invocation.getArgument(0);
            wafer.setId(3001L);
            return 1;
        });
        when(importFileRecordMapper.insert(any(ImportFileRecord.class))).thenAnswer(invocation -> {
            ImportFileRecord record = invocation.getArgument(0);
            record.setId(4001L);
            return 1;
        });
        when(measurementRunMapper.insert(any(MeasurementRun.class))).thenAnswer(invocation -> {
            MeasurementRun run = invocation.getArgument(0);
            run.setId(5001L);
            return 1;
        });
        when(overlayMeasurementPointMapper.insert(any(OverlayMeasurementPoint.class))).thenReturn(1);

        WaferAnalysisImportConfigDto config = new WaferAnalysisImportConfigDto();
        config.setLotNo("LOT-U-002");
        config.setWaferNo("W02");
        config.setLayerId(101L);
        config.setDiameterMm(new BigDecimal("300"));
        config.setFieldMapping(new WaferImportFieldMappingDto());

        String csv = "target_code,x_coord,y_coord,overlay_x,overlay_y,overlay_magnitude\n"
            + "P101,125,125,1.0,-0.2,1.0198\n";
        MockMultipartFile file = new MockMultipartFile("file", "points.csv", "text/csv", csv.getBytes());

        WaferAnalysisImportResultVo result = service.importWaferAnalysisCsv(file, config);

        assertTrue(result.isImported());
        assertEquals(1, result.getInsertedRows());
        assertEquals(0, result.getFailedRows());
    }
}
