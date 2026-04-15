package com.simulab.modules.waferconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.waferconfig.entity.WaferAnalysisConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WaferAnalysisConfigMapper extends BaseMapper<WaferAnalysisConfig> {
    // 当前主要依赖 MyBatis-Plus 通用能力；复杂权限过滤由 service 层 QueryWrapper 负责。
}
