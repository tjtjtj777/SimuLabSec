package com.simulab.modules.subrecipe.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.simulab.common.security.SecurityUser;
import com.simulab.modules.subrecipe.dto.SubRecipeExportRequest;
import com.simulab.modules.subrecipe.dto.SubRecipeQueryDto;
import com.simulab.modules.subrecipe.dto.SubRecipeUploadRequest;
import com.simulab.modules.subrecipe.entity.SubRecipe;
import com.simulab.modules.subrecipe.mapper.SubRecipeMapper;
import com.simulab.modules.subrecipe.vo.SubRecipeDetailVo;
import com.simulab.modules.subrecipe.vo.SubRecipeFileTicketVo;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SubRecipeServiceImplTest {

    @Mock
    private SubRecipeMapper subRecipeMapper;

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
    void getDetailShouldReturnMappedVo() {
        SubRecipeServiceImpl service = new SubRecipeServiceImpl(subRecipeMapper);
        SubRecipe entity = new SubRecipe();
        entity.setId(1L);
        entity.setSubRecipeCode("SUB-RC-001");
        entity.setStatus("READY");
        entity.setCreatedAt(LocalDateTime.of(2026, 4, 1, 12, 0));
        when(subRecipeMapper.selectOne(any())).thenReturn(entity);

        SubRecipeDetailVo vo = service.getDetail(1L);

        assertEquals("SUB-RC-001", vo.getSubRecipeCode());
        assertEquals("READY", vo.getStatus());
    }

    @Test
    void buildTicketsShouldReturnPlaceholderMetadata() {
        SubRecipeServiceImpl service = new SubRecipeServiceImpl(subRecipeMapper);
        SubRecipe entity = new SubRecipe();
        entity.setId(2L);
        entity.setSubRecipeCode("SUB-RC-002");
        when(subRecipeMapper.selectOne(any())).thenReturn(entity);

        SubRecipeUploadRequest uploadRequest = new SubRecipeUploadRequest();
        uploadRequest.setFileName("sub.json");
        uploadRequest.setFileType("application/json");
        SubRecipeExportRequest exportRequest = new SubRecipeExportRequest();
        exportRequest.setExportFormat("CSV");

        SubRecipeFileTicketVo upload = service.buildUploadTicket(uploadRequest);
        SubRecipeFileTicketVo download = service.buildDownloadTicket(2L);
        SubRecipeFileTicketVo export = service.buildExportTicket(2L, exportRequest);

        assertEquals("UPLOAD", upload.getAction());
        assertEquals("DOWNLOAD", download.getAction());
        assertTrue(export.getFileName().endsWith(".csv"));
    }

    @Test
    void listSubRecipesShouldMarkDataScope() {
        SubRecipeServiceImpl service = new SubRecipeServiceImpl(subRecipeMapper);
        SubRecipe demo = new SubRecipe();
        demo.setId(1L);
        demo.setCreatedBy(0L);
        demo.setSubRecipeCode("DEMO");
        when(subRecipeMapper.selectList(any())).thenReturn(List.of(demo));
        assertEquals("DEMO", service.listSubRecipes(new SubRecipeQueryDto()).get(0).getDataScope());
    }
}
