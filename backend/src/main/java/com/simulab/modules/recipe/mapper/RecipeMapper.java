package com.simulab.modules.recipe.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.simulab.modules.recipe.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
