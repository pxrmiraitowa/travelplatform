package com.travelplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.travelplatform.entity.Review;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
