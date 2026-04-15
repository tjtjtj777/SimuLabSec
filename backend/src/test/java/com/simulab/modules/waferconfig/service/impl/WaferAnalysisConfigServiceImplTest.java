package com.simulab.modules.waferconfig.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.simulab.config.AnalysisExecutionProperties;
import com.simulab.modules.layer.entity.Layer;
import com.simulab.modules.layer.mapper.LayerMapper;
import com.simulab.modules.waferconfig.dto.WaferConfigUpsertDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class WaferAnalysisConfigServiceImplTest {

    private WaferAnalysisConfigServiceImpl service() {
        LayerMapper layerMapper = mock(LayerMapper.class);
        Layer layer = new Layer();
        layer.setId(1L);
        layer.setDeleted(0);
        when(layerMapper.selectOne(any())).thenReturn(layer);
        return new WaferAnalysisConfigServiceImpl(
            null, layerMapper, null, null, null, null, null, null, mock(JdbcTemplate.class), new AnalysisExecutionProperties());
    }

    @Test
    void buildDefaultConfigShouldProvideLegalDefaults() {
        var defaults = service().buildDefaultConfig();
        assertTrue(defaults.getScannerCorrectionGain().doubleValue() >= 0.7);
        assertTrue(defaults.getScannerCorrectionGain().doubleValue() <= 1.3);
        assertTrue(defaults.getGridStep().doubleValue() >= 0.5);
        assertFalse(defaults.getValidationRules().isEmpty());
    }

    @Test
    void validateConfigShouldRejectOutOfRangeValues() {
        WaferConfigUpsertDto dto = new WaferConfigUpsertDto();
        dto.setConfigName("bad");
        dto.setLotNo("LOT-U-001");
        dto.setWaferNo("W01");
        dto.setLayerId(1L);
        dto.setMeasurementType("OVERLAY");
        dto.setStage("PRE_ETCH");
        dto.setScannerCorrectionGain(new BigDecimal("2.00"));
        dto.setOverlayBaseNm(new BigDecimal("3.00"));
        dto.setEdgeGradient(new BigDecimal("1.00"));
        dto.setLocalHotspotStrength(new BigDecimal("1.00"));
        dto.setNoiseLevel(new BigDecimal("0.10"));
        dto.setGridStep(new BigDecimal("1.00"));
        dto.setOutlierThreshold(new BigDecimal("8.00"));
        assertFalse(service().validateConfig(dto).isEmpty());
    }
}
