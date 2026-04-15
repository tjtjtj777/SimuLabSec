package com.simulab.modules.subrecipe.service;

import com.simulab.modules.subrecipe.dto.SubRecipeExportRequest;
import com.simulab.modules.subrecipe.dto.SubRecipeQueryDto;
import com.simulab.modules.subrecipe.dto.SubRecipeUploadRequest;
import com.simulab.modules.subrecipe.vo.SubRecipeDetailVo;
import com.simulab.modules.subrecipe.vo.SubRecipeFileTicketVo;
import com.simulab.modules.subrecipe.vo.SubRecipeSummaryVo;
import java.util.List;

public interface SubRecipeService {

    List<SubRecipeSummaryVo> listSubRecipes(SubRecipeQueryDto queryDto);

    SubRecipeDetailVo getDetail(Long subRecipeId);

    SubRecipeFileTicketVo buildUploadTicket(SubRecipeUploadRequest request);

    SubRecipeFileTicketVo buildDownloadTicket(Long subRecipeId);

    SubRecipeFileTicketVo buildExportTicket(Long subRecipeId, SubRecipeExportRequest request);
}
