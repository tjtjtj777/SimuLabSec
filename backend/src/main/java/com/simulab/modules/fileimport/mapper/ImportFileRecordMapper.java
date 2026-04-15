package com.simulab.modules.fileimport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.fileimport.entity.ImportFileRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportFileRecordMapper extends BaseMapper<ImportFileRecord> {
}
