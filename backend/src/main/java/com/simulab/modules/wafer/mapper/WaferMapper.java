package com.simulab.modules.wafer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.wafer.entity.Wafer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WaferMapper extends BaseMapper<Wafer> {
    // 当前模块只使用 MyBatis-Plus 通用 CRUD，复杂查询主要放在 service 的 QueryWrapper 中完成。
}
