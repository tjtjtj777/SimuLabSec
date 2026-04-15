package com.simulab.modules.lot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.lot.entity.Lot;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LotMapper extends BaseMapper<Lot> {
}
